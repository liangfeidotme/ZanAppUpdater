package com.youzan.genesis;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import com.youzan.genesis.info.DownloadInfo;
import com.youzan.genesis.info.VersionInfo;
import com.youzan.genesis.utils.DialogUtil;


/**
 * Created by Francis on 15/10/28.
 */
public class UpdateApp {

    private Context mContext;
    private String mName;
    private String mUrl;
    private String mTitle = "";
    private String mContent = "";
    private boolean mCancelable = true;
    private boolean mSilent = false;
    private OnSilentDownloadListener mOnSilentDownloadListener;
    private UpdateAppService.ServiceListener mServiceListener;

    private UpdateApp(Context context, String name, String url, String title, String content,
                      boolean cancelable, boolean silent, OnSilentDownloadListener listener) {
        this.mContext = context;
        this.mName = name;
        this.mUrl = url;
        this.mTitle = title;
        this.mContent = content;
        this.mCancelable = cancelable;
        this.mSilent = silent;
        this.mOnSilentDownloadListener = listener;
    }

    public static class Builder {

        private Context mBuilderContext;
        private String mBuilderName;
        private String mBuilderUrl;
        private String mBuilderTitle;
        private String mBuilderContent;
        private boolean mBuilderCancelableDialog;
        private boolean mBuilderSilent;
        private OnSilentDownloadListener mBuilderOnSilentDownloadListener;

        public Builder(Context context, String name, String url) {
            this.mBuilderContext = context;
            this.mBuilderName = name;
            this.mBuilderUrl = url;
        }

        public Builder title(String title) {
            this.mBuilderTitle = title;
            return this;
        }

        public Builder content(String content) {
            this.mBuilderContent = content;
            return this;
        }

        public Builder cancelableDialog(Boolean cancelableDialog) {
            this.mBuilderCancelableDialog = cancelableDialog;
            return this;
        }

        public Builder silent(boolean silent) {
            this.mBuilderSilent = silent;
            return this;
        }

        public Builder addSilentListener(OnSilentDownloadListener listener) {
            this.mBuilderOnSilentDownloadListener = listener;
            return this;
        }

        public UpdateApp build() {
            return new UpdateApp(mBuilderContext, mBuilderName, mBuilderUrl, mBuilderTitle,
                    mBuilderContent, mBuilderCancelableDialog, mBuilderSilent,
                    mBuilderOnSilentDownloadListener);
        }
    }

    public void showDialog() {
        if (mCancelable) {
            DialogUtil.showDialog(mContext, mTitle, mContent,
                    mContext.getString(R.string.update_app_now),
                    mContext.getString(R.string.update_app_next_time),
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
                    }, mCancelable
            );
        } else {
            DialogUtil.showDialogNoNegativeButton(mContext, mTitle, mContent, mContext.getString(R
                            .string.update_app_now),
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                            download();
                        }
                    }, mCancelable);
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
        final String apkName = mName + ".apk";
        if (mUrl != null) {
            ConnectivityManager conMan = (ConnectivityManager) mContext.getSystemService(Context
                    .CONNECTIVITY_SERVICE);

            //有些平板不支持ConnectivityManager.TYPE_MOBILE类型
            NetworkInfo mobileNetworkInfo = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo.State mobile = null;
            if (null != mobileNetworkInfo) {
                mobile = mobileNetworkInfo.getState();
            }

            NetworkInfo.State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                    .getState();

            if ((NetworkInfo.State.CONNECTED == mobile || NetworkInfo.State.CONNECTING == mobile)
                    && NetworkInfo.State.CONNECTED != wifi
                    && NetworkInfo.State.CONNECTING != wifi) {
                DialogUtil.showDialog(mContext, R.string.download_network_tip, R.string.confirm,
                        new DialogUtil.OnClickListener() {
                            @Override
                            public void onClick() {
                                openUpdateService(mUrl, apkName);
                            }
                        }, false);
            } else {
                openUpdateService(mUrl, apkName);
            }
        }
    }

    /**
     * 启动后台下载
     */
    private void openUpdateService(final String url, final String fileName) {

        Intent updateIntent = new Intent(mContext, UpdateAppService.class);
        DownloadInfo info = new DownloadInfo();
        info.setDownloadUrl(url);
        info.setFileName(fileName);
        updateIntent.putExtra(UpdateAppService.ARG_DOWNLOAD_INFO, info);
        updateIntent.putExtra(UpdateAppService.ARG_SILENT, mSilent);

        mServiceListener = new UpdateAppService.ServiceListener() {

            @Override
            public void onSuccess(Uri fileUri) {
                if (mOnSilentDownloadListener != null) {
                    mOnSilentDownloadListener.onSuccess(fileUri);
                }
            }

            @Override
            public void onError(String msg) {
                if (mOnSilentDownloadListener != null) {
                    mOnSilentDownloadListener.onError(msg);
                }
            }
        };

        UpdateAppService.addServiceListener(mServiceListener);
        mContext.startService(updateIntent);
    }

    public interface OnSilentDownloadListener {
        void onSuccess(Uri fileUri);

        void onError(String msg);
    }
}