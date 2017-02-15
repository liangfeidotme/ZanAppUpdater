package com.youzan.genesis;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.Nullable;
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
    public static final String ARG_SILENT = "ARG_SILENT";
    public static final String ARG_DOWNLOAD_FAIL_RETRY = "DOWNLOAD_FAIL_RETRY";
    private static boolean isDownloading = false;
    private File mApkFile = null;
    private String mApkPath;
    private String mApkName;
    private DownloadInfo mDownloadInfo;
    private static final int NOTIFY_ID = 0xA1;
    private NotificationManager mNotificationManager = null;
    private PendingIntent mPendingIntent = null;
    private String mDownloadProgressStr = null;
    private Notification mNotification = null;
    private NotificationCompat.Builder mBuilder = null;
    private boolean mIsRetry;
    private Intent mLastIntent;
    private boolean mSilent = false;
    private static ServiceListener mListener;

    public static void addServiceListener(ServiceListener listener) {
        mListener = listener;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            init(intent);
        } else {
            return super.onStartCommand(intent, START_REDELIVER_INTENT, startId);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void init(Intent intent) {
        initParam(intent);
        if (!isDownloading && null != mDownloadInfo) {
            if (FileUtil.isSDCardStateOn() && !FileUtil.isSDCardReadOnly()) {
                if (!mIsRetry) {
                    // 不是断点续传的情况下 删除无效的apk
                    FileUtil.deleteFile(mApkFile.getPath());
                }
            }
            if (!mSilent) {
                showStartNotification();
            }
            isDownloading = true;
            startDownload();
        } else {
            stopSelf();
        }
    }

    private void initParam(Intent intent) {
        mDownloadProgressStr = getString(R.string.download_progress);
        mLastIntent = intent;
        mIsRetry = mLastIntent.getBooleanExtra(ARG_DOWNLOAD_FAIL_RETRY, false);

        Parcelable parcelable = intent.getParcelableExtra(ARG_DOWNLOAD_INFO);
        if (parcelable != null && parcelable instanceof DownloadInfo) {
            mDownloadInfo = (DownloadInfo) parcelable;
            mApkPath = Environment.getExternalStorageDirectory().getPath() + "/download_app";
            mApkName = mDownloadInfo.getFileName();
            mApkFile = new File(mApkPath, mApkName);
        }
        mSilent = intent.getBooleanExtra(ARG_SILENT, false);
    }

    private void showStartNotification() {
        if (null == mNotificationManager) {
            mNotificationManager = (NotificationManager) getSystemService(Context
                    .NOTIFICATION_SERVICE);
        }

        Intent completingIntent = new Intent();
        completingIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        completingIntent.setClass(this, UpdateAppService.class);
        mPendingIntent = PendingIntent.getActivity(UpdateAppService.this, NOTIFY_ID,
                completingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this);

        mBuilder.setSmallIcon(R.drawable.ic_download);
        mBuilder.setContentIntent(mPendingIntent)
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
                    .setContentTitle(mDownloadInfo.getFileName())
                    .setContentText(String.format(mDownloadProgressStr, progress) + "%");
            mNotification = mBuilder.build();
            mNotificationManager.notify(NOTIFY_ID, mNotification);
        }
    }

    private void showErrorNotification() {
        if (null == mNotificationManager) {
            mNotificationManager = (NotificationManager) getSystemService(Context
                    .NOTIFICATION_SERVICE);
        }

        mLastIntent.putExtra(ARG_DOWNLOAD_FAIL_RETRY, true);
        PendingIntent retryIntent = PendingIntent.getService(this, 0,
                mLastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.drawable.ic_download);
        builder.setContentTitle(mApkFile.getName())
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
        DownloadUtil.newInstance().download(mDownloadInfo.getDownloadUrl(), mApkPath, mApkName,
                mIsRetry, new DownloadUtil.DownloadListener() {
                    @Override
                    public void downloading(int progress) {
                        if (!mSilent) {
                            showUpdateNotification(progress);
                        }
                    }

                    @Override
                    public void downloaded() {
                        if (!mSilent) {
                            showUpdateNotification(100);
                        }
                        handleSuccess();
                    }

                    @Override
                    public void downloadError(String error) {
                        handleError(error);
                    }
                });
    }

    private void handleError(String error) {
        if (!mSilent) {
            ToastUtil.show(this, R.string.download_fail);
            showErrorNotification();
        } else if (mListener != null) {
            mListener.onError(error);
        }
        isDownloading = false;
        stopSelf();
    }

    private void handleSuccess() {
        if (mApkFile.exists() && mApkFile.isFile()
                && FileUtil.checkApkFileValid(UpdateAppService.this, mApkFile)) {

            if (!mSilent) {
                ToastUtil.show(this, R.string.download_success);
                if (mNotificationManager != null) {
                    mNotificationManager.cancel(NOTIFY_ID);
                }
                FileUtil.install(this, mApkFile);
            }

            if (mListener != null) {
                mListener.onSuccess(Uri.fromFile(mApkFile));
            }
        } else {
            handleError("下载的文件类型无效");
        }
        isDownloading = false;
        stopSelf();
    }

    interface ServiceListener {
        void onSuccess(Uri fileUri);

        void onError(String msg);
    }
}