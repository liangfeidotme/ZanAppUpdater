package com.youzan.genesis.genesisupdater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.youzan.genesis.UpdateAppService;
import com.youzan.genesis.UpdateAppUtil;
import com.youzan.genesis.info.VersionInfo;

import java.io.IOException;

/**
 * Created by Francis on 15/10/28.
 * 在主界面{@link #onStart()}的时候检查更新
 * 是否需要更新的请求由项目发起
 * 由项目显示通知
 */
public class MainActivity extends AppCompatActivity {

    /**
     * for test
     */
    private static final String RESPONSE = "response";
    private static String CHECK_URL = "http://open.koudaitong.com/api/entry?v=1.0&sign=888d3eadc51ab458ecab5407c8d8816d&method=wsc.version.valid&sign_method=md5&version=3.0.0&app_id=a424d52df7f0723d6a33&timestamp=2015-11-02+11:49:28&type=android&format=json";
    //private static String CHECK_URL = "http://open.koudaitong.com/api/entry?sign=a4dfd87aba2d950fa74e1182fbac653c&sign_method=md5&timestamp=2015-11-04+11:52:20&v=1.0&method=wsc.version.valid&app_id=a424d52df7f0723d6a33&format=json&type=android&version=3.1.1";
    private static final String PREF_VERSION_CHECK_TIME = "PREF_VERSION_CHECK_TIME";
    private static final long VERSION_CHECK_INTERVAL = 2 * 24 * 60 * 60 * 1000;
    private static final int NOTIFY_ID = 0xA1;

    private NotificationManager mNotificationManager = null;
    private Notification mNotification = null;
    private NotificationCompat.Builder mBuilder = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.check_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUpdate(CHECK_URL, PREF_VERSION_CHECK_TIME, VERSION_CHECK_INTERVAL);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUpdate(CHECK_URL, PREF_VERSION_CHECK_TIME, VERSION_CHECK_INTERVAL);
    }

    /**
     * 项目发起检查版本的请求，传递Apk默认文件名、图片资源和返回的VersionInfo
     */
    private void checkUpdate(String checkUrl, final String prefName, long checkInterval) {
        final SharedPreferences preferences = MyApplication.getInstance().getPrefs();
        long lastTime = preferences.getLong(prefName, 0);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < checkInterval) {
            // for test
            // return;
        }

        OkHttpClient mOkHttpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(checkUrl)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                System.out.print(request.toString());
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                if (response.code() == 200) {
                    JsonParser jsonParser = new JsonParser();
                    JsonObject jsonObject = jsonParser.parse(response.body().string()).getAsJsonObject();
                    JsonElement jsonElement = jsonObject.get(RESPONSE);

                    final VersionInfo versionInfo = new Gson().fromJson(jsonElement, VersionInfo.class);
                    if (null != versionInfo) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                UpdateAppService.setShowNotification(showNotification);

                                UpdateAppUtil.getInstance(MainActivity.this, "有赞微商城").showDialog(versionInfo);

                                MyApplication.getInstance().getPrefs().edit().putLong(prefName, System.currentTimeMillis()).apply();
                            }
                        });
                    }
                }
            }
        });
    }

    private UpdateAppService.ShowNotification showNotification = new UpdateAppService.ShowNotification() {
        @Override
        public void showStartNotification(PendingIntent pendingIntent,String title) {

            if (null == mNotificationManager) {
                mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            }

            if (mBuilder == null) {
                mBuilder = new NotificationCompat.Builder(MainActivity.this);

                mBuilder.setSmallIcon(R.drawable.app_icon)
                        .setContentIntent(pendingIntent)
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(~Notification.DEFAULT_ALL)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setContentTitle(title)
                        .setProgress(100, 0, false);
            }

            mNotification = mBuilder.build();
            mNotification.flags = Notification.FLAG_ONGOING_EVENT;

            mNotificationManager.cancel(NOTIFY_ID);
            mNotificationManager.notify(NOTIFY_ID, mNotification);
        }

        @Override
        public void showUpdateNotification(int progress, String title, String context) {
            mBuilder.setProgress(100, progress, false)
                    .setContentTitle(title)
                    .setContentText(context);
            mNotification = mBuilder.build();
            mNotificationManager.notify(NOTIFY_ID, mNotification);
        }

        @Override
        public void showSuccessNotification() {
            mNotificationManager.cancel(NOTIFY_ID);
        }

        @Override
        public void showFailNotification(PendingIntent pendingIntent,String title,String context) {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this);

            builder.setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(title)
                    .setContentText(context)
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
