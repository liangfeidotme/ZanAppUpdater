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
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.youzan.genesis.info.DownloadInfo;
import com.youzan.genesis.utils.DateUtil;
import com.youzan.genesis.utils.DownloadUtil;
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
    private int appIcon;
    //private long lastDownload = 0L;
    private Intent lastIntent;

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case DOWNLOAD_SUCCESS:
                ToastUtil.show(getApplicationContext(), R.string.download_success);
                install(apkFile);
                mNotificationManager.cancel(NOTIFY_ID);
                break;
            case DOWNLOAD_FAIL:
                ToastUtil.show(getApplicationContext(), R.string.download_fail);

                //重新下载
                PendingIntent retryIntent = PendingIntent.getService(getApplicationContext(), 0,
                        lastIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());

                builder
                        .setSmallIcon(appIcon)
                        .setContentTitle(apkFile.getName())
                        .setContentText(getApplicationContext().getString(R.string.download_fail_retry))
                        .setContentIntent(retryIntent)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        .setOngoing(false);
                Notification notification = builder.build();
                mNotificationManager.notify(NOTIFY_ID, notification);

//                if (null == mNotificationManager) {
//                    mNotificationManager = (NotificationManager) getApplication().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
//                }
//                mNotification = new Notification();
//
//                mNotification.contentView = new RemoteViews(getApplication().getPackageName(), R.layout.update_app_notification);
//
//                //mNotification.icon = R.drawable.;
//                mNotification.when = System.currentTimeMillis();
//                mNotification.tickerText = getApplicationContext().getString(R.string.download_start);
//                mNotification.contentIntent = mPendingIntent;
//                mNotification.contentView.setProgressBar(R.id.app_update_progress, 100, 0, false);
//                mNotification.contentView.setTextViewText(R.id.app_update_progress_text, String.format(mDownloadProgressStr, 0) + "%");
//                mNotification.contentView.setTextViewText(R.id.app_update_time, DateUtil.getCurrentTimeForNotification());
//                mNotificationManager.cancel(NOTIFY_ID);
//                mNotificationManager.notify(NOTIFY_ID, mNotification);

                break;
            default:
                break;
        }
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
            if (checkApkFileExist(downloadInfo.getFilePath())) {
                // 本地存在有效的apk，直接安装
                if (checkApkFileValid(apkFile.getPath())) {
                    install(apkFile);
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

        showNotification();

        //new AsyncDownloader(downloadInfo.getDownloadUrl()).execute();

        startDownload();

        return super.onStartCommand(intent, flags, startId);
    }

    private void initParam(Intent intent) {
        if (intent == null) {
            return;
        }
        mDownloadProgressStr = getApplicationContext().getString(R.string.download_progress);
        lastIntent = intent;

        Bundle bundle = lastIntent.getExtras();
        if (bundle != null) {
            appType = bundle.getString(UpdateAppUtil.ARGS_APP_NAME);
            appIcon = bundle.getInt(UpdateAppUtil.ARGS_APP_ICON);
        }
        Parcelable parcelable = lastIntent.getParcelableExtra(ARG_DOWNLOAD_INFO);
        if (parcelable != null && parcelable instanceof DownloadInfo) {
            downloadInfo = (DownloadInfo) parcelable;
            downloadInfo.setFilePath(getApkFilePath(downloadInfo.getFileName()));
        } else {
            stopSelf();
        }
    }

    /**
     *  显示通知
     */
    private void showNotification() {
        if (null == mNotificationManager) {
            mNotificationManager = (NotificationManager) getApplication().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        }

        Intent completingIntent = new Intent();
        completingIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        completingIntent.setClass(getApplication().getApplicationContext(), UpdateAppService.class);
        mPendingIntent = PendingIntent.getActivity(UpdateAppService.this, NOTIFY_ID, completingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(appIcon)
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


//        if (null == mNotificationManager) {
//            mNotificationManager = (NotificationManager) getApplication().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
//        }
//        mNotification = new Notification();
//
//        mNotification.contentView = new RemoteViews(getApplication().getPackageName(), R.layout.update_app_notification);
//
//        //mNotification.icon = R.drawable.;
//        mNotification.when = System.currentTimeMillis();
//        mNotification.tickerText = getApplicationContext().getString(R.string.download_start);
//        mNotification.contentIntent = mPendingIntent;
//        mNotification.contentView.setProgressBar(R.id.app_update_progress, 100, 0, false);
//        mNotification.contentView.setTextViewText(R.id.app_update_progress_text, String.format(mDownloadProgressStr, 0) + "%");
//        mNotification.contentView.setTextViewText(R.id.app_update_time, DateUtil.getCurrentTimeForNotification());
//        mNotificationManager.cancel(NOTIFY_ID);
//        mNotificationManager.notify(NOTIFY_ID, mNotification);
    }


//    private class AsyncDownloader extends AsyncTask<Void, Long, Boolean> {
//
//
//        private String downLoadUrl;
//
//        public AsyncDownloader(String url) {
//            this.downLoadUrl = url;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            isDownloading = true;
//        }
//
//        @Override
//        protected Boolean doInBackground(Void... params) {
//            OkHttpClient httpClient = new OkHttpClient();
//            Call call = httpClient.newCall(new Request.Builder().url(downLoadUrl).get().build());
//            try {
//                Response response = call.execute();
//                if (response.code() == 200) {
//                    InputStream inputStream = null;
//                    FileOutputStream fos = new FileOutputStream(apkFile);
//                    try {
//                        inputStream = response.body().byteStream();
//                        byte[] buff = new byte[1024 * 4];
//                        long downloaded = 0L;
//                        long target = response.body().contentLength();
//
//                        publishProgress(0L, target);
//                        while (true) {
//                            int readed = inputStream.read(buff);
//                            if (readed == -1) {
//                                break;
//                            }
//                            //write buff
//                            fos.write(buff, 0, readed);
//
//                            downloaded += readed;
//                            // 防止过于频繁以致阻塞 每100KB刷新一次
//                            if (downloaded - lastDownload > (StringUtil.MB / 10.0)) {
//                                lastDownload = downloaded;
//                                publishProgress(downloaded, target);
//                            }
//                            if (isCancelled()) {
//                                return false;
//                            }
//                        }
//                        return downloaded == target;
//                    } catch (IOException ignore) {
//                        return false;
//                    } finally {
//                        if (inputStream != null) {
//                            inputStream.close();
//                        }
//                        if (fos != null) {
//                            fos.close();
//                        }
//                    }
//                } else {
//                    return false;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                return false;
//            }
//        }
//
//        @Override
//        protected void onProgressUpdate(Long... values) {
//            if (values[0] < values[1]) {
//                long progress = (values[0] * 100) / values[1];
//                mBuilder.setProgress(100, (int) progress, false)
//                        .setContentTitle(downloadInfo.getFileName())
//                        .setContentText(String.format(mDownloadProgressStr, progress) + "%");
//            } else {
//                mBuilder.setProgress(0, 0, false);
//            }
//            mNotification = mBuilder.build();
//            mNotificationManager.notify(NOTIFY_ID, mNotification);
//
//        }
//
//        @Override
//        protected void onPostExecute(Boolean result) {
//            isDownloading = false;
//            if (result) {
//                if (apkFile.exists() && apkFile.isFile()
//                        && checkApkFileValid(apkFile.getPath())) {
//                    Message msg = Message.obtain();
//                    msg.what = DOWNLOAD_SUCCESS;
//                    handleMessage(msg);
//                } else {
//                    Message msg = Message.obtain();
//                    msg.what = DOWNLOAD_FAIL;
//                    handleMessage(msg);
//                }
//            } else {
//                Message msg = Message.obtain();
//                msg.what = DOWNLOAD_FAIL;
//                handleMessage(msg);
//            }
//            stopSelf();
//        }
//    }

    private void startDownload(){
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

                if (apkFile.exists() && apkFile.isFile()
                        && checkApkFileValid(apkFile.getPath())) {
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
            public void downError(String error) {
                isDownloading = false;

                Message msg = Message.obtain();
                msg.what = DOWNLOAD_FAIL;
                handleMessage(msg);

                stopSelf();
            }
        });
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
        //for test;
        //return true;
    }

    private void install(File apkFile) {
        Uri uri = Uri.fromFile(apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        startActivity(intent);
    }
}
