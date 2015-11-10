package com.youzan.genesis.genesisupdater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v7.app.NotificationCompat;

import com.youzan.genesis.UpdateAppService;

/**
 * Created by Francis on 15/11/10.
 */
public class UpdateNotificationUtil {

    private Context context;
    private static final int NOTIFY_ID = 0xA1;
    private NotificationManager mNotificationManager = null;
    private Notification mNotification = null;
    private android.support.v4.app.NotificationCompat.Builder mBuilder = null;

    private static UpdateNotificationUtil instance = null;

    private UpdateNotificationUtil(Context context) {
        this.context = context;
    }

    public static UpdateNotificationUtil getInstance(Context context){
        if (instance == null){
            synchronized (UpdateNotificationUtil.class){
                if (instance == null){
                    instance = new UpdateNotificationUtil(context);
                }
            }
        }
        return  instance;
    }

    public UpdateAppService.ShowNotification getShowNotification() {
        return showNotification;
    }

    private UpdateAppService.ShowNotification showNotification = new UpdateAppService.ShowNotification() {
        @Override
        public void showStartNotification(PendingIntent pendingIntent, String title) {

            mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            mBuilder = new NotificationCompat.Builder(context);
            mBuilder.setSmallIcon(R.drawable.app_icon)
                    .setContentIntent(pendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(~Notification.DEFAULT_ALL)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentTitle(title)
                    .setProgress(100, 0, false);

            mNotification = mBuilder.build();
            mNotification.flags = Notification.FLAG_ONGOING_EVENT;

            mNotificationManager.cancel(NOTIFY_ID);
            mNotificationManager.notify(NOTIFY_ID, mNotification);
        }

        @Override
        public void showUpdateNotification(int progress, String title, String text) {

            mBuilder.setProgress(100, progress, false)
                    .setContentTitle(title)
                    .setContentText(text);
            mNotification = mBuilder.build();
            mNotificationManager.notify(NOTIFY_ID, mNotification);
        }

        @Override
        public void showSuccessNotification() {
            mNotificationManager.cancel(NOTIFY_ID);
        }

        @Override
        public void showFailNotification(PendingIntent pendingIntent, String title, String text) {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

            builder.setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setContentIntent(pendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)
                    .setOngoing(false);
            Notification notification = builder.build();
            mNotificationManager.cancel(NOTIFY_ID);
            mNotificationManager.notify(NOTIFY_ID, notification);

        }

    };
}
