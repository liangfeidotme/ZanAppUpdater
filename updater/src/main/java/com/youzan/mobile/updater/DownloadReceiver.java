package com.youzan.mobile.updater;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class DownloadReceiver extends BroadcastReceiver {
    public DownloadReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case DownloadManager.ACTION_DOWNLOAD_COMPLETE:
                onDownloadCompleted(context,
                        intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0));
                break;
        }
    }

    private void onDownloadCompleted(final Context context, final long downloadId) {
        final DownloadManager downloadManager = (DownloadManager) context.getSystemService(
                Context.DOWNLOAD_SERVICE);

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        final Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            final int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                installApk(context, Uri.parse(c.getString(c.getColumnIndex(
                        DownloadManager.COLUMN_LOCAL_URI))));
            } else if (status == DownloadManager.STATUS_FAILED) {
                final int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                Toast.makeText(context,
                        context.getString(R.string.app_updater_download_failed, reason),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void installApk(final Context context, final Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Uri apkUri = uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider",
                    new File(uri.getPath()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }
}
