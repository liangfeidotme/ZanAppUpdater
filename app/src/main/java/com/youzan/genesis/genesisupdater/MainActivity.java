package com.youzan.genesis.genesisupdater;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.youzan.genesis.UpdateAppUtil;
import com.youzan.genesis.info.VersionInfo;

/**
 * Created by Francis on 15/10/28.
 * 在主界面{@link #onStart()}的时候检查更新
 */
public class MainActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.check_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUpdate();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUpdate();
    }

    /**
     *  所需参数：
     *  lastTime                上次检查更新的时间
     *  version                 当前版本
     *  notificationIconId      下载时显示的App icon
     *  defaultApkName          默认的apk文件名（为获取到服务端）
     */
    private void checkUpdate() {

        final SharedPreferences preferences = MyApplication.getInstance().getPrefs();
        long lastTime = preferences.getLong(UpdateAppUtil.WSC_VERSION_CHECK_TIME, 0);

        String version = MyApplication.getInstance().getVersionName();

        int notificationIconId =  R.drawable.app_icon;
        String defaultApkName = "wsc";

        UpdateAppUtil.getInstance(this,defaultApkName,notificationIconId).checkVersion(version, lastTime, true, new UpdateAppUtil.CheckVersionSuccessCallback() {
            @Override
            public void onCheckVersionSuccess(VersionInfo versionInfo) {

                preferences.edit().putLong(UpdateAppUtil.WSC_VERSION_CHECK_TIME, System.currentTimeMillis()).apply();

            }
        });
    }

}
