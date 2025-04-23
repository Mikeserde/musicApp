package com.example.music_liujinhong.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music_liujinhong.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class SongListBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvSongList;
    private TextView tvCurrentPlayingInfo;
    private RadioGroup rgPlayMode;

    private List<Music> songList; // 外部传入的歌曲列表
    private SongListAdapter adapter;

    // 可在构造函数或 setter 中传入歌曲列表
    public SongListBottomSheet(List<Music> songList) {
        this.songList = songList;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 加载底部弹窗布局
        View view = inflater.inflate(R.layout.layout_bottom_sheet_song_list, container, false);

        rvSongList = view.findViewById(R.id.rv_song_list);
        tvCurrentPlayingInfo = view.findViewById(R.id.tv_sheet_title);
        rgPlayMode = view.findViewById(R.id.rg_play_mode);

        tvCurrentPlayingInfo.setText("当前播放" + songList.size());

        // 初始化 RecyclerView
        adapter = new SongListAdapter(songList, new SongListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // TODO: 在此进行歌曲切换播放，例如:
                // musicService.playAt(position);
            }

            @Override
            public void onRemoveClick(int position) {
                // TODO: 删除歌曲示例
                // songList.remove(position);
                // adapter.notifyItemRemoved(position);
            }
        });

        rvSongList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSongList.setAdapter(adapter);

        // 其它控件监听，如播放模式
        rgPlayMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_sequence) {
                // 顺序播放逻辑
            } else if (checkedId == R.id.rb_random) {
                // 随机播放逻辑
            }
        });

        return view;
    }
}
