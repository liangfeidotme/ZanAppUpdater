package com.youzan.genesis;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.youzan.genesis.info.DownloadInfo;
import com.youzan.genesis.utils.DateUtil;
import com.youzan.genesis.utils.FileUtil;
import com.youzan.genesis.utils.StringUtil;
import com.youzan.genesis.utils.ToastUtil;

import java.io.File;
import java.util.Date;

/**
 * Created by Francis on 15/10/28.
 */
public class UpdateAppService extends Service {

    public static final String ARG_DOWNLOAD_INFO = "DOWNLOAD_INFO";
    public static final String ARG_APP_ICON = "APP_ICON";

    public static final int mNotificationId = R.id.app_name;
    private NotificationManager mNotificationManager = null;
    private PendingIntent mPendingIntent = null;
    private Notification mNotification = null;
    private String mDownloadProgressStr = null;
    private File apkFile = null;
    private DownloadInfo downloadInfo;
    private int iconID;

    private static boolean isDownloading = false;

    private static final int DOWNLOAD_FAIL = -1;
    private static final int DOWNLOAD_SUCCESS = 0;
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DOWNLOAD_SUCCESS:
                    ToastUtil.show(getApplicationContext(), R.string.download_success);
                    install(apkFile);
                    break;
                case DOWNLOAD_FAIL:
                    ToastUtil.show(getApplicationContext(), R.string.download_fail);
                    mNotificationManager.cancel(mNotificationId);

                    //重新下载
                    PendingIntent retryIntent = PendingIntent.getService(getApplicationContext(), 0,
                            new Intent(getApplicationContext(), UpdateAppService.class),
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                    builder.setSmallIcon(iconID)
                            .setContentTitle(getApplicationContext().getString(R.string.app_name))
                            .setContentText(getApplicationContext().getString(R.string.download_fail_retry))
                            .setContentIntent(retryIntent)
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            .setOngoing(false);
                    Notification notification = builder.build();
                    mNotificationManager.notify(mNotificationId, notification);
                    break;
                default:
                    break;
            }
        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isDownloading) {
            return super.onStartCommand(intent, flags, startId);
        }

        initParam(intent);

        if (null == downloadInfo) {
            return super.onStartCommand(intent, flags, startId);
        }

        mDownloadProgressStr = getApplicationContext().getString(R.string.download_progress);

        if (FileUtil.isSDCardStateOn()
                && !FileUtil.isSDCardReadOnly()) {
            if (checkApkFileExist(downloadInfo.getFilePath())) {
                // 本地存在有效的apk，直接安装
                if (checkApkFileValid(apkFile.getPath())) {
                    install(apkFile);
                    stopSelf();
                    return super.onStartCommand(intent, flags, startId);
                }
            }
        } else {
            return super.onStartCommand(intent, flags, startId);
        }

        mNotificationManager = (NotificationManager) getApplication().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = new Notification();
        mNotification.contentView = new RemoteViews(getApplication().getPackageName(), R.layout.update_app_notification);

        Intent completingIntent = new Intent();
        completingIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        completingIntent.setClass(getApplication().getApplicationContext(), UpdateAppService.class);

        mPendingIntent = PendingIntent.getActivity(UpdateAppService.this, R.string.app_name, completingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mNotification.icon = iconID;
        mNotification.when = System.currentTimeMillis();
        mNotification.tickerText = getApplicationContext().getString(R.string.download_start);
        mNotification.contentIntent = mPendingIntent;
        mNotification.contentView.setImageViewResource(R.id.app_icon, iconID);
        mNotification.contentView.setProgressBar(R.id.app_update_progress, 100, 0, false);
        mNotification.contentView.setTextViewText(R.id.app_update_progress_text, String.format(mDownloadProgressStr, 0) + "%");
        mNotification.contentView.setTextViewText(R.id.app_update_time, DateUtil.getCurrentTimeForNotification());
        mNotificationManager.cancel(mNotificationId);
        mNotificationManager.notify(mNotificationId, mNotification);
        new AppUpgradeThread().start();

        return super.onStartCommand(intent, flags, startId);
    }

    private void initParam(Intent intent) {
        if (intent == null) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            iconID = bundle.getInt(ARG_APP_ICON, 0);
        }

        Parcelable parcelable = intent.getParcelableExtra(ARG_DOWNLOAD_INFO);
        if (parcelable != null && parcelable instanceof DownloadInfo) {
            downloadInfo = (DownloadInfo) parcelable;
            downloadInfo.setFilePath(getApkFilePath(downloadInfo.getFileName()));
        } else {
            stopSelf();
        }
    }

    class AppUpgradeThread extends Thread {

        @Override
        public void run() {
            isDownloading = true;
            Ion.with(getApplicationContext())
                    .load(downloadInfo.getDownloadUrl())
                    .progress(new ProgressCallback() {
                        @Override
                        public void onProgress(long downloaded, long total) {
                            mNotification.tickerText = getApplicationContext().getString(R.string.downloading);
                            mNotification.contentView.setProgressBar(R.id.app_update_progress, (int) total, (int) downloaded, false);
                            mNotification.contentView.setTextViewText(R.id.app_update_progress_text,
                                    String.format(mDownloadProgressStr, 100 * downloaded / total) + "%");
                            mNotificationManager.notify(mNotificationId, mNotification);
                        }
                    })
                    .write(apkFile)
                    .setCallback(new FutureCallback<File>() {
                        @Override
                        public void onCompleted(Exception e, File result) {
                            isDownloading = false;
                            if (null == e) {
                                mNotification.contentView.setViewVisibility(R.id.app_update_progress, View.GONE);
                                mNotification.contentIntent = mPendingIntent;
                                mNotification.contentView.setTextViewText(R.id.app_update_progress_text, getApplicationContext().getString(R.string.download_done));
                                mNotificationManager.notify(mNotificationId, mNotification);
                                if (apkFile.exists() && apkFile.isFile()
                                        && checkApkFileValid(apkFile.getPath())) {
                                    Message msg = mHandler.obtainMessage();
                                    msg.what = DOWNLOAD_SUCCESS;
                                    mHandler.sendMessage(msg);
                                } else {
                                    Message msg = mHandler.obtainMessage();
                                    msg.what = DOWNLOAD_FAIL;
                                    mHandler.sendMessage(msg);
                                }
                                mNotificationManager.cancel(mNotificationId);
                            } else {
                                Message msg = mHandler.obtainMessage();
                                msg.what = DOWNLOAD_FAIL;
                                mHandler.sendMessage(msg);
                            }
                        }
                    });
            stopSelf();
        }
    }

    /**
     * 检查新版本的文件是否已经下载
     */
    private boolean checkApkFileExist(String apkPath) {
        if (StringUtil.isEmpty(apkPath)) {
            return false;
        }
        apkFile = new File(apkPath);
        return apkFile.exists() && apkFile.isFile();
    }

    /**
     * 获取apk更新文件路径
     */
    public String getApkFilePath(String apkName) {
        return FileUtil.getDownloadAppFilePath(apkName);
    }

    private boolean checkApkFileValid(String apkPath) {
        boolean valid;

        // 创建时间大于10min，不再有效
        if (checkApkFileCreatedTime()) {
            valid = false;
        } else {
            try {
                PackageManager pManager = getPackageManager();
                PackageInfo pInfo = pManager.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
                if (pInfo == null) {
                    valid = false;
                } else {
                    valid = true;
                }
            } catch (Exception e) {
                valid = false;
                e.printStackTrace();
            }
        }

        return valid;
    }

    private boolean checkApkFileCreatedTime() {
        if (downloadInfo == null) {
            return true;
        }
        apkFile = new File(downloadInfo.getFilePath());
        long lastTime = apkFile.lastModified();
        long nowTime = new Date().getTime();

        return nowTime - lastTime > 10 * 60 * 1000;
    }

    private void install(File apkFile) {
        Uri uri = Uri.fromFile(apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        startActivity(intent);
    }
}
