package com.example.music_liujinhong.home;
import android.os.Parcel;
import android.os.Parcelable;
public class Music implements Parcelable {
    private String musicName;
    private String author;
    private String coverUrl;
    private String musicUrl;
    private String lyricUrl;
    private Boolean like;

    // Parcelable接口实现
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // 将对象的每个字段写入Parcel
        dest.writeString(musicName);
        dest.writeString(author);
        dest.writeString(coverUrl);
        dest.writeString(musicUrl);
        dest.writeString(lyricUrl);
    }

    // 用于从Parcel中创建对象的Creator
    public static final Creator<Music> CREATOR = new Creator<Music>() {
        @Override
        public Music createFromParcel(Parcel source) {
            // 从Parcel中读取数据并创建对象
            return new Music(
                    source.readString(), // 读取musicName
                    source.readString(), // 读取author
                    source.readString(), // 读取coverUrl
                    source.readString(), // 读取musicUrl
                    source.readString() // 读取lyricUrl
            );
        }

        @Override
        public Music[] newArray(int size) {
            return new Music[size];
        }
    };
    public Music(String musicName, String author, String coverUrl, String musicUrl, String lyricUrl) {
        this.musicName = musicName;
        this.author = author;
        this.coverUrl = coverUrl;
        this.musicUrl = musicUrl;
        this.lyricUrl = lyricUrl;
    }

    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getMusicUrl() {
        return musicUrl;
    }

    public void setMusicUrl(String musicUrl) {
        this.musicUrl = musicUrl;
    }

    public String getLyricUrl() {
        return lyricUrl;
    }

    public void setLyricUrl(String lyricUrl) {
        this.lyricUrl = lyricUrl;
    }

    public Boolean getLike() {
        return like;
    }

    public void setLike(Boolean like) {
        this.like = like;
    }
}
