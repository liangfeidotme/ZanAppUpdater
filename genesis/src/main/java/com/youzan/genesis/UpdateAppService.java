package com.youzan.genesis;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;

import com.youzan.genesis.info.DownloadInfo;
import com.youzan.genesis.utils.DownloadUtil;
import com.youzan.genesis.utils.FileUtil;
import com.youzan.genesis.utils.ToastUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created by Francis on 15/10/28.
 */
public class UpdateAppService extends Service {

    public static final String ARG_DOWNLOAD_INFO = "DOWNLOAD_INFO";
    private static boolean isDownloading = false;
    private static final int DOWNLOAD_FAIL = -1;
    private static final int DOWNLOAD_SUCCESS = 0;
    private static final int NOTIFY_ID = 0xA1;

    private NotificationManager mNotificationManager = null;
    private PendingIntent mPendingIntent = null;
    private Notification mNotification = null;
    private NotificationCompat.Builder mBuilder = null;
    private String mDownloadProgressStr = null;
    private File apkFile = null;
    private DownloadInfo downloadInfo;
    private String appType;
    //private long lastDownload = 0L;
    private Intent lastIntent;


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
        if (FileUtil.isSDCardStateOn()
                && !FileUtil.isSDCardReadOnly()) {
            if (FileUtil.checkApkFileExist(apkFile)) {
                // 本地存在有效的apk，直接安装
                if (FileUtil.checkApkFileValid(this, downloadInfo, apkFile.getPath())) {
                    FileUtil.install(this, apkFile);
                    stopSelf();
                    return super.onStartCommand(intent, flags, startId);
                } else {
                    // 删除无效的apk
                    FileUtil.deleteFile(apkFile.getPath());
                }
            }
        } else {
            return super.onStartCommand(intent, flags, startId);
        }

        showStartNotification();
        startDownload();

        return super.onStartCommand(intent, flags, startId);
    }

    public static boolean getDownLoadState() {
        return isDownloading;
    }

    private void initParam(Intent intent) {
        if (intent == null) {
            return;
        }
        mDownloadProgressStr = getString(R.string.download_progress);
        lastIntent = intent;

        Bundle bundle = lastIntent.getExtras();
        if (bundle != null) {
            appType = bundle.getString(UpdateAppUtil.ARGS_APP_TYPE);
        }
        Parcelable parcelable = lastIntent.getParcelableExtra(ARG_DOWNLOAD_INFO);
        if (parcelable != null && parcelable instanceof DownloadInfo) {
            downloadInfo = (DownloadInfo) parcelable;
            downloadInfo.setFilePath(FileUtil.getDownloadApkFilePath(downloadInfo.getFileName()));
            apkFile = new File(downloadInfo.getFilePath());
            FileUtil.checkToCreateApkFile(apkFile);
        } else {
            stopSelf();
        }
    }

    private void showErrorNotification(){
        if (null == mNotificationManager) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        PendingIntent retryIntent = PendingIntent.getService(this, 0,
                lastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(getNotificationIcon(appType))
                .setContentTitle(apkFile.getName())
                .setContentText(getString(R.string.download_fail_retry))
                .setContentIntent(retryIntent)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setOngoing(false);
        Notification notification = builder.build();
        mNotificationManager.cancel(NOTIFY_ID);
        mNotificationManager.notify(NOTIFY_ID, notification);
    }

    private void showStartNotification() {
        if (null == mNotificationManager) {
            mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        }

        Intent completingIntent = new Intent();
        completingIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        completingIntent.setClass(this, UpdateAppService.class);
        mPendingIntent = PendingIntent.getActivity(UpdateAppService.this, NOTIFY_ID, completingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(getNotificationIcon(appType))
                .setContentIntent(mPendingIntent)
                .setWhen(System.currentTimeMillis())
                .setDefaults(~Notification.DEFAULT_ALL)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentTitle(getString(R.string.download_start))
                .setProgress(100, 0, false);

        mNotification = mBuilder.build();
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;

        mNotificationManager.cancel(NOTIFY_ID);
        mNotificationManager.notify(NOTIFY_ID, mNotification);

    }

    private int getNotificationIcon(String appType) {
        boolean whiteIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        if (whiteIcon) {
            return appType.equals(UpdateAppUtil.APP_TYPE_WXD) ? R.drawable.wxd_icon_trans : R.drawable.wsc_icon_trans;
        }
        return appType.equals(UpdateAppUtil.APP_TYPE_WXD) ? R.drawable.wxd_icon : R.drawable.wsc_icon;
    }

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case DOWNLOAD_SUCCESS:
                ToastUtil.show(this, R.string.download_success);
                FileUtil.install(this, apkFile);
                mNotificationManager.cancel(NOTIFY_ID);
                break;
            case DOWNLOAD_FAIL:
                ToastUtil.show(this, R.string.download_fail);
                //重新下载
                showErrorNotification();
                break;
            default:
                break;
        }
    }

    private void startDownload() {
        DownloadUtil.newInstance().download(downloadInfo.getDownloadUrl(), apkFile, false, new DownloadUtil.DownloadListener() {
            @Override
            public void downloading(int progress) {

                mBuilder.setProgress(100, progress, false)
                        .setContentTitle(downloadInfo.getFileName())
                        .setContentText(String.format(mDownloadProgressStr, progress) + "%");
                mNotification = mBuilder.build();
                mNotificationManager.notify(NOTIFY_ID, mNotification);
            }

            @Override
            public void downloaded() {
                isDownloading = false;

                mBuilder.setProgress(100, 100, false)
                        .setContentTitle(downloadInfo.getFileName())
                        .setContentText(getString(R.string.download_done));
                mNotification = mBuilder.build();
                mNotificationManager.notify(NOTIFY_ID, mNotification);

                if (apkFile.exists() && apkFile.isFile()
                        && FileUtil.checkApkFileValid(UpdateAppService.this, downloadInfo, apkFile.getPath())) {
                    Message msg = Message.obtain();
                    msg.what = DOWNLOAD_SUCCESS;
                    handleMessage(msg);
                } else {
                    Message msg = Message.obtain();
                    msg.what = DOWNLOAD_FAIL;
                    handleMessage(msg);
                }

                stopSelf();
            }

            @Override
            public void downloadError(String error) {
                isDownloading = false;

                Message msg = Message.obtain();
                msg.what = DOWNLOAD_FAIL;
                handleMessage(msg);

                stopSelf();
            }
        });
    }
}
