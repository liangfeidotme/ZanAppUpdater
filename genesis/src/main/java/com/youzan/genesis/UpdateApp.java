package com.youzan.genesis;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.SpannableStringBuilder;

import com.youzan.genesis.info.DownloadInfo;
import com.youzan.genesis.info.VersionInfo;
import com.youzan.genesis.utils.DialogUtil;
import com.youzan.genesis.utils.StringUtil;

/**
 * Created by Francis on 15/10/28.
 */
public class UpdateApp {

    private Context context;
    private String name;
    private String title = "";
    private String content = "";
    private boolean cancelable = true;
    private String url;

    private UpdateApp(Context context, String name, String url, String title, String content, boolean cancelable) {
        this.context = context;
        this.name = name;
        this.url = url;
        this.title = title;
        this.content = content;
        this.cancelable = cancelable;
    }

    public static class Builder {

        private Context context;
        private String name;
        private String url;
        private String title;
        private String content;
        private boolean cancelableDialog;

        public Builder(Context context, String name, String url) {
            this.context = context;
            this.name = name;
            this.url = url;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder cancelableDialog(Boolean cancelableDialog) {
            this.cancelableDialog = cancelableDialog;
            return this;
        }

        public UpdateApp build() {
            return new UpdateApp(context, name, url, title, content, cancelableDialog);
        }
    }

    public void showDialog() {
        if (cancelable) {
            DialogUtil.showDialog(context, title, content,
                    context.getString(R.string.update_app_now),
                    context.getString(R.string.update_app_next_time),
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                            download();
                        }
                    },
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                        }
                    }, cancelable
            );
        } else {
            DialogUtil.showDialogNoNegativeButton(context, title, content, context.getString(R.string.update_app_now),
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                            download();
                        }
                    }, cancelable);
        }
    }

    /**
     * 是否为新版本
     */
    public static boolean haveNewVersion(VersionInfo info) {
        if (info == null) {
            return false;
        }
        return info.isNeed_upgrade();
    }

    /**
     * 是否为可维护
     */
    public static boolean isVersionValid(VersionInfo versionInfo) {
        return null != versionInfo && versionInfo.isIs_valid();
    }

    public void download() {
        final String apkName = name + ".apk";
        if (url != null) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            //有些平板不支持ConnectivityManager.TYPE_MOBILE类型
            NetworkInfo mobileNetworkInfo = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo.State mobile = null;
            if (null != mobileNetworkInfo) {
                mobile = mobileNetworkInfo.getState();
            }

            NetworkInfo.State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();

            if ((NetworkInfo.State.CONNECTED == mobile || NetworkInfo.State.CONNECTING == mobile)
                    && NetworkInfo.State.CONNECTED != wifi && NetworkInfo.State.CONNECTING != wifi) {
                DialogUtil.showDialog(context, R.string.download_network_tip, R.string.confirm,
                        new DialogUtil.OnClickListener() {
                            @Override
                            public void onClick() {
                                openUpdateService(url, apkName);
                            }
                        }, false);
            } else {
                openUpdateService(url, apkName);
            }
        }
    }

    /**
     * 启动后台下载
     */
    private void openUpdateService(final String url, final String fileName) {

        Intent updateIntent = new Intent(context, UpdateAppService.class);
        Bundle bundle = new Bundle();
        DownloadInfo info = new DownloadInfo();
        info.setDownloadUrl(url);
        info.setFileName(fileName);
        bundle.putParcelable(UpdateAppService.ARG_DOWNLOAD_INFO, info);
        updateIntent.putExtras(bundle);
        context.startService(updateIntent);
    }

    private SpannableStringBuilder buildContentText(VersionInfo versionInfo) {
        if (versionInfo == null) {
            return new SpannableStringBuilder();
        }
        String fileSizeText = String.format("\n安装包大小: %s", StringUtil.friendlyFileSize(versionInfo.getFile_size()));
        String contentText = String.format("%s\n%s", versionInfo.getContent(), fileSizeText).replace(";", "\n");

        return new SpannableStringBuilder(contentText);
    }

}
