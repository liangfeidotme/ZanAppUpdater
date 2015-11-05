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

    public static final String ARGS_APP_TYPE = "type";
    public static final String APP_TYPE_WSC = "有赞微商城";
    public static final String APP_TYPE_WXD = "有赞微小店";

    private Context context;
    private String defaultAppName;

    private static UpdateAppUtil updateAppUtil;

    private UpdateAppUtil(Context context, String type) {
        this.context = context;
        this.defaultAppName = type;
    }

    public static UpdateAppUtil getInstance(Context context, String type) {
        if (updateAppUtil == null)
            updateAppUtil = new UpdateAppUtil(context, type);
        return updateAppUtil;
    }


    public void showDialog(VersionInfo versionInfo){
        if (!isVersionValid(versionInfo)){
            showUpdateVersionDialog();
        }else if (haveNewVersion(versionInfo)){
            showUpdateVersionDialog(versionInfo);
        }
    }

    /**
     * 根据返回的 version info 弹框
     * 提示更新
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
     * 默认弹框
     * 强制更新
     * 供外部调用
     */
    public void showUpdateVersionDialog(){
        DialogUtil.showDialogNoNegativeButton(context, R.string.please_update_to_newest_version_hard, R.string.update_app_now,
                new DialogUtil.OnClickListener() {
                    @Override
                    public void onClick() {
                        readyTodownloadFile(null);
                    }
                }, false);
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
    public static boolean isVersionValid(VersionInfo versionInfo){
        return null != versionInfo && versionInfo.isIs_valid();
    }

    /**
     * 外部可以直接下载
     */
    public void readyTodownloadFile(final VersionInfo versionInfo) {

        final String upgradeUrl;
        final String apkName;
        final long apkSize;
        if(null == versionInfo){
            upgradeUrl = context.getString(R.string.update_address);
            apkName = getApkFileName(null);
            apkSize = -100;
        }
        else{
            upgradeUrl = versionInfo.getDownload();
            apkName = getApkFileName(versionInfo.getVersion());
            apkSize = versionInfo.getFile_size();
        }

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
        bundle.putString(ARGS_APP_TYPE, defaultAppName);
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
