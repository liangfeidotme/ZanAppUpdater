package com.youzan.genesis.utils;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.SpannableStringBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.youzan.genesis.DownloadInfo;
import com.youzan.genesis.R;
import com.youzan.genesis.UpdateAppService;
import com.youzan.genesis.VersionInfo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by Francis on 15/10/28.
 */
public class UpdateAppUtil {

    private static final long WSC_VERSION_CHECK_INTERVAL = 2 * 24 * 60 * 60 * 1000;//每2天检测一次版本

    private final Context context;

    private UpdateAppUtil(Context context){
        this.context = context;
    }

    public static UpdateAppUtil getInstance(Context context) {
        return new UpdateAppUtil(context);
    }

    /**
     * 检测版本号
     *
     * @param callback
     * @param checkTime 是否要根据时间周期判断是否检测，在设置页不需要
     */
    public void checkVersion(String version,long lastTime,final boolean checkTime,final CheckVersionSuccessCallback callback){


        long currentTime = System.currentTimeMillis();
        if(checkTime && currentTime - lastTime < WSC_VERSION_CHECK_INTERVAL){
            // TODO: 15/10/28 测试 每次都通过
            //return;
        }

        Ion.with(context)
                .load(getVersionCheckUrl(version))
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (null == e) {
                            if (result.has(RESPONSE)) {
                                VersionInfo versionInfo = new Gson().fromJson(result.get(RESPONSE), VersionInfo.class);
                                if (null != versionInfo) {
                                    if (null != callback) {
                                        callback.onCheckVersionSuccess(versionInfo);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    //版本控制
    private static final String APP_VERSION_VALID = "wsc.version.valid";

    private static final String RESPONSE = "response";

    private  String getVersionCheckUrl(String version) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("version", version);
        params.put("type", "android");

        return KDT_REGISTER_API_URL_HEAD + getParamStr(APP_VERSION_VALID, params);
    }

    private static final String KDT_REGISTER_API_URL_HEAD = "http://open.koudaitong.com/api/entry?";

    private String getParamStr(String method, Map<String, String> parames){
        String str = "";
        try {
            str = URLEncoder.encode(buildParamStr(buildCompleteParams(method, parames)), "UTF-8")
                    .replace("%3A", ":")
                    .replace("%2F", "/")
                    .replace("%26", "&")
                    .replace("%3D", "=")
                    .replace("%3F", "?");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return str;
    }

    private String buildParamStr(Map<String, String> param){
        String paramStr = "";
        Object[] keyArray = param.keySet().toArray();
        for(int i = 0; i < keyArray.length; i++){
            String key = (String)keyArray[i];

            if(0 == i){
                paramStr += (key + "=" + param.get(key));
            }
            else{
                paramStr += ("&" + key + "=" + param.get(key));
            }
        }

        return paramStr;
    }

    private Map<String, String> buildCompleteParams(String method, Map<String, String> parames) throws Exception{
        Map<String, String> commonParams = getCommonParams(method);
        for (String key : parames.keySet()) {
            if(commonParams.containsKey(key)){
                throw new Exception("参数名冲突");
            }

            commonParams.put(key, parames.get(key));
        }

        commonParams.put(SIGN_KEY, sign(commonParams));
        return commonParams;
    }


    public static final String APP_ID = "a424d52df7f0723d6a33";
    private static final String APP_SECRET = "2732d7464e3b8e53a983ee95d8e0df03";
    private static final String VERSION = "1.0";
    private static final String FORMAT = "json";
    private static final String SIGN_METHOD = "md5";

    private static final String APP_ID_KEY = "app_id";
    private static final String METHOD_KEY = "method";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String VERSION_KEY = "v";
    private static final String FORMAT_KEY = "format";
    private static final String SIGN_KEY = "sign";
    private static final String SIGN_METHOD_KEY = "sign_method";

    private Map<String, String> getCommonParams(String method){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        Map<String, String> parames = new HashMap<String, String>();
        parames.put(APP_ID_KEY, APP_ID);
        parames.put(METHOD_KEY, method);
        parames.put(TIMESTAMP_KEY, simpleDateFormat.format(new Date()));
        parames.put(FORMAT_KEY, FORMAT);
        parames.put(SIGN_METHOD_KEY, SIGN_METHOD);
        parames.put(VERSION_KEY, VERSION);
        return parames;
    }

    private String sign(Map<String, String> parames){
        Object[] keyArray = parames.keySet().toArray();
        List<String> keyList = new ArrayList<String>();
        for(int i = 0; i < keyArray.length; i++){
            keyList.add((String)keyArray[i]);
        }
        Collections.sort(keyList);
        String signContent = APP_SECRET;

        for (String key : keyList) {
            signContent += (key + parames.get(key));
        }
        signContent += APP_SECRET;
        return hash(signContent);
    }

    private String hash(String signContent){
        String hashResult = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(signContent.getBytes("UTF-8"));
            byte byteData[] = md.digest();
            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < byteData.length; i++){
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            hashResult = sb.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }

        return hashResult;
    }











    public void downloadAppApk(final VersionInfo versionInfo){
        final String upgradeUrl;
        final String apkName;
        final long apkSize;
        if(null == versionInfo){
            upgradeUrl = context.getString(R.string.update_address);
            apkName = getApkFileName(null);
            apkSize = -100;
        }
        else{
            upgradeUrl = versionInfo.getUpgradeUrl();
            apkName = getApkFileName(versionInfo.getVersionName());
            apkSize = versionInfo.getFileSize();
        }

        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        //有些平板不支持ConnectivityManager.TYPE_MOBILE类型
        NetworkInfo mobileNetworkInfo = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo.State mobile = null;
        if(null != mobileNetworkInfo) {
            mobile = mobileNetworkInfo.getState();//mobile 3G Data Network
        }
        NetworkInfo.State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();//wifi

        //只连了3G
        if((NetworkInfo.State.CONNECTED == mobile || NetworkInfo.State.CONNECTING == mobile)
                && NetworkInfo.State.CONNECTED != wifi && NetworkInfo.State.CONNECTING != wifi){
            DialogUtil.showDialog(context, R.string.download_network_tip, R.string.confirm,
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                            downloadFile(upgradeUrl, apkName, apkSize);
                        }
                    }, false);
        }
        else {
            downloadFile(upgradeUrl, apkName, apkSize);
        }
    }

    /**
     * 获取应用文件名(格式如: wsc_v3.0.0.apk)
     */
    public String getApkFileName(String versionName) {
        String appName = context.getString(R.string.app_short_name);

        if (StringUtil.isEmpty(versionName)) {
            return appName + ".apk";
        }
        return appName + "_v" + versionName + ".apk";
    }

    /**
     * 下载文件
     */
    public void downloadFile(String upgradeUrl, String apkName, long fileSize) {
        openUpdateService(upgradeUrl, apkName, fileSize);
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
        bundle.putParcelable(UpdateAppService.DOWNLOAD_INFO, info);
        updateIntent.putExtras(bundle);
        context.startService(updateIntent);
    }

    /**
     * 是否为新版本
     */
    public static boolean haveNewVersion(VersionInfo versionInfo) {
        return null != versionInfo && versionInfo.isNeedUpgrade();
    }

    /**
     * 是否为可维护
     */
    public static boolean isVersionValid(VersionInfo versionInfo){
        return null != versionInfo && versionInfo.isValid();
    }

    /**
     * 根据返回的 version info 弹框
     * 提示更新
     * @param versionInfo
     */
    public void showUpdateVersionDialog(final VersionInfo versionInfo){
        //只显示title
        if("".equals(versionInfo.getContent())){
            DialogUtil.showDialog(context, versionInfo.getTitle(),
                    context.getString(R.string.update_app_now),
                    context.getString(R.string.update_app_next_time),
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                            downloadAppApk(versionInfo);
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
        else{
            SpannableStringBuilder span = buildContentText(versionInfo);
            DialogUtil.showDialog(context, versionInfo.getTitle(), span,
                    context.getString(R.string.update_app_now),
                    context.getString(R.string.update_app_next_time),
                    new DialogUtil.OnClickListener() {
                        @Override
                        public void onClick() {
                            downloadAppApk(versionInfo);
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
     */
    public void showUpdateVersionDialog(){
        DialogUtil.showDialogNoNegativeButton(context, R.string.please_update_to_newest_version_hard, R.string.update_app_now,
                new DialogUtil.OnClickListener() {
                    @Override
                    public void onClick() {
                        downloadAppApk(null);
                    }
                }, false);
    }

    private SpannableStringBuilder buildContentText(VersionInfo versionInfo) {
        if (versionInfo == null) {
            return new SpannableStringBuilder();
        }
        String fileSizeText = String.format("\n安装包大小: %s", StringUtil.friendlyFileSize(versionInfo.getFileSize()));
        String contentText = String.format("%s\n%s", versionInfo.getContent(), fileSizeText).replace(";", "\n");

        return new SpannableStringBuilder(contentText);
    }

    public interface CheckVersionSuccessCallback{
        void onCheckVersionSuccess(VersionInfo versionInfo);
    }

}
