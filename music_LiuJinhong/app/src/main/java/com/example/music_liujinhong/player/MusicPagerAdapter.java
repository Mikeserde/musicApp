package com.example.music_liujinhong.player;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MusicPagerAdapter extends FragmentStateAdapter {
    public MusicPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new CoverFragment();
            case 1:
                LyricsFragment lyricsFragment = new LyricsFragment();
                // 传递歌词数据（示例数据）
                Bundle args = new Bundle();
                args.putString("lyrics", "这里是歌词内容...");
                lyricsFragment.setArguments(args);
                return lyricsFragment;
            default:
                throw new IllegalArgumentException("Invalid position");
        }
    }

    @Override
    public int getItemCount() {
        return 2; // 封面页 + 歌词页
    }
}