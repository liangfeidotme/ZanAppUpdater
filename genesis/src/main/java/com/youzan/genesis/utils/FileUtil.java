package com.youzan.genesis.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;

import com.youzan.genesis.info.DownloadInfo;

import java.io.File;
import java.util.Date;

/**
 * Created by Francis on 15/10/28.
 */
public class FileUtil {

    /**
     * apk file 存放路径
     */
    private static String SDPATH = Environment.getExternalStorageDirectory() + File.separator;
    private static String APP_PATH = SDPATH + "koudaitong" + File.separator;
    private static String DOWNLOAD_PATH = APP_PATH + "download" + File.separator;

    public static boolean isSDCardStateOn(){
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static boolean isSDCardReadOnly(){
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState());
    }

    public static String getDownloadApkFilePath(String apkName){
        return DOWNLOAD_PATH + apkName;
    }

    public static void deleteFile(String filePath) {
        if (null != filePath) {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public static boolean  checkApkFileCreatedTime(DownloadInfo downloadInfo) {
        if (downloadInfo == null) {
            return true;
        }
        File apkFile = new File(downloadInfo.getFilePath());
        long lastTime = apkFile.lastModified();
        long nowTime = new Date().getTime();
        return nowTime - lastTime > 10 * 60 * 1000;
        //for test;
        //return true;
    }

    public static boolean checkApkFileValid(Context context,DownloadInfo downloadInfo,String apkPath) {
        boolean valid;
        // 创建时间大于10min，不再有效
        if (checkApkFileCreatedTime(downloadInfo)) {
            valid = false;
        } else {
            try {
                PackageManager pManager = context.getPackageManager();
                PackageInfo pInfo = pManager.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
                if (pInfo == null) {
                    valid = false;
                } else {
                    valid = true;
                }
            } catch (Exception e) {
                valid = false;
                e.printStackTrace();
            }
        }
        return valid;
    }

    public static boolean checkApkFileExist(File apkFile) {
        if (StringUtil.isEmpty(apkFile.getPath())) {
            return false;
        }
        return apkFile.exists() && apkFile.isFile();
    }

    public static void install(Context context,File apkFile) {
        Uri uri = Uri.fromFile(apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    public static void checkToCreateApkFile(File file){
        if (file.exists() && file.isFile()){
            return;
        }
        File parent = file.getParentFile();
        parent.mkdirs();
    }

}
