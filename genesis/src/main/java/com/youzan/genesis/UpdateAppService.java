package com.youzan.genesis;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;

import com.youzan.genesis.info.DownloadInfo;
import com.youzan.genesis.utils.DownloadUtil;
import com.youzan.genesis.utils.FileUtil;
import com.youzan.genesis.utils.ToastUtil;

import java.io.File;

/**
 * Created by Francis on 15/10/28.
 */
public class UpdateAppService extends Service {

    public static final String ARG_DOWNLOAD_INFO = "DOWNLOAD_INFO";
    private static boolean isDownloading = false;
    private File apkFile = null;
    private DownloadInfo downloadInfo;
    private static final int DOWNLOAD_FAIL = -1;
    private static final int DOWNLOAD_SUCCESS = 0;
    private static final int NOTIFY_ID = 0xA1;
    private NotificationManager mNotificationManager = null;
    private PendingIntent mPendingIntent = null;
    private String mDownloadProgressStr = null;
    private Notification mNotification = null;
    private NotificationCompat.Builder mBuilder = null;
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
                if (FileUtil.checkApkFileValid(this, apkFile)) {
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

        Parcelable parcelable = intent.getParcelableExtra(ARG_DOWNLOAD_INFO);
        if (parcelable != null && parcelable instanceof DownloadInfo) {
            downloadInfo = (DownloadInfo) parcelable;
            apkFile = new File(getExternalFilesDir("download_app") + "/" + downloadInfo.getFileName());
        } else {
            stopSelf();
        }
    }

    private void showStartNotification() {
        if (null == mNotificationManager) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        Intent completingIntent = new Intent();
        completingIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        completingIntent.setClass(this, UpdateAppService.class);
        mPendingIntent = PendingIntent.getActivity(UpdateAppService.this, NOTIFY_ID, completingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.download_icon)
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

    private void showUpdateNotification(int progress) {
        if (mBuilder != null && mNotificationManager != null) {
            mBuilder.setProgress(100, progress, false)
                    .setContentTitle(downloadInfo.getFileName())
                    .setContentText(String.format(mDownloadProgressStr, progress) + "%");
            mNotification = mBuilder.build();
            mNotificationManager.notify(NOTIFY_ID, mNotification);
        }
    }

    private void showErrorNotification() {
        if (null == mNotificationManager) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        PendingIntent retryIntent = PendingIntent.getService(this, 0,
                lastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.drawable.download_icon)
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

    private void startDownload() {
        isDownloading = true;
        DownloadUtil.newInstance().download(downloadInfo.getDownloadUrl(), apkFile, true, new DownloadUtil.DownloadListener() {
            @Override
            public void downloading(int progress) {
                showUpdateNotification(progress);
            }

            @Override
            public void downloaded() {
                isDownloading = false;
                showUpdateNotification(100);

                if (apkFile.exists() && apkFile.isFile()
                        && FileUtil.checkApkFileValid(UpdateAppService.this, apkFile)) {
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

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case DOWNLOAD_SUCCESS:
                ToastUtil.show(this, R.string.download_success);
                FileUtil.install(this, apkFile);
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

}