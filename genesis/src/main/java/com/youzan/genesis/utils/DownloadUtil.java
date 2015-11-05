package com.youzan.genesis.utils;

import android.os.Handler;
import android.os.Looper;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Created by Francis on 15/11/5.
 */
public class DownloadUtil {
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int DATA_TIMEOUT = 40000;
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

    private DownloadUtil(){
        this.handler = new Handler(Looper.getMainLooper());
    }

    public interface DownloadListener {
        void downloading(int progress);

        void downloaded();

        void downError(String error);
    }

    public void download(final String urlStr, final File dest, final boolean append, final DownloadListener downloadListener) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                int downloadProgress = 0;
                long remoteSize = 0;
                int currentSize = 0;
                long totalSize = -1;
                long lastDownload = 0L;

                if (!append && dest.exists() && dest.isFile()) {
                    dest.delete();
                }

                try {
                    if (append && dest.exists() && dest.isFile()) {
                        FileInputStream fis = new FileInputStream(dest);
                        currentSize = fis.available();
                        if (fis != null) {
                            fis.close();
                        }
                    }

                    HttpGet request = new HttpGet(urlStr);

                    if (currentSize > 0) {
                        request.addHeader("RANGE", "bytes=" + currentSize + "-");
                    }

                    HttpParams params = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(params, CONNECT_TIMEOUT);
                    HttpConnectionParams.setSoTimeout(params, DATA_TIMEOUT);
                    HttpClient httpClient = new DefaultHttpClient(params);

                    InputStream is = null;
                    FileOutputStream os = null;
                    HttpResponse response = httpClient.execute(request);
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        is = response.getEntity().getContent();
                        remoteSize = response.getEntity().getContentLength();
                        Header contentEncoding = response.getFirstHeader("Content-Encoding");
                        if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                            is = new GZIPInputStream(is);
                        }
                        os = new FileOutputStream(dest, append);
                        byte buffer[] = new byte[DATA_BUFFER];
                        int readSize = 0;
                        while ((readSize = is.read(buffer)) > 0) {
                            os.write(buffer, 0, readSize);
                            os.flush();
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
                        if (totalSize < 0) {
                            totalSize = 0;
                        }

                        if (os != null) {
                            os.close();
                        }
                        if (is != null) {
                            is.close();
                        }
                    }

                    if (totalSize < 0) {
                        if (downloadListener != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    downloadListener.downError("not downloaded,totalSize < 0");
                                }
                            });
                        }
                    }

                    if (downloadListener != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                downloadListener.downloaded();
                            }
                        });
                    }

                } catch (final Exception e) {
                    if (downloadListener != null){

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                downloadListener.downError(e.getMessage());
                            }
                        });

                    }

                } finally {

                }
                //return totalSize;
            }
        }).start();

    }
}
