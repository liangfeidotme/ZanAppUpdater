package com.youzan.genesis.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.util.Date;

/**
 * Created by Francis on 15/10/28.
 */
public class FileUtil {

    public static boolean isSDCardStateOn() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static boolean isSDCardReadOnly() {
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState());
    }

    public static void deleteFile(String filePath) {
        if (null != filePath) {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public static boolean checkApkFileCreatedTime(File apkFile) {
        if (apkFile == null) {
            return true;
        }
        long lastTime = apkFile.lastModified();
        long nowTime = new Date().getTime();
        return nowTime - lastTime > 10 * 60 * 1000;
    }

    public static boolean checkApkFileValid(Context context, File apkFile) {
        boolean valid;
        // 创建时间大于10min，不再有效
        if (checkApkFileCreatedTime(apkFile)) {
            valid = false;
        } else {
            try {
                PackageManager pManager = context.getPackageManager();
                PackageInfo pInfo = pManager.getPackageArchiveInfo(apkFile.getPath(), PackageManager.GET_ACTIVITIES);
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

    public static void install(Context context, File apkFile) {
        Uri uri = Uri.fromFile(apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

}
