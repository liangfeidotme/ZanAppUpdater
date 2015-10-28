package com.youzan.genesis;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Francis on 15/10/28.
 */
public class DownloadInfo implements Parcelable {
    public static final Creator<DownloadInfo> CREATOR = new Creator<DownloadInfo>() {
        public DownloadInfo createFromParcel(Parcel source) {
            return new DownloadInfo(source);
        }

        public DownloadInfo[] newArray(int size) {
            return new DownloadInfo[size];
        }
    };
    /**
     * 文件名(全称, 比如:wsc_v3.0.0.apk)
     */
    private String fileName;
    /**
     * 下载链接
     */
    private String downloadUrl;
    /**
     * 文件保存路径
     */
    private String filePath;
    /**
     * 文件大小
     */
    private long fileSize;

    public DownloadInfo() {
    }

    protected DownloadInfo(Parcel in) {
        this.fileName = in.readString();
        this.downloadUrl = in.readString();
        this.filePath = in.readString();
        this.fileSize = in.readLong();
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.fileName);
        dest.writeString(this.downloadUrl);
        dest.writeString(this.filePath);
        dest.writeLong(this.fileSize);
    }
}
