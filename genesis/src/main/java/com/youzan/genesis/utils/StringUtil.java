package com.youzan.genesis.utils;

/**
 * Created by Francis on 15/10/28.
 */
public class StringUtil {

    public static final long KB = 1 << 10L;
    public static final long MB = 1 << 20L;
    public static final long GB = 1 << 30L;

    /**
     * 字符串是否为空
     * null 为 true
     * ""   为 true
     * “ ”  为 true
     * “ a” 为 false
     *
     * @param s
     * @return
     */
    public static boolean isEmpty(String s) {
        if (null == s) {
            return true;
        } else {
            if (s.trim().length() < 1) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * 友好的显示文件尺寸
     * 比如:0B, 120KB, 1022KB, 1.02MB, 1021MB, 1GB
     *
     * @param size byte
     */
    public static String friendlyFileSize(long size) {
        if (size < 0) {
            return "0B";
        } else if (size >= 0 && size < KB) {
            return String.format("%dB", size);
        } else if (size >= KB && size < MB) {
            return String.format("%.2fKB", size * 1.0 / KB);
        } else if (size >= MB && size < GB) {
            return String.format("%.2fMB", size * 1.0 / MB);
        } else {
            return String.format("%.2fGB", size * 1.0 / GB);
        }
    }

}
