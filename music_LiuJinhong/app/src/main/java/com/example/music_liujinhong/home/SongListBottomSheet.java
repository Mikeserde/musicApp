package com.example.music_liujinhong.home;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music_liujinhong.R;
import com.example.music_liujinhong.player.CoverFragment;
import com.example.music_liujinhong.player.MusicService;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.shape.MaterialShapeDrawable;

import java.util.ArrayList;
import java.util.List;

public class SongListBottomSheet extends BottomSheetDialogFragment{

    private RecyclerView rvSongList;
    private TextView tvCurrentListCount;
    private LinearLayout llPlayMode;
    private ImageView ivPlayMode;
    private TextView tvPlayMode;
    private MusicService musicService;
    private List<Music> songList = new ArrayList();
    private MusicService.PlaybackMode currentPlayMode = MusicService.PlaybackMode.LIST_LOOP;
    private int currentSongIndex = -1;
    private boolean isBound = false;
    // 服务连接回调
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;
            updateSongList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private void updateSongList(){
        if(musicService != null){
            songList = musicService.getMusicList();
            currentPlayMode = musicService.getPlaybackMode();
            currentSongIndex = musicService.getCurrentPosition();
            if(adapter != null){
                adapter.setSongList(songList);
                adapter.setCurrentPlayingIndex(currentSongIndex);
                adapter.notifyDataSetChanged();
                // 设置当前列表歌曲数量
                tvCurrentListCount.setText(String.valueOf(songList.size()));
                updatePlayModeBtn();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // 绑定服务
        Intent intent = new Intent(getActivity(), MusicService.class);
        requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        // 解绑服务
        if (isBound) {
            requireActivity().unbindService(connection);
            isBound = false;
        }
    }

    private SongListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 加载底部弹窗布局
        View view = inflater.inflate(R.layout.layout_bottom_sheet_song_list, container, false);

        rvSongList = view.findViewById(R.id.rv_song_list);
        tvCurrentListCount = view.findViewById(R.id.tv_sheet_title_number);
        llPlayMode = view.findViewById(R.id.ll_play_mode);
        ivPlayMode = view.findViewById(R.id.iv_play_mode);
        tvPlayMode = view.findViewById(R.id.tv_play_mode);

        // 设置当前列表歌曲数量
        tvCurrentListCount.setText(String.valueOf(songList.size()));

        // 初始化 RecyclerView
        adapter = new SongListAdapter(songList, new SongListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                //在此进行歌曲切换播放
                musicService.playAt(position);
                currentSongIndex = position;
                // 更新正在播放歌曲高亮显示
                adapter.setCurrentPlayingIndex(currentSongIndex);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onRemoveClick(int position) {
                // 删除歌曲
                musicService.removeSongAt(position);
                // 更新列表
                songList = musicService.getMusicList();
                adapter.notifyDataSetChanged();
                currentSongIndex = musicService.getCurrentPosition();
                adapter.setCurrentPlayingIndex(currentSongIndex);
                // 更新当前列表歌曲数量显示
                tvCurrentListCount.setText(String.valueOf(songList.size()));
            }
        });

        rvSongList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSongList.setAdapter(adapter);

        // 监听播放模式
        llPlayMode.setOnClickListener(v -> {
            // 更新UI
            MusicService.PlaybackMode oldMode = musicService.getPlaybackMode();
            MusicService.PlaybackMode newMode;
            switch (oldMode) {
                case LIST_LOOP:
                    newMode = MusicService.PlaybackMode.RANDOM;
                    ivPlayMode.setImageResource(R.drawable.ic_shuffle_black);
                    tvPlayMode.setText("随机播放");
                    break;
                case RANDOM:
                    newMode = MusicService.PlaybackMode.SINGLE_LOOP;
                    ivPlayMode.setImageResource(R.drawable.ic_circle_black);
                    tvPlayMode.setText("单曲循环");
                    break;
                case SINGLE_LOOP:
                default:
                    newMode = MusicService.PlaybackMode.LIST_LOOP;
                    ivPlayMode.setImageResource(R.drawable.ic_order_black);
                    tvPlayMode.setText("列表循环");
                    break;
            }
            musicService.setPlaybackMode(newMode);
        });

        return view;
    }

    private void updatePlayModeBtn() {
        //初始化播放模式按钮
        switch (currentPlayMode) {
            case LIST_LOOP:
                ivPlayMode.setImageResource(R.drawable.ic_order_black);
                tvPlayMode.setText("列表循环");
                break;
            case RANDOM:
                ivPlayMode.setImageResource(R.drawable.ic_shuffle_black);
                tvPlayMode.setText("随机播放");
                break;
            case SINGLE_LOOP:
                ivPlayMode.setImageResource(R.drawable.ic_circle_black);
                tvPlayMode.setText("单曲循环");
                break;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (bottomSheet == null) return;

            // 修复1：确保在设置背景前移除可能的padding
            bottomSheet.setPadding(0, 0, 0, 0);

            // 修复2：使用正确的圆角设置方法
            float radius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());

            MaterialShapeDrawable shape = new MaterialShapeDrawable();
            shape.setShapeAppearanceModel(
                    shape.getShapeAppearanceModel()
                            .toBuilder()
                            .setTopLeftCornerSize(radius)
                            .setTopRightCornerSize(radius)
                            .setBottomLeftCornerSize(0)
                            .setBottomRightCornerSize(0)
                            .build()
            );
            shape.setFillColor(ColorStateList.valueOf(Color.WHITE));

            // 修复3：确保背景设置生效
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                bottomSheet.setBackground(shape);
            } else {
                bottomSheet.setBackgroundDrawable(shape);
            }

            // 修复4：确保clipToOutline启用
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                bottomSheet.setClipToOutline(true);
            }
            // —— 2. 强制让内容区能撑满屏幕（以便可以拖到隐藏） ——
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.requestLayout();

            // —— 3. 拿到 Behavior，设置 peekHeight、初始状态、以及拖到底时关闭 ——
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

            int peekPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 582, getResources().getDisplayMetrics());
            behavior.setPeekHeight(peekPx);
            behavior.setHideable(true);
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dismiss();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    if (slideOffset > 0) {
                        // user is dragging upward past our 582dp → 直接回到 COLLAPSED
                        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                }
            });
        });

        return dialog;
    }
}
