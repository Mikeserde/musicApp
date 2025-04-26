package com.example.music_liujinhong.home;

import android.os.Parcel;
import android.os.Parcelable;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.util.ArrayList;
import java.util.List;

public class Item implements MultiItemEntity, Parcelable {
    private List<Music> musicList;
    private String type;
    private String title;

    // Parcelable接口实现
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // 将对象的每个字段写入Parcel
        dest.writeTypedList(musicList); // 写入musicList
        dest.writeString(type); // 写入type
        dest.writeString(title); // 写入title
    }

    // 用于从Parcel中创建对象的Creator
    public static final Creator<Item> CREATOR = new Creator<Item>() {
        @Override
        public Item createFromParcel(Parcel source) {
            // 从Parcel中读取数据并创建对象
            ArrayList<Music> musicList = new ArrayList<>();
            source.readTypedList(musicList, Music.CREATOR); // 读取musicList
            String type = source.readString(); // 读取type
            return new Item(type,musicList);
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };

    public String getTitle() {
        return title;
    }
    public Item(String type,List<Music> musicList) {
        this.musicList = musicList;
        this.type = type;
        switch (type) {
            case "1":
                title = null;
                break;
            case "2":
                title = "专属好歌";
                break;
            case "3":
                title = "每日推荐";
                break;
            case "4":
                title = "热门金曲";
                break;
        }
    }

    @Override
    public int getItemType() {
        if(type.equals("1")){
            return MultiTypeAdapter.ItemType.BANNER;
        } else if (type.equals("2")) {
            return MultiTypeAdapter.ItemType.CARD;
        } else if (type.equals("3")) {
            return MultiTypeAdapter.ItemType.SINGLE;
        } else if (type.equals("4")) {
            return MultiTypeAdapter.ItemType.DOUBLE;
        }
        return MultiTypeAdapter.ItemType.SINGLE;
    }

    // Getter/Setter
    public List<Music> getMusicList() { return musicList; }
    public void setMusicList(List<Music> musicList) { this.musicList = musicList; }
}
