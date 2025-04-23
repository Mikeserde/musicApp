package com.example.music_liujinhong.player;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.music_liujinhong.R;
import com.example.music_liujinhong.home.Music;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class MusicService extends Service {
    private final static String TAG = "MusicService";
    private MediaPlayer mediaPlayer;
    private List<Music> playList = new ArrayList<>();
    private int currentPosition = 0; // 当前播放索引

    private final IBinder binder = new LocalBinder();

    // 前台服务相关
    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "music_channel";

    // 音频焦点管理
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusListener;
    //播放模式
    public enum PlaybackMode {
        LIST_LOOP,    // 列表循环（默认）
        RANDOM,       // 随机播放
        SINGLE_LOOP   // 单曲循环
    }

    private PlaybackMode playbackMode = PlaybackMode.LIST_LOOP;

    public void setPlaybackMode(PlaybackMode mode) {
        this.playbackMode = mode;
    }

    public PlaybackMode getPlaybackMode() {
        return playbackMode;
    }


    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initMediaPlayer();
        createMinimalNotificationChannel();
        initAudioFocus();
    }

    // 创建极简通知渠道
    private void createMinimalNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background Playback",
                    NotificationManager.IMPORTANCE_MIN // 最低优先级（无声音、不弹出）
            );
            channel.setShowBadge(false); // 不在应用图标显示角标
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET); // 隐藏锁屏内容
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // 构建极简通知
    private Notification buildMinimalNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("音乐播放中")
                .setSmallIcon(R.mipmap.ic_launcher) // 必须设置有效图标
                .setPriority(NotificationCompat.PRIORITY_MIN) // 最低优先级
                .setVisibility(NotificationCompat.VISIBILITY_SECRET) // 隐藏敏感内容
                .build();
    }

    // 初始化音频焦点
    private void initAudioFocus() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioFocusListener = focusChange -> {
            play();
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "ACTION_TOGGLE_PLAYBACK":
                        play();
                        break;
                    case "ACTION_PREV":
                        playPrevious();
                        break;
                    case "ACTION_NEXT":
                        playNext();
                        break;
                    case "ACTION_CLOSE":
                        stopSelf();
                        break;
                }
            }
        }
        Notification notification = buildMinimalNotification();

        // Android 14+ 必须传递前台服务类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        return START_STICKY;
    }

    // 初始化播放器
    private void initMediaPlayer() {
        Log.d(TAG,"initMediaPlayer");
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
    }
    private void setOnMusicCompletionListener(){
        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG,"OnCompletionListener触发了");
            switch (playbackMode) {
                case RANDOM:
                    int newPosition = currentPosition;
                    if (playList.size() > 1) {
                        Random random = new Random();
                        while (newPosition == currentPosition) {
                            newPosition = random.nextInt(playList.size());
                        }
                        currentPosition = newPosition;
                    }
                    break;
                case SINGLE_LOOP:
                    // 单曲循环：currentPosition 保持不变
                    break;
                case LIST_LOOP:
                default:
                    currentPosition = (currentPosition+1) % playList.size();
                    break;
            }
            Log.d(TAG,"name:"+getCurrentMusic().getMusicName());
            if(onCompleteListenerMain!=null)
            onCompleteListenerMain.updateSongInfo();
            if(onCompleteListenerPlayer!=null)
            onCompleteListenerPlayer.updateSongInfo();
            playCurrent();
        });
    }

    // 添加播放列表
    public void setPlayList(List<Music> songs) {
        for (Music song : songs) {
            boolean exists = false;
            for (Music existingSong : playList) {
                // 假设相同的歌曲具有相同的音乐 URL
                if (existingSong.getMusicUrl().equals(song.getMusicUrl())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                playList.add(song);
            }
        }
    }


    // 根据歌曲 URL 播放对应歌曲
    public void playAt(String songUrl) {
        int position = -1;
        for (int i = 0; i < playList.size(); i++) {
            if (playList.get(i).getMusicUrl().equals(songUrl)) {
                position = i;
                break;
            }
        }
        if (position == -1) {
            Log.e(TAG, "未找到歌曲，URL：" + songUrl);
            return;
        }
        currentPosition = position;
        Log.d(TAG, "歌名：" + playList.get(currentPosition).getMusicName());
        playCurrent();
    }


    // 播放当前索引歌曲
    @SuppressLint("ForegroundServiceType")
    private void playCurrent() {
        positionHandler.removeCallbacks(positionUpdater); // 停止旧的位置更新
        try {
            mediaPlayer.reset();
            // 暂时清除 onCompletionListener，避免重置过程中的异常回调
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setDataSource(playList.get(currentPosition).getMusicUrl());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                if (requestAudioFocus()) {
                    mp.start();
                    startForeground(NOTIFICATION_ID, buildMinimalNotification());
                    if (playbackListener != null) {
                        playbackListener.onDurationChanged(mp.getDuration());
                    }
                    //播放开始后，再设置 onCompletionListener
                    setOnMusicCompletionListener();
                }

                if (playStateChangeListenerCover != null) {
                    playStateChangeListenerCover.onMusicPlay();
                }

                if (playStateChangeListenerActivity != null) {
                    playStateChangeListenerActivity.onMusicPlay();
                }

                if (coverListener != null) {
                    coverListener.onCoverChanged(getCurrentCoverUrl());
                }
                startPositionUpdates();
            });
        } catch (IOException e) {
        }
    }

    // 请求音频焦点
    private boolean requestAudioFocus() {
        return audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    // 释放音频焦点
    private void releaseAudioFocus() {
        audioManager.abandonAudioFocus(audioFocusListener);
    }

    // 下一首
    public void playNext() {
        switch (playbackMode) {
            case RANDOM:
                int newPosition = currentPosition;
                if (playList.size() > 1) {
                    Random random = new Random();
                    while (newPosition == currentPosition) {
                        newPosition = random.nextInt(playList.size());
                    }
                    currentPosition = newPosition;
                }
                break;
            case SINGLE_LOOP:
                // 单曲循环模式下，手动切换可以选择保持当前播放，也可以切换到下一首，
                // 这里我们选择让手动操作仍然切换到下一首（列表循环方式）
                currentPosition = (currentPosition + 1) % playList.size();
                break;
            case LIST_LOOP:
            default:
                currentPosition = (currentPosition + 1) % playList.size();
                break;
        }
        playCurrent();
        if (playStateChangeListenerCover != null) {
            playStateChangeListenerCover.onMusicPlay();
        }
        if(lyricListener!=null) lyricListener.onLyricsChanged();
        startPositionUpdates();
    }

    // 上一首
    public void playPrevious() {
        switch (playbackMode) {
            case RANDOM:
                int newPosition = currentPosition;
                if (playList.size() > 1) {
                    Random random = new Random();
                    while (newPosition == currentPosition) {
                        newPosition = random.nextInt(playList.size());
                    }
                    currentPosition = newPosition;
                }
                break;
            case SINGLE_LOOP:
                // 单曲循环模式下，手动切换同上，我们依然采用列表循环逻辑
                currentPosition = (currentPosition - 1 + playList.size()) % playList.size();
                break;
            case LIST_LOOP:
            default:
                currentPosition = (currentPosition - 1 + playList.size()) % playList.size();
                break;
        }
        playCurrent();
        if (playStateChangeListenerCover != null) {
            playStateChangeListenerCover.onMusicPlay();
        }
        if(lyricListener!=null) lyricListener.onLyricsChanged();
        startPositionUpdates();
    }

    public void play(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            if(playStateChangeListenerCover != null){
                playStateChangeListenerCover.onMusicPause();
            }
            if (playStateChangeListenerActivity != null) {
                playStateChangeListenerActivity.onMusicPause();
            }
        }else{
            mediaPlayer.start();
            if(playStateChangeListenerCover != null){
                playStateChangeListenerCover.onMusicPlay();
            }
            if (playStateChangeListenerActivity != null) {
                playStateChangeListenerActivity.onMusicPlay();
            }
        }
        startPositionUpdates();
    }
    @Override
    public void onDestroy() {
        releaseAudioFocus();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
    public Boolean isPlaying(){
        return mediaPlayer != null ? mediaPlayer.isPlaying(): false;
    }
    public void seekTo(int progress){
        mediaPlayer.seekTo(progress);
        mediaPlayer.start();
    }
    public String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public int getTotalDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }
    public int getCurrentProgress() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }
    public Music getCurrentMusic() {
        if (playList.isEmpty() || currentPosition < 0 || currentPosition >= playList.size()) {
            return null;
        }
        return playList.get(currentPosition);
    }

    public int getMusicListSize(){
        return playList.size();
    }

    // Service 中定义回调控制面板接口
    public interface OnPlaybackUpdateListener {
        void onDurationChanged(int totalDuration);
        void onPositionChanged(int currentPosition);
    }
    private OnPlaybackUpdateListener playbackListener;
    public void setOnPlaybackUpdateListener(OnPlaybackUpdateListener listener) {
        this.playbackListener = listener;
    }

    public String getCurrentCoverUrl() {
        Music currentMusic = getCurrentMusic();
        return currentMusic != null ? currentMusic.getCoverUrl() : "";
    }

    // 定义封面更新回调接口
    public interface OnCoverUpdateListener {
        void onCoverChanged(String coverUrl);
    }

    private OnCoverUpdateListener coverListener;

    public void setOnCoverUpdateListener(OnCoverUpdateListener listener) {
        this.coverListener = listener;
    }

    public interface OnPlayStateChangeListener{
        void onMusicPlay();
        void onMusicPause();
    }
    private OnPlayStateChangeListener playStateChangeListenerCover;

    public void setOnPlayStateChangeListenerCover(OnPlayStateChangeListener listener){
        this.playStateChangeListenerCover = listener;
    }

    private OnPlayStateChangeListener playStateChangeListenerActivity;

    public void playStateChangeListenerActivity(OnPlayStateChangeListener listener){
        this.playStateChangeListenerActivity = listener;
    }

    public List getMusicList(){
        return this.playList;
    }

    public int getCurrentPosition(){
        return currentPosition;
    }
//////////////////////////////////歌词/////////////////////////////////////
    private LyricsSyncListener lyricListener;
    private final Object listenerLock = new Object();

    // 简化版同步监听接口
    public interface LyricsSyncListener {
        void onLyricsPositionChanged(long currentPosition);
        void onLyricsChanged();
    }

    // 设置监听器
    public void setLyricsListener(LyricsSyncListener listener) {
        synchronized (listenerLock) {
            this.lyricListener = listener;
            updatePlaybackPosition(getCurrentProgress());
        }
    }

    // 清除监听器
    public void clearLyricsListener() {
        synchronized (listenerLock) {
            this.lyricListener = null;
        }
    }

    // 更新播放位置
    private void updatePlaybackPosition(long position) {
        synchronized (listenerLock) {
            if (lyricListener != null) {
                // 切换到主线程通知
                new Handler(Looper.getMainLooper()).post(() -> {
                    synchronized (listenerLock) {
                        if (lyricListener != null) {
                            lyricListener.onLyricsPositionChanged(position);
                        }
                    }
                });
            }
        }
    }
    private Handler positionHandler = new Handler(Looper.getMainLooper());
    private Runnable positionUpdater = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                long currentPos = getCurrentProgress();
                updatePlaybackPosition(currentPos);
            }
            // 无论是否播放，都延时再次执行，以便播放开始后能立即同步
            positionHandler.postDelayed(this, 100);
        }
    };

    private void startPositionUpdates() {
        positionHandler.removeCallbacks(positionUpdater);
        positionHandler.post(positionUpdater);
    }

    private IOnComplete onCompleteListenerPlayer;
    public void setOnCompleteListenerPlayer(IOnComplete onCompleteListener){
        this.onCompleteListenerPlayer = onCompleteListener;
    }

    private IOnComplete onCompleteListenerMain;
    public void setOnCompleteListenerMain(IOnComplete onCompleteListenerMain) {
        this.onCompleteListenerMain = onCompleteListenerMain;
    }

    public interface IOnComplete{
        void updateSongInfo();
    }

}
