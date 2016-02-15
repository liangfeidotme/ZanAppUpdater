package com.youzan.genesis;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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
    private File apkFile = null;
    private DownloadInfo downloadInfo;
    private static final int DOWNLOAD_FAIL = -1;
    private static final int DOWNLOAD_SUCCESS = 0;
    private CompleteReceiver completeReceiver;
    private long downloadId = 0;
    private DownloadManager downloadManager;

    class CompleteReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                isDownloading = false;
                long completeDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (completeDownloadId == downloadId) {
                    Uri uri = downloadManager.getUriForDownloadedFile(downloadId);
                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
                    context.startActivity(installIntent);
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        completeReceiver = new CompleteReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(completeReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(completeReceiver);
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

        Parcelable parcelable = intent.getParcelableExtra(ARG_DOWNLOAD_INFO);
        if (parcelable != null && parcelable instanceof DownloadInfo) {
            downloadInfo = (DownloadInfo) parcelable;
            apkFile = new File(getExternalFilesDir("download_app") + "/" + downloadInfo.getFileName());
        } else {
            stopSelf();
        }
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

    
    private void showUpdateNotification(int progress) {
    }

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case DOWNLOAD_SUCCESS:
                ToastUtil.show(this, R.string.download_success);
                FileUtil.install(this, apkFile);

//                if (showNotificationLisenter != null) {
//                    showNotificationLisenter.showSuccessNotification();
//                }
                break;
            case DOWNLOAD_FAIL:
                ToastUtil.show(this, R.string.download_fail);
                //重新下载
                //showFailNotification(lastIntent);
                break;
            default:
                break;
        }
    }

}
