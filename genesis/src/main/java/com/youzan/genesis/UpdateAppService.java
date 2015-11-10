package com.youzan.genesis;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;

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
    private static final int DOWNLOAD_FAIL = -1;
    private static final int DOWNLOAD_SUCCESS = 0;
    private static final int REQUEST_CODE = 0x11;

    private String mDownloadProgressStr = null;
    private File apkFile = null;
    private DownloadInfo downloadInfo;
    //private long lastDownload = 0L;
    private Intent lastIntent;


    private static ShowNotification showNotificationLisenter;

    public interface ShowNotification {
        void showStartNotification(PendingIntent pendingIntent,String title);

        void showUpdateNotification(int progress, String title, String context);

        void showSuccessNotification();

        void showFailNotification(PendingIntent pendingIntent,String title,String context);
    }

    public static void setShowNotification(ShowNotification showNotification) {
        showNotificationLisenter = showNotification;
    }

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

    private void showStartNotification() {
        if (showNotificationLisenter != null) {
            Intent completingIntent = new Intent();
            completingIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            completingIntent.setClass(this, UpdateAppService.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, REQUEST_CODE, completingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            showNotificationLisenter.showStartNotification(pendingIntent, getString(R.string.download_start));
        }
    }

    private void showUpdateNotification(int progress) {
        if (showNotificationLisenter != null) {
            showNotificationLisenter.showUpdateNotification(progress, downloadInfo.getFileName(), String.format(mDownloadProgressStr, progress) + "%");
        }
    }

    private void showFailNotification(Intent lastIntent) {
        if (showNotificationLisenter != null) {
            PendingIntent retryIntent = PendingIntent.getService(this, 0,
                    lastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            showNotificationLisenter.showFailNotification(retryIntent, downloadInfo.getFileName(), getString(R.string.download_fail_retry));
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case DOWNLOAD_SUCCESS:
                ToastUtil.show(this, R.string.download_success);
                FileUtil.install(this, apkFile);

                if (showNotificationLisenter != null){
                    showNotificationLisenter.showSuccessNotification();
                }
                break;
            case DOWNLOAD_FAIL:
                ToastUtil.show(this, R.string.download_fail);
                //重新下载
                showFailNotification(lastIntent);
                break;
            default:
                break;
        }
    }

    private void startDownload() {
        isDownloading = true;
        DownloadUtil.newInstance().download(downloadInfo.getDownloadUrl(), apkFile, false, new DownloadUtil.DownloadListener() {
            @Override
            public void downloading(int progress) {
                showUpdateNotification(progress);
            }

            @Override
            public void downloaded() {
                isDownloading = false;

                showUpdateNotification(100);

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
