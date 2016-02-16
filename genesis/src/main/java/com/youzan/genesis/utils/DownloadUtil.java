package com.youzan.genesis.utils;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Francis on 15/11/5.
 */
public class DownloadUtil {
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 40000;
    private final static int DATA_BUFFER = 8192;

    private Handler handler;
    private static DownloadUtil downloadUtil;

    public static DownloadUtil newInstance() {

        if (downloadUtil == null)
            synchronized (DownloadUtil.class) {
                if (downloadUtil == null) {
                    downloadUtil = new DownloadUtil();
                }
            }
        return downloadUtil;
    }

    private DownloadUtil() {
        this.handler = new Handler(Looper.getMainLooper());
    }

    public interface DownloadListener {
        void downloading(int progress);

        void downloaded();

        void downloadError(String error);
    }

    public void download(final String urlStr, final File dest, final boolean append, final DownloadListener downloadListener) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                int downloadProgress = 0;
                long remoteSize = 0;
                int currentSize = 0;
                long totalSize = 0;
                long lastDownload = 0L;
                InputStream inputStream = null;
                FileOutputStream fileOutputStream = null;
                HttpURLConnection conn = null;

                if (!append && dest.exists() && dest.isFile()) {
                    dest.delete();
                }

                try {
                    if (append && dest.exists() && dest.isFile()) {
                        FileInputStream fis = new FileInputStream(dest);
                        currentSize = fis.available();
                        totalSize = currentSize;
                        if (fis != null) {
                            fis.close();
                        }
                    }

                    URL url = new URL(urlStr);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(CONNECT_TIMEOUT);
                    conn.setReadTimeout(READ_TIMEOUT);
                    if (currentSize > 0) {
                        conn.addRequestProperty("RANGE", "bytes=" + currentSize + "-");
                    }
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                        inputStream = conn.getInputStream();
                        remoteSize = conn.getContentLength() + totalSize;

                        fileOutputStream = new FileOutputStream(dest, append);
                        byte buffer[] = new byte[DATA_BUFFER];
                        int readSize = 0;
                        while ((readSize = inputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, readSize);
                            fileOutputStream.flush();
                            totalSize += readSize;
                            if (downloadListener != null) {

                                // 防止过于频繁以致阻塞 每100KB刷新一次
                                if (totalSize - lastDownload > (StringUtil.MB / 10.0)) {
                                    lastDownload = totalSize;
                                    downloadProgress = (int) (totalSize * 100 / remoteSize);
                                    final int finalDownloadProgress = downloadProgress;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            downloadListener.downloading(finalDownloadProgress);
                                        }
                                    });
                                }
                            }
                        }
                    }

                    if (totalSize <= 0) {
                        if (downloadListener != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    downloadListener.downloadError("not downloaded,totalSize <＝ 0");
                                }
                            });
                        }
                    } else if (downloadListener != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                downloadListener.downloaded();
                            }
                        });
                    }

                } catch (final Exception e) {
                    if (downloadListener != null) {

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                downloadListener.downloadError(e.getMessage());
                            }
                        });
                    }

                } finally {
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();

    }
}