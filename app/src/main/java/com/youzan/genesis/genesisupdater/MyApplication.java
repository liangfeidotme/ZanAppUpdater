package com.youzan.genesis.genesisupdater;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Created by Francis on 15/10/28.
 */
public class MyApplication extends Application {

    public static final String PREFS = "com.youzan.genesis.PREFS";

    private static SharedPreferences prefs;
    private static MyApplication application ;

    @Override
    public void onCreate() {
        super.onCreate();

        application = MyApplication.this;

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    public static MyApplication getInstance() {
        return application;
    }

    public SharedPreferences getPrefs(){
        return prefs;
    }


    public String getVersionName(){
        String versionName = "";
        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
            versionName = packInfo.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }
}

