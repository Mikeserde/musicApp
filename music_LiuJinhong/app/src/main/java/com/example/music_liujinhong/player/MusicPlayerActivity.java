package com.example.music_liujinhong.player;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.animation.ValueAnimator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.OvershootInterpolator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.music_liujinhong.R;
import com.example.music_liujinhong.home.Item;
import com.example.music_liujinhong.home.Music;
import com.example.music_liujinhong.home.SongListBottomSheet;

import java.util.List;

import android.graphics.drawable.Drawable;
import androidx.appcompat.content.res.AppCompatResources;
import android.graphics.drawable.InsetDrawable;
 import android.view.ViewGroup;

public class MusicPlayerActivity extends AppCompatActivity implements MusicService.IOnComplete, MusicService.OnPlayStateChangeListener, MusicService.OnPlayModeChangedListener, MusicService.OnPlayListEmptyListener {
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
            musicService.setOnPlayStateChangeListenerActivity(MusicPlayerActivity.this);
            musicService.setOnPlayModeChangedListener(MusicPlayerActivity.this);
            musicService.setOnPlayListEmptyListenerPlayerActivity(MusicPlayerActivity.this);
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

        // 点赞/喜欢按钮
        likeBtn.setOnClickListener(v -> {
            if (musicService == null) return;
            Music current = musicService.getCurrentMusic();
            if (current == null) return;
            boolean liked = Boolean.TRUE.equals(current.getLike());
            // 切换前的可视缩放（空心或红心）
            float prevScaleX = likeBtn.getScaleX();
            float prevScaleY = likeBtn.getScaleY();
            // 红心与空心的尺寸比（将红心缩小到与空心一致）
            float factor = computeHeartScaleForLiked();
            boolean nowLiked = !liked;
            current.setLike(nowLiked);
            // 立即切换图标，但不重置缩放，防止覆盖动画收尾
            updateLikeButton(nowLiked);
            // 计算动画目标的收尾缩放：
            // 切到红心：end = prev * factor（红心缩小到空心之前的尺寸）
            // 切到空心：end = prev / factor（恢复到空心的尺寸）
            float endScaleX = nowLiked ? prevScaleX * factor : prevScaleX / factor;
            float endScaleY = nowLiked ? prevScaleY * factor : prevScaleY / factor;
            playLikeToggleAnimation(likeBtn, nowLiked, endScaleX, endScaleY, prevScaleX, prevScaleY);
        });

        // 初始化时进度条
        if (musicService != null) {
            int totalDuration = musicService.getTotalDuration();
            seekBar.setMax(totalDuration);
            totalTimeTextView.setText(musicService.formatTime(totalDuration));

            int currentPos = musicService.getCurrentProgress();
            // 更新 SeekBar 和当前时间
            seekBar.setProgress(currentPos);
            currentTimeTextView.setText(musicService.formatTime(currentPos));
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean isUserDragging = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTimeTextView.setText(musicService.formatTime(progress));
                    playBtn.setImageResource(R.drawable.ic_play);
                    if (musicService.getMediaPlayer() != null) {
                        musicService.getMediaPlayer().pause();
                    }
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

        //初始化播放模式按钮
        updatePlayModeBtnOfController();

        playModeBtn.setOnClickListener(v->{
            MusicService.PlaybackMode oldMode = musicService.getPlaybackMode();
            MusicService.PlaybackMode newMode;
            switch (oldMode) {
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
            SongListBottomSheet bottomSheet = new SongListBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "SongListBottomSheet");
        });
    }

    private void updatePlayModeBtnOfController() {
        MusicService.PlaybackMode currentMode = musicService.getPlaybackMode();
        switch (currentMode) {
            case LIST_LOOP:
                playModeBtn.setImageResource(R.drawable.ic_order);
                break;
            case RANDOM:
                playModeBtn.setImageResource(R.drawable.ic_shuffle);
                break;
            case SINGLE_LOOP:
                playModeBtn.setImageResource(R.drawable.ic_cycle);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 核心代码：设置透明状态栏和导航栏（API 21+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT); // 状态栏透明
            window.setNavigationBarColor(Color.TRANSPARENT); // 导航栏透明（可选）

            // 设置状态栏文字颜色为深色（浅色背景时使用）
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR // 可选：深色文字
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 兼容 API 19-20 的透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

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
        // 保持固定容器尺寸，居中显示，避免因资源尺寸不同引起位置偏移
        likeBtn.setAdjustViewBounds(false);
        likeBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        likeBtn.setPadding(0,0,0,0);
        normalizeLikeButtonContainerSize();
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
            // 同步喜欢按钮状态
            updateLikeButton(Boolean.TRUE.equals(currentMusic.getLike()));
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
        if(musicService!=null) {
            musicService.removeOnCompleteListenerPlayer();
            musicService.removeOnPlayStateChangeListenerActivity();
            musicService.removeOnPlayModeChangedListener();
            musicService.removeOnPlayListEmptyListenerPlayerActivity();
        }
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

    @Override
    public void onPlayModeChanged() {
        updatePlayModeBtnOfController();
    }

    @Override
    public void onPlayListEmpty() {
        closePlayer();
    }

    // 喜欢按钮状态刷新
    private void updateLikeButton(boolean liked) {
        if (likeBtn == null) return;
        if (liked) {
            Drawable padded = getPaddedFilledHeart();
            if (padded != null) {
                likeBtn.setImageDrawable(padded);
            } else {
                likeBtn.setImageResource(R.drawable.like);
            }
        } else {
            likeBtn.setImageResource(R.drawable.collect);
        }
        // 静态刷新时同步缩放：未喜欢统一为1，已喜欢按资源因子
        float base = liked ? computeHeartScaleForLiked() : 1f;
        likeBtn.setScaleX(base);
        likeBtn.setScaleY(base);
    }

    // 喜欢动画：富有弹性的填充/取消切换（以切换前尺寸为基准）
    private void playLikeToggleAnimation(ImageView target, boolean nowLiked, float endScaleX, float endScaleY, float prevScaleX, float prevScaleY) {
        if (target == null) return;
        // 强制以中心为缩放枢轴，避免视觉位置偏移
        target.setPivotX(target.getWidth() / 2f);
        target.setPivotY(target.getHeight() / 2f);
        AnimatorSet set = new AnimatorSet();
        if (nowLiked) {
            // 切到红心：以目标收尾尺寸为基准，先小到0.2×end，再弹到1.5×end，最后收尾到end
            target.setScaleX(0.2f * endScaleX);
            target.setScaleY(0.2f * endScaleY);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(target, "scaleX", 0.2f * endScaleX, 1.5f * endScaleX, endScaleX);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(target, "scaleY", 0.2f * endScaleY, 1.5f * endScaleY, endScaleY);
            set.setDuration(380);
            set.setInterpolator(new OvershootInterpolator());
            set.playTogether(scaleX, scaleY);
        } else {
            // 切到空心：从当前prev到0.85×end，再回到end，保持轻弹
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(target, "scaleX", prevScaleX, 0.85f * endScaleX, endScaleX);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(target, "scaleY", prevScaleY, 0.85f * endScaleY, endScaleY);
            set.setDuration(260);
            set.setInterpolator(new OvershootInterpolator());
            set.playTogether(scaleX, scaleY);
        }
        set.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // 强制收尾到目标缩放，确保最终尺寸与切换前空心一致
                target.setScaleX(endScaleX);
                target.setScaleY(endScaleY);
            }
        });
        set.start();
    }
    private float computeHeartScaleForLiked() {
        Drawable filled = AppCompatResources.getDrawable(this, R.drawable.like);
        Drawable empty = AppCompatResources.getDrawable(this, R.drawable.collect);
        if (filled == null || empty == null) return 1f;
        int fw = filled.getIntrinsicWidth();
        int fh = filled.getIntrinsicHeight();
        int ew = empty.getIntrinsicWidth();
        int eh = empty.getIntrinsicHeight();
        if (fw <= 0 || fh <= 0 || ew <= 0 || eh <= 0) return 1f;
        float scaleX = ew / (float) fw;
        float scaleY = eh / (float) fh;
        return Math.min(scaleX, scaleY);
    }
    private void applyHeartSizeAdjust(boolean liked) {
        if (likeBtn == null) return;
        float base = liked ? computeHeartScaleForLiked() : 1f;
        likeBtn.setScaleX(base);
        likeBtn.setScaleY(base);
    }
    private Drawable getPaddedFilledHeart() {
        Drawable filled = AppCompatResources.getDrawable(this, R.drawable.like);
        Drawable empty = AppCompatResources.getDrawable(this, R.drawable.collect);
        if (filled == null || empty == null) return filled;
        int fw = filled.getIntrinsicWidth();
        int fh = filled.getIntrinsicHeight();
        int ew = empty.getIntrinsicWidth();
        int eh = empty.getIntrinsicHeight();
        if (fw <= 0 || fh <= 0 || ew <= 0 || eh <= 0) return filled;
        int dx = Math.max(0, ew - fw);
        int dy = Math.max(0, eh - fh);
        int left = dx / 2;
        int right = dx - left;
        int top = dy / 2;
        int bottom = dy - top;
        return new InsetDrawable(filled, left, top, right, bottom);
    }
    private void normalizeLikeButtonContainerSize() {
        if (likeBtn == null) return;
        Drawable empty = AppCompatResources.getDrawable(this, R.drawable.collect);
        Drawable filled = AppCompatResources.getDrawable(this, R.drawable.like);
        if (empty == null || filled == null) return;
        int targetW = Math.max(empty.getIntrinsicWidth(), filled.getIntrinsicWidth());
        int targetH = Math.max(empty.getIntrinsicHeight(), filled.getIntrinsicHeight());
        ViewGroup.LayoutParams lp = likeBtn.getLayoutParams();
        if (lp != null) {
            lp.width = targetW;
            lp.height = targetH;
            likeBtn.setLayoutParams(lp);
        }
    }
}
