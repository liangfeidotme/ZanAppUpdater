package com.youzan.genesis.genesisupdater;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.youzan.genesis.UpdateAppUtil;
import com.youzan.genesis.info.VersionInfo;

/**
 * Created by Francis on 15/10/28.
 * 在主界面{@link #onStart()}的时候检查更新
 * 是否需要更新的接口开放在外面
 */
public class MainActivity extends AppCompatActivity {

    private static final String RESPONSE = "response";

    /**
     * for test
     */
    private static String CHECK_URL = "http://open.koudaitong.com/api/entry?v=1.0&sign=888d3eadc51ab458ecab5407c8d8816d&method=wsc.version.valid&sign_method=md5&version=3.0.0&app_id=a424d52df7f0723d6a33&timestamp=2015-11-02+11:49:28&type=android&format=json";
    private static final String VERSION_CHECK_TIME = "VERSION_CHECK_TIME";
    private static final long VERSION_CHECK_INTERVAL = 2 * 24 * 60 * 60 * 1000;
    private static final String DEAULT_APP_NAME = "wsc";
    private static final int APP_ICON = R.drawable.app_icon;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.check_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUpdate(CHECK_URL, VERSION_CHECK_TIME, VERSION_CHECK_INTERVAL, DEAULT_APP_NAME, APP_ICON);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUpdate(CHECK_URL, VERSION_CHECK_TIME, VERSION_CHECK_INTERVAL, DEAULT_APP_NAME, APP_ICON);
    }

    /**
     * @param checkUrl       检查版本更新的Url
     * @param prefName       preference名称，纪录更新
     * @param checkInterval  检查更新的周期
     * @param defaultAppName apk名称前缀
     * @param appIcon        app icon,下载时显示notification
     */
    private void checkUpdate(String checkUrl, final String prefName, long checkInterval, final String defaultAppName, final int appIcon) {
        final SharedPreferences preferences = MyApplication.getInstance().getPrefs();
        long lastTime = preferences.getLong(prefName, 0);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < checkInterval) {
            return;
        }

        Ion.with(this)
                .load(checkUrl)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (null == e) {
                            if (result.has(RESPONSE)) {
                                VersionInfo versionInfo = new Gson().fromJson(result.get(RESPONSE), VersionInfo.class);
                                if (null != versionInfo) {
                                    //是否为最新版本
                                    if (UpdateAppUtil.haveNewVersion(versionInfo)) {
                                        UpdateAppUtil.getInstance(MainActivity.this, defaultAppName, appIcon).showUpdateVersionDialog(versionInfo);
                                    } else {
                                        MyApplication.getInstance().getPrefs().edit().putLong(prefName, System.currentTimeMillis()).apply();
                                    }
                                }
                            }
                        }
                    }
                });
    }

}
