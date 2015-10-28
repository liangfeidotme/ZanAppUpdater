package com.youzan.genesis.info;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Francis on 15/10/28.
 */
public class VersionInfo implements Parcelable {

    public static final Creator<VersionInfo> CREATOR = new Creator<VersionInfo>() {
        public VersionInfo createFromParcel(Parcel source) {
            return new VersionInfo(source);
        }

        public VersionInfo[] newArray(int size) {
            return new VersionInfo[size];
        }
    };

    @SerializedName("is_valid")
    private boolean isValid;//当前版本是否可用
    @SerializedName("need_upgrade")
    private boolean needUpgrade;//当前版本是否需要升级
    @SerializedName("title")
    private String title;//升级提示标题
    @SerializedName("content")
    private String content;//新版本更新内容（每条内容间以半角分号“;”划分）//处理成: \n
    @SerializedName("download")
    private String upgradeUrl;//新版本下载地址
    @SerializedName("file_size")
    private long fileSize;//新版本大小，字节
    @SerializedName("version")
    private String versionName;//新版本版本号

    public VersionInfo() {
    }

    private VersionInfo(Parcel in) {
        this.isValid = in.readByte() != 0;
        this.needUpgrade = in.readByte() != 0;
        this.title = in.readString();
        this.content = in.readString();
        this.upgradeUrl = in.readString();
        this.fileSize = in.readLong();
        this.versionName = in.readString();
    }

    public boolean isValid() {
        return isValid;
    }

    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    public boolean isNeedUpgrade() {
        return needUpgrade;
    }

    public void setNeedUpgrade(boolean needUpgrade) {
        this.needUpgrade = needUpgrade;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUpgradeUrl() {
        return upgradeUrl;
    }

    public void setUpgradeUrl(String upgradeUrl) {
        this.upgradeUrl = upgradeUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    @Override
    public String toString() {
        return "VersionInfo{" +
                "isValid=" + isValid +
                ", needUpgrade=" + needUpgrade +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", upgradeUrl='" + upgradeUrl + '\'' +
                ", fileSize='" + fileSize + '\'' +
                ", versionName='" + versionName + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(isValid ? (byte) 1 : (byte) 0);
        dest.writeByte(needUpgrade ? (byte) 1 : (byte) 0);
        dest.writeString(this.title);
        dest.writeString(this.content);
        dest.writeString(this.upgradeUrl);
        dest.writeLong(this.fileSize);
        dest.writeString(this.versionName);
    }
}
