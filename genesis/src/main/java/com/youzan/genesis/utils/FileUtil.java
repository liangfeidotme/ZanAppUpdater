package com.youzan.genesis.utils;

import android.os.Environment;

import java.io.File;

/**
 * Created by Francis on 15/10/28.
 */
public class FileUtil {

    private static String SDPATH = Environment.getExternalStorageDirectory() + File.separator;
    private static String APP_PATH = SDPATH + "koudaitong" + File.separator;
    private static String DOWNLOAD_PATH = APP_PATH + "download" + File.separator;

    public static boolean isSDCardStateOn(){
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static boolean isSDCardReadOnly(){
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState());
    }

    public static String getDownloadAppFilePath(String apkName){
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
}
