package com.youzan.genesis;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.os.Parcelable;

import com.youzan.genesis.info.DownloadInfo;
import com.youzan.genesis.utils.FileUtil;

import java.io.File;

/**
 * Created by Francis on 15/10/28.
 */
public class UpdateAppService extends Service {

    public static final String ARG_DOWNLOAD_INFO = "DOWNLOAD_INFO";
    private static boolean isDownloading = false;
    private File apkFile = null;
    private DownloadInfo downloadInfo;

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

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadInfo.getDownloadUrl()));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(this, "download_app", downloadInfo.getFileName());
        request.setTitle(downloadInfo.getFileName());

        downloadManager = ((DownloadManager) getSystemService(DOWNLOAD_SERVICE));
        downloadId = downloadManager.enqueue(request);
    }
}
