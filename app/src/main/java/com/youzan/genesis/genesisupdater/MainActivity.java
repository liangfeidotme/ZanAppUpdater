package com.youzan.genesis.genesisupdater;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.youzan.mobile.updater.AppUpdater;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSION_REQUEST_STORAGE = 0x00;

    private boolean storagePermissionGranted = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!storagePermissionGranted) {
            requestStoragePermission();
        }

        findViewById(R.id.force_update).setOnClickListener(this);
        findViewById(R.id.normal_update).setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    storagePermissionGranted = true;
                } else {
                    requestStoragePermission();
                }
                break;
        }
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_STORAGE);
        }
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        switch (id) {
            case R.id.normal_update:
                onNormalUpdate();
                break;
            case R.id.force_update:
                onForceUpdate();
                break;
        }
    }

    private void onForceUpdate() {
        new AppUpdater.Builder(this)
                .url("https://dl.yzcdn.cn/koudaitong.apk")
                //.url("http://photocdn.sohu.com/20170223/Img481553167.jpeg")
                .title("版本更新啦")
                .content("1. 很牛逼\n2. 很厉害\n3. 吊炸天")
                .app("有赞微商城")
                .description("做生意 用有赞")
                .force(true)
                .build()
                .update();
    }

    private void onNormalUpdate() {
        new AppUpdater.Builder(this)
                .url("https://dl.yzcdn.cn/koudaitong.apk")
                .title("版本更新啦")
                .content("1. 很牛逼\n2. 很厉害\n3. 吊炸天")
                .app("有赞微商城")
                .description("做生意 用有赞")
//                .force()
                .build()
                .update();
    }
}
