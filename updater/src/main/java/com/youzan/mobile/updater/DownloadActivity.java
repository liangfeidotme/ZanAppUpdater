package com.youzan.mobile.updater;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class DownloadActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "UpdaterActivity";

    private static final String STATE_DOWNLOAD_ID = "state_download_id";
    private static final String STATE_DOWNLOAD_STATUS = "state_download_status";

    public static final String EXTRA_STRING_URL = "extra_url";
    public static final String EXTRA_STRING_TITLE = "extra_title";
    public static final String EXTRA_STRING_CONTENT = "extra_message";
    public static final String EXTRA_BOOLEAN_FORCE = "extra_force";

    public static final String EXTRA_STRING_APP_NAME = "extra_download_app_name";
    public static final String EXTRA_STRING_DESCRIPTION = "extra_download_description";

    // internal data
    private int status;
    private long downloadId;

    // data from outside
    private String appName;
    private String description;
    private String downloadUrl;
    private boolean force;

    private Uri apkUri;
    private DownloadManager downloadManager;
    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case DownloadManager.ACTION_DOWNLOAD_COMPLETE:
                    onDownloadCompleted(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0));
                    break;
            }
        }
    };

    // ui
    private Button downloadBtn;
    private Button cancelBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updater);

        final Intent intent = getIntent();
        appName = intent.getStringExtra(EXTRA_STRING_APP_NAME);
        description = intent.getStringExtra(EXTRA_STRING_DESCRIPTION);
        downloadUrl = intent.getStringExtra(EXTRA_STRING_URL);
        force = intent.getBooleanExtra(EXTRA_BOOLEAN_FORCE, false);

        // title
        setTitle(intent.getStringExtra(EXTRA_STRING_TITLE));

        // content
        ((TextView) findViewById(R.id.content)).setText(
                intent.getStringExtra(EXTRA_STRING_CONTENT));

        // cancel button
        cancelBtn = (Button) findViewById(R.id.cancel);
        cancelBtn.setVisibility(force ? View.GONE : View.VISIBLE);
        cancelBtn.setOnClickListener(this);

        // ok button
        downloadBtn = (Button) findViewById(R.id.ok);
        downloadBtn.setOnClickListener(this);

        // market button
        findViewById(R.id.market).setOnClickListener(this);

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_DOWNLOAD_ID, downloadId);
        outState.putInt(STATE_DOWNLOAD_STATUS, status);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        downloadId = savedInstanceState.getLong(STATE_DOWNLOAD_ID);
        status = savedInstanceState.getInt(STATE_DOWNLOAD_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);

        if (downloadId != 0) {
            apkUri = queryDownloadedFileUri(downloadId);
            if (apkUri != null) {
                status = STATUS_INSTALL;
            }
        }

        setStatus(status);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(downloadReceiver);
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.ok) {
            switch (status) {
                case STATUS_RETRY:
                case STATUS_DOWNLOAD:
                    downloadApk();
                    break;
                case STATUS_INSTALL:
                    installApk(apkUri);
                    break;
                case STATUS_DOWNLOADING:
                default:
                    break;
            }
        } else if (id == R.id.cancel) {
            finish();
        } else if (id == R.id.market) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + getPackageName()));
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        if (force || status == STATUS_DOWNLOADING) return;
        super.onBackPressed();
    }

    private static final int STATUS_DOWNLOAD = 0x00;
    private static final int STATUS_DOWNLOADING = 0x01;
    private static final int STATUS_INSTALL = 0x02;
    private static final int STATUS_RETRY = 0x03;

    private void setStatus(final int status) {
        this.status = status;
        switch (status) {
            case STATUS_DOWNLOAD:
                downloadBtn.setText(R.string.app_updater_download_now);
                downloadBtn.setEnabled(true);
                break;
            case STATUS_DOWNLOADING:
                downloadBtn.setText(R.string.app_updater_downloading);
                downloadBtn.setEnabled(false);
                if (!force) {
                    cancelBtn.setVisibility(View.GONE);
                }
                break;
            case STATUS_INSTALL:
                downloadBtn.setText(R.string.app_updater_install);
                downloadBtn.setEnabled(true);
                if (!force) {
                    cancelBtn.setVisibility(View.VISIBLE);
                }
                break;
            case STATUS_RETRY:
                downloadBtn.setText(R.string.app_updater_retry);
                downloadBtn.setEnabled(true);
                if (!force) {
                    cancelBtn.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    private void installApk(final Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        Uri apkUri = uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            apkUri = FileProvider.getUriForFile(this, getPackageName() + ".provider",
                    new File(uri.getPath()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        startActivity(intent);
    }

    private void downloadApk() {
        if (TextUtils.isEmpty(downloadUrl)) return;

        // Some device may not provide DownloadManager, so forward to browser.
        if (downloadManager == null) {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(downloadUrl));
            startActivity(intent);
            return;
        }

        // check dir
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!path.exists() && !path.mkdirs()) {
            Toast.makeText(this, String.format(getString(R.string.app_updater_dir_not_found),
                    path.getPath()), Toast.LENGTH_SHORT).show();
            return;
        }

        /** construct request */
        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE
                | DownloadManager.Request.NETWORK_WIFI);
        request.setAllowedOverRoaming(false);

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                appName + ".apk");


        if (!TextUtils.isEmpty(appName)) {
            request.setTitle(appName);
        }

        if (!TextUtils.isEmpty(description)) {
            request.setDescription(description);
        } else {
            request.setDescription(downloadUrl);
        }

        /** start downloading */
        downloadId = downloadManager.enqueue(request);
        setStatus(STATUS_DOWNLOADING);
    }

    private void onDownloadCompleted(final long downloadId) {
        this.downloadId = downloadId;

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        final Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            final int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                setStatus(STATUS_INSTALL);
                apkUri = Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
            } else if (status == DownloadManager.STATUS_FAILED) {
                setStatus(STATUS_RETRY);
            } else {
                setStatus(STATUS_DOWNLOADING);
            }
        }
    }

    @Nullable
    private Uri queryDownloadedFileUri(final long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        final Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            final int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                setStatus(STATUS_INSTALL);
                return Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
            }
        }
        return null;
    }
}

