package com.youzan.genesis.genesisupdater;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.youzan.genesis.UpdateApp;

public class MainActivity extends AppCompatActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new UpdateApp.Builder(MainActivity.this,
                        "有赞", "http://www.eoemarket.com/download/356118_0")
                        .title("title")
                        .content("content")
                        .cancelableDialog(true)
                        .build()
                        .showDialog();
            }
        });
        findViewById(R.id.download_silent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new UpdateApp.Builder(MainActivity.this,
                        "有赞", "http://www.eoemarket.com/download/356118_0")
                        .silent(true)
                        .addSilentListener(new UpdateApp.OnSilentDownloadListener() {
                            @Override
                            public void onSuccess(Uri fileUri) {
                                Toast.makeText(MainActivity.this, "Success: " + fileUri.toString(),
                                        Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(String msg) {
                                Toast.makeText(MainActivity.this, "Failed: " + msg,
                                        Toast.LENGTH_LONG).show();
                            }
                        })
                        .build()
                        .download();
            }
        });
    }

}