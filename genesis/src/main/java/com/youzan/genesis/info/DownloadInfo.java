package com.youzan.genesis.info;

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
     * 文件名
     */
    private String fileName;
    /**
     * 下载链接
     */
    private String downloadUrl;

    public DownloadInfo() {
    }

    protected DownloadInfo(Parcel in) {
        this.fileName = in.readString();
        this.downloadUrl = in.readString();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.fileName);
        dest.writeString(this.downloadUrl);
    }
}
