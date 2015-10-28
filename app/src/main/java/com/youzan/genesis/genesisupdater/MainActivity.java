package com.youzan.genesis.genesisupdater;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.youzan.genesis.utils.UpdateAppUtil;
import com.youzan.genesis.VersionInfo;

/**
 * Created by Francis on 15/10/28.
 */
public class MainActivity extends AppCompatActivity {

    private static final String WSC_VERSION_CHECK_TIME = "WSC_VERSION_CHECK_TIME";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.check_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkVersion();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        checkVersion();
    }

    private void checkVersion() {
        final SharedPreferences preferences = MyApplication.getInstance().getPrefs();

        String version = MyApplication.getInstance().getVersionName();

        long lastTime = preferences.getLong(WSC_VERSION_CHECK_TIME, 0);

        UpdateAppUtil.getInstance(this).checkVersion(version, lastTime, true, new UpdateAppUtil.CheckVersionSuccessCallback() {
            @Override
            public void onCheckVersionSuccess(VersionInfo versionInfo) {

                preferences.edit().putLong(WSC_VERSION_CHECK_TIME, System.currentTimeMillis()).apply();

                if (!UpdateAppUtil.isVersionValid(versionInfo)) {
                    UpdateAppUtil.getInstance(MainActivity.this).showUpdateVersionDialog();
                } else if (UpdateAppUtil.haveNewVersion(versionInfo)) {
                    UpdateAppUtil.getInstance(MainActivity.this).showUpdateVersionDialog(versionInfo);
                }
            }
        });
    }


}
