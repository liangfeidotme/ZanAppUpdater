package com.youzan.genesis.info;

import android.os.Parcel;
import android.os.Parcelable;


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

    private boolean is_valid;//当前版本是否可用
    private boolean need_upgrade;//当前版本是否需要升级
    private String title;//升级提示标题
    private String content;//新版本更新内容（每条内容间以半角分号“;”划分）//处理成: \n
    private String download;//新版本下载地址
    private long file_size;//新版本大小，字节
    private String version;//新版本版本号

    private VersionInfo(Parcel in) {
        this.is_valid = in.readByte() != 0;
        this.need_upgrade = in.readByte() != 0;
        this.title = in.readString();
        this.content = in.readString();
        this.download = in.readString();
        this.file_size = in.readLong();
        this.version = in.readString();
    }

    public boolean isIs_valid() {
        return is_valid;
    }

    public void setIs_valid(boolean isValid) {
        this.is_valid = isValid;
    }

    public boolean isNeed_upgrade() {
        return need_upgrade;
    }

    public void setNeed_upgrade(boolean need_upgrade) {
        this.need_upgrade = need_upgrade;
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

    public String getDownload() {
        return download;
    }

    public void setDownload(String download) {
        this.download = download;
    }

    public long getFile_size() {
        return file_size;
    }

    public void setFile_size(long file_size) {
        this.file_size = file_size;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "VersionInfo{" +
                "is_valid=" + is_valid +
                ", need_upgrade=" + need_upgrade +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", download='" + download + '\'' +
                ", file_size='" + file_size + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(is_valid ? (byte) 1 : (byte) 0);
        dest.writeByte(need_upgrade ? (byte) 1 : (byte) 0);
        dest.writeString(this.title);
        dest.writeString(this.content);
        dest.writeString(this.download);
        dest.writeLong(this.file_size);
        dest.writeString(this.version);
    }
}
