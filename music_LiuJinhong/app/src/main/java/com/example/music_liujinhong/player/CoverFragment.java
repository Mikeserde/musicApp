package com.example.music_liujinhong.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.music_liujinhong.R;

public class CoverFragment extends Fragment implements MusicService.OnCoverUpdateListener,MusicService.OnPlayStateChangeListener {

    private ImageView coverImage;
    private MusicService musicService;
    private boolean isBound = false;
    // 服务连接回调
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;
            musicService.setOnCoverUpdateListener(CoverFragment.this); // 注册监听
            updateCover(musicService.getCurrentCoverUrl()); // 初始加载封面
            if(musicService.isPlaying()){
                onMusicPlay();
            }else{
                onMusicPause();
            }
            musicService.setOnPlayStateChangeListenerCover(CoverFragment.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cover_viewpager_page, container, false);
        coverImage = view.findViewById(R.id.albumCover);
        return view;
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

    // 实现封面更新回调
    @Override
    public void onCoverChanged(String coverUrl) {
        updateCover(coverUrl);
    }

    private void updateCover(String coverUrl) {
    if (coverImage != null && !TextUtils.isEmpty(coverUrl)) {
        Glide.with(this)
                .asBitmap()
                .transform(new CenterCrop())
                .load(coverUrl)
                .addListener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap bitmap, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        // 用 Palette 分析颜色
                        extractDominantColor(bitmap);
                        return false;
                    }
                })
                .into(coverImage);
    }
}

    // 提取主色
    private void extractDominantColor(Bitmap bitmap) {
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                if (palette != null) {
                    // 获取主色样本（占比最大的颜色）
                    Palette.Swatch dominantSwatch = palette.getDominantSwatch();
                    if (dominantSwatch != null) {
                        int dominantColor = dominantSwatch.getRgb();
                        applyBackgroundColor(dominantColor);
                    } else {
                        // 备选方案：获取柔和的深色
                        int fallbackColor = palette.getDarkMutedColor(
                                ContextCompat.getColor(requireContext(), R.color.white)
                        );
                        applyBackgroundColor(fallbackColor);
                    }
                }
            }
        });
    }

    // 应用背景色（带渐变过渡）
    private void applyBackgroundColor(int targetColor) {
        Activity activity = getActivity();
        if (activity instanceof MusicPlayerActivity) {
            ((MusicPlayerActivity) activity).animateBackgroundColor(targetColor);
        }
    }

    @Override
    public void onDestroy() {
        if (musicService != null) {
            musicService.setOnCoverUpdateListener(null);
        }
        super.onDestroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startRotationAnimation();
    }

    private ObjectAnimator rotateAnimator;
    private float currentRotation = 0f;  // 记录当前旋转角度
    private boolean isAnimating = false; // 记录动画状态

    // 初始化动画（仅一次）
    private void initRotationAnimation() {
        if (rotateAnimator != null) return;

        rotateAnimator = ObjectAnimator.ofFloat(coverImage, "rotation", 0f, 360f);
        rotateAnimator.setDuration(15000);
        rotateAnimator.setInterpolator(new LinearInterpolator());
        rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);

        // 监听动画循环，更新起始角度
        rotateAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                currentRotation = (currentRotation + 360f) % 360f;
                rotateAnimator.setFloatValues(currentRotation, currentRotation + 360f);
            }
        });
    }

    // 开始/继续旋转
    private void startRotationAnimation() {
        if (rotateAnimator == null) {
            initRotationAnimation();
        }
        if (!isAnimating) {
            // 从当前角度开始
            rotateAnimator.setFloatValues(currentRotation, currentRotation + 360f);
            rotateAnimator.start();
            isAnimating = true;
        }
    }

    // 暂停旋转（保存当前角度）
    private void pauseRotationAnimation() {
        if (rotateAnimator != null && isAnimating) {
            currentRotation = coverImage.getRotation() % 360f; // 保存当前角度
            rotateAnimator.cancel();
            isAnimating = false;
        }
    }

    @Override
    public void onDestroyView() {
        if (rotateAnimator != null) {
            rotateAnimator.cancel();
        }
        super.onDestroyView();
    }

    public void onMusicPlay() {
        startRotationAnimation();
    }

    public void onMusicPause() {
        pauseRotationAnimation();
    }

}