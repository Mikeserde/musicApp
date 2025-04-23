package com.example.music_liujinhong.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.music_liujinhong.player.MusicPlayerActivity;
import com.example.music_liujinhong.R;
import com.example.music_liujinhong.player.MusicService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements MultiTypeAdapter.OnItemClickListener, MusicService.IOnComplete {
    private Handler mHandler = new Handler();
    private static final String TAG = "okHttp";
    private static final ArrayList<Item> items = new ArrayList<>();
    private MultiTypeAdapter adapter;
    private RecyclerView recyclerView;

    // 悬浮栏
    private LinearLayout miniPlayer;
    private ImageView ivPlay,ivCover,ivList;
    private TextView tvTitle, tvArtist;
    private MusicService musicService;
    private SeekBar seekBar;
    private boolean isBound;

    // 标记是否已进行初次随机选歌并设置播放列表
    private boolean initialSelectionDone = false;

    private final ArrayList<Call> ongoingCalls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recycler_view);
        setData();

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            mHandler.postDelayed(() -> {
                // 清空数据重置，达到刷新效果
                items.clear();
                setData();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MainActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
            }, 1000);
        });

        bindLayout();

        // 启动并绑定音乐服务
        Intent serviceIntent = new Intent(this, MusicService.class);
        if(musicService == null){
            startService(serviceIntent);  // 启动前台服务
        }
        // 注意这里返回值只是一个同步标识，绑定结果以 onServiceConnected 为准
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "bindService invoked");
        setupMiniPlayer();
    }

    private void bindLayout() {
        miniPlayer = findViewById(R.id.mini_player);
        ivPlay = findViewById(R.id.iv_play);
        tvTitle = findViewById(R.id.tv_title);
        tvArtist = findViewById(R.id.tv_artist);
        ivCover = findViewById(R.id.iv_cover);
        ivList = findViewById(R.id.menu_button);
        seekBar = findViewById(R.id.mini_seekBar);

        ivList.setOnClickListener(v -> {
            // 假设从 MusicService 获取播放列表
            List<Music> songList = musicService.getMusicList();
            // 创建 BottomSheetDialogFragment
            SongListBottomSheet bottomSheet = new SongListBottomSheet(songList);
            bottomSheet.show(getSupportFragmentManager(), "SongListBottomSheet");
        });

        miniPlayer.setOnClickListener(v -> {
            if (musicService != null && musicService.getCurrentMusic() != null) {
                Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
                startActivity(intent);
                // 使用从底部滑入的动画
                overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
            }
        });
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service is bound!");
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;
            updateMiniPlayer();
            initSeekbar();
            musicService.setOnCompleteListenerMain(MainActivity.this);

            // 设置播放状态回调
            musicService.setOnPlayStateChangeListenerCover(new MusicService.OnPlayStateChangeListener() {
                @Override
                public void onMusicPlay() {
                    // 当开始播放时，更新 UI
                    runOnUiThread(() -> updateMiniPlayer());
                }

                @Override
                public void onMusicPause() {
                    // 当暂停时，更新 UI
                    runOnUiThread(() -> updateMiniPlayer());
                }
            });

            // 如果数据已加载且还没进行初次随机播放，就进行
            if (!initialSelectionDone && !items.isEmpty()) {
                performInitialSelection();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private void setupMiniPlayer() {
        // 播放/暂停按钮
        ivPlay.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.play();
                updatePlayButton();
            }
        });

        // 点击悬浮栏打开播放页
        miniPlayer.setOnClickListener(v -> {
            if (musicService != null && musicService.getCurrentMusic() != null) {
                // 通过 Intent 传递数据，启动音乐播放页
                Intent intent = new Intent(this, MusicPlayerActivity.class);
                intent.putExtra("startWay",2);//2代表点击悬浮View启动
                startActivityForResult(intent, 1);
                // 使用从底部滑入的动画
                overridePendingTransition(R.anim.slide_up,0);
            }
        });
    }

    // 更新悬浮控制栏状态
    private void updateMiniPlayer() {
        // 始终显示 miniPlayer
        miniPlayer.setVisibility(View.VISIBLE);
        Log.d(TAG,"更新悬浮View");
        if (musicService != null && musicService.getCurrentMusic() != null) {
            Music current = musicService.getCurrentMusic();
            tvTitle.setText(current.getMusicName());
            tvArtist.setText(current.getAuthor());
            RequestOptions options = new RequestOptions().circleCrop(); // 让图片变圆
            Glide.with(this)
                 .load(current.getCoverUrl())
                    .apply(options)
                        .into(ivCover);
        } else {
            // 如果没有当前歌曲，可以设置一个默认状态
            tvTitle.setText("暂无播放歌曲");
            tvArtist.setText("");
            ivCover.setImageResource(R.mipmap.ic_launcher);
        }
        updatePlayButton();
    }


    private void updatePlayButton() {
        ivPlay.setImageResource(musicService != null && musicService.isPlaying()
                ? R.drawable.ic_black_pause : R.drawable.ic_black_play);
    }

    private void showMiniPlayer() {
        if (miniPlayer.getVisibility() != View.VISIBLE) {
            miniPlayer.setVisibility(View.VISIBLE);
            miniPlayer.animate().translationY(0).setDuration(300).start();
        }
    }

    private void hideMiniPlayer() {
        if (miniPlayer.getVisibility() == View.VISIBLE) {
            miniPlayer.animate().translationY(miniPlayer.getHeight()).setDuration(300)
                    .withEndAction(() -> miniPlayer.setVisibility(View.GONE)).start();
        }
    }

    private void setData() {
        // 每次请求随机页号
        int page = new Random().nextInt(2) + 1;
        requestEnqueue(page);
    }

    private void requestEnqueue(int page) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        String url = String.format("https://hotfix-service-prod.g.mi.com/music/homePage?current=%d&size=5", page);
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();

        Call call = client.newCall(request);
        ongoingCalls.add(call); // 保存 Call

        try {
            call.enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Log.d(TAG, "数据请求成功");
                    ongoingCalls.remove(call); // 请求完成后移除 Call
                    String jsonString = response.body().string();
                    // 解析 JSON 字符串为 JsonObject
                    JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

                    // 获取 "data" 对象
                    JsonObject dataObject = jsonObject.getAsJsonObject("data");
                    // 获取 "records" 数组
                    JsonArray recordsArray = dataObject.getAsJsonArray("records");

                    // 遍历 records 数组，构建 Item 列表
                    for (JsonElement recordElement : recordsArray) {
                        String style = recordElement.getAsJsonObject().get("style").getAsString();
                        List<Music> musicList = new ArrayList<>();
                        // 获取 "musicInfoList" 数组
                        JsonArray musicInfoList = recordElement.getAsJsonObject().getAsJsonArray("musicInfoList");
                        for (JsonElement musicInfoElement : musicInfoList) {
                            JsonObject musicInfo = musicInfoElement.getAsJsonObject();
                            String musicName = musicInfo.get("musicName").getAsString();
                            String author = musicInfo.get("author").getAsString();
                            String coverUrl = musicInfo.get("coverUrl").getAsString().replace("http://", "https://");
                            String musicUrl = musicInfo.get("musicUrl").getAsString().replace("http://", "https://");
                            String lyricUrl = musicInfo.get("lyricUrl").getAsString().replace("http://", "https://");
                            Music music = new Music(musicName, author, coverUrl, musicUrl, lyricUrl);
                            musicList.add(music);
                        }
                        if (style.equals("3")) {
                            musicList = musicList.subList(0, 1);
                        } else if (style.equals("4")) {
                            musicList = musicList.subList(0, 2);
                        }
                        Item item = new Item(style, musicList);
                        items.add(item);
                    }

                    mHandler.post(() -> {
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        } else {
                            initRecyclerView();
                        }
                        // 如果服务已绑定且初始随机播放未执行，则执行初次随机播放
                        if (!initialSelectionDone && musicService != null && !items.isEmpty()) {
                            performInitialSelection();
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.d(TAG, "数据请求失败");
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void performInitialSelection() {
        // 随机选择某个 Item 模块，并设置该模块的所有音乐到服务播放列表中，播放第一首
        int randomIndex = new Random().nextInt(items.size());
        Item selectedItem = items.get(randomIndex);
        List<Music> moduleMusicList = selectedItem.getMusicList();
        if (moduleMusicList != null && !moduleMusicList.isEmpty()) {
            musicService.setPlayList(moduleMusicList);
            musicService.playAt(selectedItem.getMusicList().get(0).getMusicUrl());
            initialSelectionDone = true;
            updateMiniPlayer();
            Log.d(TAG, "首次随机选歌完成，选中的模块索引：" + randomIndex);
        }
    }

    private void initRecyclerView() {
        adapter = new MultiTypeAdapter(items);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        adapter.getLoadMoreModule().setOnLoadMoreListener(() -> {
            mHandler.postDelayed(() -> {
                setData();
                adapter.getLoadMoreModule().loadMoreComplete();
            }, 1000);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        // 取消所有未完成的请求
        for (Call call : ongoingCalls) {
            call.cancel();
        }
        ongoingCalls.clear();
        if(isBound){
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    public void proceedToPlayer(Item item, int position) {
        // 通过 Intent 传递数据，启动音乐播放页
        Intent intent = new Intent(this, MusicPlayerActivity.class);
        intent.putExtra("music_List", item);
        intent.putExtra("position", position);
        intent.putExtra("startWay",1);//1代表点击列表启动
        startActivityForResult(intent, 1);
    }

    @Override
    public void onItemClick(Item item, int position) {
        proceedToPlayer(item, position);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // 返回播放页后更新悬浮控制栏
            updateMiniPlayer();
        }
    }

    private void initSeekbar(){
        // 初始化时设置总时长
        if (musicService != null) {
            int totalDuration = musicService.getTotalDuration();
            seekBar.setMax(totalDuration);
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean isUserDragging = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    musicService.getMediaPlayer().pause();
                    ivPlay.setImageResource(R.drawable.ic_black_play);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserDragging = false;
                musicService.seekTo(seekBar.getProgress());
                // 强制同步一次进度
                new Handler().postDelayed(updateSeekBar, 100);
                ivPlay.setImageResource(R.drawable.ic_black_pause);
            }
        });

        musicService.setOnPlaybackUpdateListener(new MusicService.OnPlaybackUpdateListener() {
            @Override
            public void onDurationChanged(int totalDuration) {
                runOnUiThread(() -> {
                    seekBar.setMax(totalDuration);
                });
            }

            @Override
            public void onPositionChanged(int currentPosition) {
                // 如果不需要实时回调可留空
            }
        });

        // 定时更新SeekBar的进度
        new Handler().postDelayed(updateSeekBar, 100);
    }

    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (musicService != null && musicService.isPlaying()) {
                int currentPos = musicService.getCurrentProgress();
                int totalDuration = musicService.getTotalDuration();

                // 更新 SeekBar
                seekBar.setProgress(currentPos);

                // 确保总时长显示正确（防止动态码率音频总时长变化）
                if (seekBar.getMax() != totalDuration) {
                    seekBar.setMax(totalDuration);
                }
            }
            // 每 500ms 更新一次
            new Handler().postDelayed(this, 500);
        }
    };

    public void updateSongInfo(){
        Log.d(TAG,"MainActivity的updateSongInfo被调用");
        updateMiniPlayer();
    }
}
