package com.youzan.genesis.genesisupdater;

import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
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
import com.youzan.genesis.UpdateAppUtil;
import com.youzan.genesis.info.VersionInfo;

import java.io.IOException;

/**
 * Created by Francis on 15/10/28.
 * 在主界面{@link #onStart()}的时候检查更新
 * 是否需要更新的接口开放在外面
 */
public class MainActivity extends AppCompatActivity {

    private static final String RESPONSE = "response";

    String iconName = "app_icon";

    /**
     * for test
     */
    private static String CHECK_URL = "http://open.koudaitong.com/api/entry?v=1.0&sign=888d3eadc51ab458ecab5407c8d8816d&method=wsc.version.valid&sign_method=md5&version=3.0.0&app_id=a424d52df7f0723d6a33&timestamp=2015-11-02+11:49:28&type=android&format=json";
    //private static String CHECK_URL = "http://open.koudaitong.com/api/entry?sign=a4dfd87aba2d950fa74e1182fbac653c&sign_method=md5&timestamp=2015-11-04+11:52:20&v=1.0&method=wsc.version.valid&app_id=a424d52df7f0723d6a33&format=json&type=android&version=3.1.1";
    private static final String PREF_VERSION_CHECK_TIME = "PREF_VERSION_CHECK_TIME";
    private static final long VERSION_CHECK_INTERVAL = 2 * 24 * 60 * 60 * 1000;

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
     *
     * 项目发起检查版本的请求，传递Apk默认文件名、图片资源和返回的VersionInfo
     */
    private void checkUpdate(String checkUrl, final String prefName, long checkInterval) {
        final SharedPreferences preferences = MyApplication.getInstance().getPrefs();
        long lastTime = preferences.getLong(prefName, 0);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < checkInterval) {
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

                                UpdateAppUtil.getInstance(MainActivity.this, "wsc", BitmapFactory.decodeResource(getResources(), R.drawable.app_icon)).showDialog(versionInfo);

                                MyApplication.getInstance().getPrefs().edit().putLong(prefName, System.currentTimeMillis()).apply();
                            }
                        });
                    }
                }
            }
        });
    }

}
