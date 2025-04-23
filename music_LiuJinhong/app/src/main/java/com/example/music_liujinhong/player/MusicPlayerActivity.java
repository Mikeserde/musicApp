package com.example.music_liujinhong.player;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.music_liujinhong.R;
import com.example.music_liujinhong.home.Item;
import com.example.music_liujinhong.home.Music;
import com.example.music_liujinhong.home.SongListBottomSheet;

import java.util.List;

public class MusicPlayerActivity extends AppCompatActivity implements MusicService.IOnComplete, MusicService.OnPlayStateChangeListener {
    private final static String TAG = "MusicPlayerActivity";
    private ViewPager2 viewPager;
    private static MusicService musicService;
    private Item item;
    private boolean isBound = false;
    private int position;
    private GestureDetector gestureDetector;

    private int startWay;//启动MusicPlayerActivity的方式 1-by 点击列表项； 2-by 点击悬浮View

    //////////////////布局部件//////////////////
    private ImageView playBtn,playNext,playPrevious,playModeBtn,likeBtn,listBtn;
    private SeekBar seekBar;
    private TextView currentTimeTextView, totalTimeTextView,songNameTextView,singerNameTextView;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;

            //根据不同启动的情况来做初始化
            if(startWay == 1){
                musicService.setPlayList(item.getMusicList());
                musicService.playAt(item.getMusicList().get(position).getMusicUrl());
            }
            else{
                //检测一下播放状态设置按钮
                if(musicService.isPlaying()){
                    playBtn.setImageResource(R.drawable.ic_pause);
                }else{
                    playBtn.setImageResource(R.drawable.ic_play);
                }
            }
            initMusicPlayerController();
            musicService.setOnCompleteListenerPlayer(MusicPlayerActivity.this);
            musicService.playStateChangeListenerActivity(MusicPlayerActivity.this);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    // 定义更新 SeekBar 的任务
    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (musicService != null && musicService.isPlaying()) {
                int currentPos = musicService.getCurrentProgress();
                int totalDuration = musicService.getTotalDuration();

                // 更新 SeekBar 和当前时间
                seekBar.setProgress(currentPos);
                currentTimeTextView.setText(musicService.formatTime(currentPos));

                // 确保总时长显示正确（防止动态码率音频总时长变化）
                if (seekBar.getMax() != totalDuration) {
                    seekBar.setMax(totalDuration);
                    totalTimeTextView.setText(musicService.formatTime(totalDuration));
                }
            }
            // 每 500ms 更新一次
            new Handler().postDelayed(this, 500);
        }
    };
    private void initMusicPlayerController() {
        updateSongInfo();

        //控制暂停和播放
        playBtn.setOnClickListener(v->{
            if (musicService != null) {
                if (musicService.isPlaying()) {
                    // 暂停播放
                    playBtn.setImageResource(R.drawable.ic_play);
                    musicService.play();
                } else {
                    // 开始播放
                    playBtn.setImageResource(R.drawable.ic_pause);
                    musicService.play();
                }
            }
        });
        //控制播放上一首
        playPrevious.setOnClickListener(v->{
            musicService.playPrevious();
            playBtn.setImageResource(R.drawable.ic_pause);
            updateSongInfo();
        });
        //控制播放下一首
        playNext.setOnClickListener(v->{
            musicService.playNext();
            playBtn.setImageResource(R.drawable.ic_pause);
            updateSongInfo();
        });

        // 初始化时设置总时长
        if (musicService != null) {
            int totalDuration = musicService.getTotalDuration();
            seekBar.setMax(totalDuration);
            totalTimeTextView.setText(musicService.formatTime(totalDuration));
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean isUserDragging = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTimeTextView.setText(musicService.formatTime(progress));
                    playBtn.setImageResource(R.drawable.ic_play);
                    musicService.getMediaPlayer().pause();
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
                playBtn.setImageResource(R.drawable.ic_pause);
            }
        });

        musicService.setOnPlaybackUpdateListener(new MusicService.OnPlaybackUpdateListener() {
            @Override
            public void onDurationChanged(int totalDuration) {
                runOnUiThread(() -> {
                    seekBar.setMax(totalDuration);
                    totalTimeTextView.setText(musicService.formatTime(totalDuration));
                });
            }

            @Override
            public void onPositionChanged(int currentPosition) {
                // 如果不需要实时回调可留空
            }
         });

        // 定时更新SeekBar的进度
        new Handler().postDelayed(updateSeekBar, 100);


        playModeBtn.setOnClickListener(v->{
            MusicService.PlaybackMode currentMode = musicService.getPlaybackMode();
            MusicService.PlaybackMode newMode;
            switch (currentMode) {
                case LIST_LOOP:
                    newMode = MusicService.PlaybackMode.RANDOM;
                    playModeBtn.setImageResource(R.drawable.ic_shuffle);
                    break;
                case RANDOM:
                    newMode = MusicService.PlaybackMode.SINGLE_LOOP;
                    playModeBtn.setImageResource(R.drawable.ic_cycle);
                    break;
                case SINGLE_LOOP:
                default:
                    newMode = MusicService.PlaybackMode.LIST_LOOP;
                    playModeBtn.setImageResource(R.drawable.ic_order);
                    break;
            }
            musicService.setPlaybackMode(newMode);
        });

        listBtn.setOnClickListener(v -> {
            // 假设从 MusicService 获取播放列表
            List<Music> songList = musicService.getMusicList();
            // 创建 BottomSheetDialogFragment
            SongListBottomSheet bottomSheet = new SongListBottomSheet(songList);
            bottomSheet.show(getSupportFragmentManager(), "SongListBottomSheet");
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_music_player);
        Intent intent = getIntent();
        item = intent.getParcelableExtra("music_List", Item.class);
        position = intent.getIntExtra("position",0);
        startWay = intent.getIntExtra("startWay",1);
        // 初始化 ViewPager2
        viewPager = findViewById(R.id.view_pager);
        MusicPagerAdapter adapter = new MusicPagerAdapter(this);
        viewPager.setAdapter(adapter);
        // 设置垂直滑动
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        // 返回按钮点击事件
        findViewById(R.id.back).setOnClickListener(v -> closePlayer());
        //绑定布局部件
        bindLayout();
        //初始化手势
        initGestureDetector();

        // 绑定前台服务
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void bindLayout() {
        //绑定布局部件
        playBtn = findViewById(R.id.btnPlay);
        playNext = findViewById(R.id.btnNext);
        playPrevious = findViewById(R.id.btnPrevious);
        seekBar = findViewById(R.id.seekBar);
        currentTimeTextView = findViewById(R.id.currentTime);
        totalTimeTextView = findViewById(R.id.totalTime);
        songNameTextView = findViewById(R.id.song_name);
        singerNameTextView = findViewById(R.id.singer);
        rootView = findViewById(R.id.root_view);
        playModeBtn = findViewById(R.id.btnMode);
        likeBtn = findViewById(R.id.iv_like);
        listBtn = findViewById(R.id.btn_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().post(updateSeekBar);
    }

    private void closePlayer() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("is_playing", musicService != null && musicService.isPlaying());
        setResult(RESULT_OK, resultIntent);
        finish();
        overridePendingTransition(0, R.anim.slide_out_down);
    }

    // 更新歌曲信息UI
    public void updateSongInfo() {
        if (musicService == null) return;

        Music currentMusic = musicService.getCurrentMusic();
        if (currentMusic != null) {
            songNameTextView.setText(currentMusic.getMusicName());    // 歌曲名
            singerNameTextView.setText(currentMusic.getAuthor()); // 歌手名
        }
    }

    //背景色渐变动画
    private ValueAnimator colorAnimator;
    private View rootView; // 根布局（如 ConstraintLayout）

    public void animateBackgroundColor(int targetColor) {
        if (rootView == null) return;

        // 取消之前的动画
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }

        int currentColor = ((ColorDrawable) rootView.getBackground()).getColor();

        colorAnimator = ValueAnimator.ofArgb(currentColor, targetColor);
        colorAnimator.setDuration(800);
        colorAnimator.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            rootView.setBackgroundColor(color);
        });
        colorAnimator.start();
    }

    public MusicService getMusicService() {
        return musicService;
    }

    void initGestureDetector(){
        // 初始化手势检测器，判断向下滑动
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            // 重写 onFling 判断向下滑动
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // e1：手指按下时的事件，e2：手指抬起时的事件
                // 设定一个阈值，例如 Y 方向的位移大于100，并且速度足够快
                if (e2.getY() - e1.getY() > 100 && Math.abs(velocityY) > 1000) {
                    finish();
                    // 可添加自定义动画：页面从上往下退出
                    overridePendingTransition(0, R.anim.slide_down);
                    return true;
                }
                return false;
            }
        });
    }

    // 将触摸事件传递给 GestureDetector
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }

    @Override
    public void onMusicPlay() {
        playBtn.setImageResource(R.drawable.ic_pause);
    }

    @Override
    public void onMusicPause() {
        playBtn.setImageResource(R.drawable.ic_play);
    }
}
