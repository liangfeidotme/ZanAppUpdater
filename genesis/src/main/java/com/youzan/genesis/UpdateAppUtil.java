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
public class UpdateAppUtil {

    private Context context;
    private String defaultAppName;
    private int iconId;

    private static UpdateAppUtil updateAppUtil;

    private UpdateAppUtil(Context context, String defaultAppName, int iconId) {
        this.context = context;
        this.defaultAppName = defaultAppName;
        this.iconId = iconId;
    }

    public static UpdateAppUtil getInstance(Context context, String defaultAppName, int iconId) {
        if (updateAppUtil == null)
            updateAppUtil = new UpdateAppUtil(context, defaultAppName, iconId);
        return updateAppUtil;
    }


    /**
     * 根据返回的 version info 弹框
     * 提示更新
     *
     * @param versionInfo
     */
    public void showUpdateVersionDialog(final VersionInfo versionInfo) {
        //只显示title
        if ("".equals(versionInfo.getContent())) {
            DialogUtil.showDialog(context, versionInfo.getTitle(),
                    context.getString(R.string.update_app_now),
                    context.getString(R.string.update_app_next_time),
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                            readyTodownloadFile(versionInfo);
                        }
                    },
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                        }
                    }, false
            );
        }
        //显示title和content
        else {
            SpannableStringBuilder span = buildContentText(versionInfo);
            DialogUtil.showDialog(context, versionInfo.getTitle(), span,
                    context.getString(R.string.update_app_now),
                    context.getString(R.string.update_app_next_time),
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                            readyTodownloadFile(versionInfo);
                        }
                    },
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                        }
                    }, false
            );
        }
    }

    /**
     * 是否为新版本
     */
    public static boolean haveNewVersion(VersionInfo info) {
        if (info == null) {
            return false;
        }
        return info.isNeedUpgrade();
    }

    private void readyTodownloadFile(final VersionInfo versionInfo) {
        final String upgradeUrl = versionInfo.getUpgradeUrl();
        final String apkName = getApkFileName(versionInfo.getVersionName());
        final long apkSize = versionInfo.getFileSize();

        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        //有些平板不支持ConnectivityManager.TYPE_MOBILE类型
        NetworkInfo mobileNetworkInfo = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo.State mobile = null;
        if (null != mobileNetworkInfo) {
            mobile = mobileNetworkInfo.getState();//mobile 3G Data Network
        }
        NetworkInfo.State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();//wifi

        //只连了3G
        if ((NetworkInfo.State.CONNECTED == mobile || NetworkInfo.State.CONNECTING == mobile)
                && NetworkInfo.State.CONNECTED != wifi && NetworkInfo.State.CONNECTING != wifi) {
            DialogUtil.showDialog(context, R.string.download_network_tip, R.string.confirm,
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                            openUpdateService(upgradeUrl, apkName, apkSize);
                        }
                    }, false);
        } else {
            openUpdateService(upgradeUrl, apkName, apkSize);
        }
    }

    /**
     * 获取应用文件名(格式如: wsc_v3.0.0.apk)
     */
    private String getApkFileName(String versionName) {
        String appName = defaultAppName;

        if (StringUtil.isEmpty(versionName)) {
            return appName + ".apk";
        }
        return appName + "_v" + versionName + ".apk";
    }

    /**
     * 启动后台下载
     */
    private void openUpdateService(final String url, final String fileName, long fileSize) {
        Intent updateIntent = new Intent(context, UpdateAppService.class);
        Bundle bundle = new Bundle();
        DownloadInfo info = new DownloadInfo();
        info.setDownloadUrl(url);
        info.setFileName(fileName);
        info.setFileSize(fileSize);
        bundle.putParcelable(UpdateAppService.ARG_DOWNLOAD_INFO, info);
        bundle.putInt(UpdateAppService.ARG_APP_ICON, iconId);
        updateIntent.putExtras(bundle);
        context.startService(updateIntent);
    }

    private SpannableStringBuilder buildContentText(VersionInfo versionInfo) {
        if (versionInfo == null) {
            return new SpannableStringBuilder();
        }
        String fileSizeText = String.format("\n安装包大小: %s", StringUtil.friendlyFileSize(versionInfo.getFileSize()));
        String contentText = String.format("%s\n%s", versionInfo.getContent(), fileSizeText).replace(";", "\n");

        return new SpannableStringBuilder(contentText);
    }

}
