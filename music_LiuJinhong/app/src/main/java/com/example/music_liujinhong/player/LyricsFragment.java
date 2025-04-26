package com.example.music_liujinhong.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music_liujinhong.R;

import java.util.List;

public class LyricsFragment extends Fragment implements MusicService.LyricsSyncListener, LyricFetcher.LyricFetchListener {
    private MusicService musicService;
    private boolean isBound = false;
    private LyricsAdapter adapter;
    private String lyricsUrl;
    private RecyclerView rvLyrics;
    private final static String TAG = "LyricsFragment";

    // 服务绑定
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;
            // 注册监听器
            musicService.setLyricsListener(LyricsFragment.this);
            onLyricsChanged();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            musicService = null;
        }
    };

    public void onLyricsChanged() {
        // 获取歌词 URL
        if (musicService != null && musicService.getCurrentMusic() != null) {
            lyricsUrl = musicService.getCurrentMusic().getLyricUrl();
        }
        // 歌词 URL 获取成功后加载歌词
        if (lyricsUrl != null) {
            loadLyrics(lyricsUrl);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // 绑定服务前先检查 Activity 是否存在
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), MusicService.class);
            getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // 解绑服务并清理资源
        if (isBound && getActivity() != null) {
            if (musicService != null) {
                musicService.clearLyricsListener();
            }
            getActivity().unbindService(connection);
            isBound = false;
            musicService = null;
        }
    }

    // 实现接口回调
    @Override
    public void onLyricsPositionChanged(long currentPosition) {
        Log.d(TAG,"onLyricsPositionChanged");
        if (getActivity() != null) {
            // 切换到主线程更新 UI
            getActivity().runOnUiThread(() -> {
                if (isVisible()) {
                    syncWithPlayback(currentPosition);
                }
            });
        }
    }

    // 歌词同步逻辑：根据当前播放位置计算目标歌词行号，平滑滚动并高亮显示
    public void syncWithPlayback(long currentPosition) {
        int targetPos = findCurrentLyricIndex(currentPosition);
        if (targetPos != -1) {
            smoothScrollToPosition(targetPos);
            Log.d(TAG,targetPos+"");
            adapter.highlightLine(targetPos);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lyrics_viewpager_page, container, false);
        rvLyrics = view.findViewById(R.id.rv_lyrics);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new LyricsAdapter();
        rvLyrics.setAdapter(adapter);
        rvLyrics.setItemAnimator(null);
    }

    private void loadLyrics(String url) {
        LyricFetcher.fetchFromUrl(requireContext(), url, this);
    }

    @Override
    public void onLyricsFetched(List<LyricItem> lyrics) {
        adapter.updateLyrics(lyrics);
    }

    @Override
    public void onError(String error) {
        if (getContext() != null) {
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 根据当前播放时间查找对应的歌词索引。
     * 遍历歌词列表，找到最后一个时间小于等于当前播放时间的歌词项。
     */
    private int findCurrentLyricIndex(long currentPosition) {
        int index = -1;
        int count = adapter.getItemCount();
        for (int i = 0; i < count; i++) {
            LyricItem item = adapter.getLyricAt(i);
            // 假设 getTime() 返回歌词时间对应的毫秒数
            if (item.getTime() <= currentPosition) {
                index = i;
            } else {
                // 因为歌词已按时间排序，后面的值必然更大
                break;
            }
        }
        return index;
    }

    /**
     * 平滑滚动 RecyclerView ，并将目标项居中显示
     */
    private void smoothScrollToPosition(int targetPos) {
        if (rvLyrics != null) {
            RecyclerView.LayoutManager layoutManager = rvLyrics.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
                    @Override
                    public PointF computeScrollVectorForPosition(int targetPosition) {
                        return ((LinearLayoutManager) layoutManager).computeScrollVectorForPosition(targetPosition);
                    }

                    @Override
                    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                        // 计算目标项中心和列表中心之间的偏移量，从而实现居中
                        int viewCenter = viewStart + (viewEnd - viewStart) / 2;
                        int boxCenter = boxStart + (boxEnd - boxStart) / 2;
                        return boxCenter - viewCenter;
                    }
                };
                smoothScroller.setTargetPosition(targetPos);
                layoutManager.startSmoothScroll(smoothScroller);
            } else {
                // 如果不是LinearLayoutManager，使用默认的平滑滚动
                rvLyrics.smoothScrollToPosition(targetPos);
            }
        }
    }

}

