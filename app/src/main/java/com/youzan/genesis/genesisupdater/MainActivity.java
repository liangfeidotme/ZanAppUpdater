package com.youzan.genesis.genesisupdater;

import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.youzan.genesis.UpdateApp;
import com.youzan.genesis.info.VersionInfo;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    /**
     * for test
     */
    private static final String RESPONSE = "response";

    private static String CHECK_URL = "http://open.koudaitong.com/api/entry?v=1"
            + ".0&sign=a02eb453b7851d0177a5194c86854636&method=wsc.version"
            + ".valid&sign_method=md5&version=3.6.0"
            + ".1&app_id=a424d52df7f0723d6a33&timestamp=2016-03-30%2018%3A42%3A37&type=android"
            + "&format=json";
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
                    JsonObject jsonObject = jsonParser.parse(response.body().string())
                            .getAsJsonObject();
                    JsonElement jsonElement = jsonObject.get(RESPONSE);

                    final VersionInfo versionInfo = new Gson().fromJson(jsonElement, VersionInfo
                            .class);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!UpdateApp.isVersionValid(versionInfo)
                                    || UpdateApp.haveNewVersion(versionInfo)) {
                                new UpdateApp.Builder(MainActivity.this, "有赞微商城", versionInfo
                                        .getDownload())
                                        .title(versionInfo.getTitle())
                                        .content(versionInfo.getContent())
                                        .cancelableDialog(true)
                                        .build()
                                        .showDialog();

                                MyApplication.getInstance().getPrefs().edit().putLong(prefName,
                                        System.currentTimeMillis()).apply();
                            }
                        }
                    });
                }
            }

        });
    }
}