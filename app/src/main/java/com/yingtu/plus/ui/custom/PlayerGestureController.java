package com.yingtu.plus.ui.custom;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import androidx.annotation.NonNull;

import com.yingtu.plus.util.UiUtil;

/**
 * 播放页手势状态机（移植自 webhtv CustomKeyDown）。
 *
 * 支持：
 *  - 左右滑动 → 快进/快退（onSeeking / onSeekEnd）
 *  - 左半屏上下滑 → 亮度（onBright）
 *  - 右半屏上下滑 → 音量（onVolume）
 *  - 长按 → 倍速播放（onSpeedUp / onSpeedEnd）
 *  - 单击 → 显隐控制层（onSingleTap）
 *  - 双击 → 播放/暂停（onDoubleTap）
 *  - 双指捏合 → 画面缩放
 *  - 快速上/下滑 → 上/下一集（onFlingUp / onFlingDown）
 */
public class PlayerGestureController extends GestureDetector.SimpleOnGestureListener
        implements ScaleGestureDetector.OnScaleGestureListener {

    private static final int DISTANCE = 100;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector detector;
    private final AudioManager manager;
    private final Listener listener;
    private final Activity activity;
    private final View videoView;

    private boolean changeBright;
    private boolean changeVolume;
    private boolean changeSpeed;
    private boolean changeScale;
    private boolean changeTime;
    private boolean multiTouch;
    private boolean animating;
    private boolean touch;
    private boolean lock;

    private float bright;
    private float currentBright;
    private float volume;
    private float scale;
    private long time;

    public static PlayerGestureController create(Activity activity, View videoView) {
        return new PlayerGestureController(activity, videoView);
    }

    private PlayerGestureController(Activity activity, View videoView) {
        this.manager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        this.scaleDetector = new ScaleGestureDetector(activity, this);
        this.detector = new GestureDetector(activity, this);
        this.listener = (Listener) activity;
        this.videoView = videoView;
        this.activity = activity;
        this.scale = 1.0f;
    }

    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) multiTouch = false;
        if (action == MotionEvent.ACTION_POINTER_DOWN) multiTouch = true;
        if (action == MotionEvent.ACTION_UP) listener.onTouchEnd();
        if (changeBright && action == MotionEvent.ACTION_UP) listener.onBrightEnd(currentBright);
        if (changeSpeed && action == MotionEvent.ACTION_UP) listener.onSpeedEnd();
        if (changeTime && action == MotionEvent.ACTION_UP) listener.onSeekEnd(time);
        return e.getPointerCount() == 2 ? scaleDetector.onTouchEvent(e) : detector.onTouchEvent(e);
    }

    public void resetScale() {
        if (scale == 1.0f) return;
        videoView.animate().scaleX(1.0f).scaleY(1.0f).translationX(0f).translationY(0f)
                .setDuration(250).withEndAction(() -> {
                    videoView.setPivotY(videoView.getHeight() / 2.0f);
                    videoView.setPivotX(videoView.getWidth() / 2.0f);
                    scale = 1.0f;
                }).start();
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    public float getScale() {
        return scale;
    }

    private boolean isMultiple(MotionEvent e) {
        return e.getPointerCount() > 1;
    }

    private boolean isEdge(MotionEvent e) {
        return UiUtil.isEdge(com.yingtu.plus.base.App.context(), e, UiUtil.dp2px(24));
    }

    private boolean isSide(MotionEvent e) {
        int four = UiUtil.getScreenWidth(com.yingtu.plus.base.App.context()) / 4;
        float x = e.getRawX();
        return x <= four || x >= four * 3;
    }

    private void reset() {
        time = 0;
        touch = true;
        changeTime = false;
        changeSpeed = false;
        changeBright = false;
        changeVolume = false;
        bright = UiUtil.getBrightness(activity);
        currentBright = bright;
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        if (isMultiple(e) || isEdge(e) || changeScale || lock) return true;
        reset();
        return true;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {
        if (multiTouch || isEdge(e) || changeScale || lock) return;
        listener.onSpeedUp();
        changeSpeed = true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
        if (e1 == null || isMultiple(e1) || isEdge(e1) || changeScale || lock || changeSpeed) return true;
        float deltaX = e2.getX() - e1.getX();
        float deltaY = e1.getY() - e2.getY();
        if (touch) checkFunc(Math.abs(deltaX), Math.abs(deltaY), e2);
        if (changeTime) listener.onSeeking(time = (long) (deltaX * 50));
        if (changeBright) setBright(deltaY);
        if (changeVolume) setVolume(deltaY);
        return true;
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        if (isMultiple(e) || isEdge(e) || changeScale) return true;
        listener.onDoubleTap();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        if (isMultiple(e) || changeScale) return true;
        listener.onSingleTap();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || isMultiple(e1) || isEdge(e1) || isSide(e1) || changeScale || lock || animating) return true;
        checkFunc(e1, e2);
        return true;
    }

    private void checkFunc(float distanceX, float distanceY, MotionEvent e2) {
        if ((float) Math.sqrt(distanceX * distanceX + distanceY * distanceY) < UiUtil.dp2px(20)) return;
        if (distanceX >= distanceY) changeTime = true;
        else if (isSide(e2)) checkSide(e2);
        touch = false;
    }

    private void checkFunc(MotionEvent e1, MotionEvent e2) {
        float dx = e2.getX() - e1.getX();
        float dy = e2.getY() - e1.getY();
        double angle = Math.toDegrees(Math.atan2(Math.abs(dy), Math.abs(dx)));
        if (angle > 70 && e1.getY() - e2.getY() > DISTANCE) {
            videoView.animate().translationYBy(UiUtil.dp2px(-24)).setDuration(150)
                    .withStartAction(() -> animating = true)
                    .withEndAction(() -> videoView.animate().translationY(0).setDuration(100)
                            .withStartAction(listener::onFlingUp)
                            .withEndAction(() -> animating = false).start()).start();
        } else if (angle > 70 && e2.getY() - e1.getY() > DISTANCE) {
            videoView.animate().translationYBy(UiUtil.dp2px(24)).setDuration(150)
                    .withStartAction(() -> animating = true)
                    .withEndAction(() -> videoView.animate().translationY(0).setDuration(100)
                            .withStartAction(listener::onFlingDown)
                            .withEndAction(() -> animating = false).start()).start();
        }
    }

    private void checkSide(MotionEvent e2) {
        int half = UiUtil.getScreenWidth(com.yingtu.plus.base.App.context()) / 2;
        if (e2.getRawX() > half) changeVolume = true;
        else changeBright = true;
    }

    private void setBright(float deltaY) {
        int height = videoView.getMeasuredHeight();
        if (height <= 0) return;
        float brightness = deltaY * 2.0f / height + bright;
        if (brightness < 0) brightness = 0f;
        if (brightness > 1.0f) brightness = 1.0f;
        WindowManager.LayoutParams attributes = activity.getWindow().getAttributes();
        attributes.screenBrightness = brightness;
        activity.getWindow().setAttributes(attributes);
        currentBright = brightness;
        listener.onBright((int) (brightness * 100));
    }

    private void setVolume(float deltaY) {
        int height = videoView.getMeasuredHeight();
        if (height <= 0) return;
        int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float deltaV = deltaY * 2.0f / height * maxVolume;
        float index = volume + deltaV;
        if (index > maxVolume) index = maxVolume;
        if (index < 0) index = 0;
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) index, 0);
        listener.onVolume((int) (index / maxVolume * 100.0f));
    }

    @Override
    public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
        if (changeBright || changeVolume || changeSpeed || changeTime || lock) return changeScale = false;
        return changeScale = true;
    }

    @Override
    public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> changeScale = false, 500);
    }

    @Override
    public boolean onScale(@NonNull ScaleGestureDetector detector) {
        scale *= detector.getScaleFactor();
        scale = Math.max(1.0f, Math.min(scale, 5.0f));
        videoView.setPivotX(detector.getFocusX());
        videoView.setPivotY(detector.getFocusY());
        videoView.setScaleX(scale);
        videoView.setScaleY(scale);
        return true;
    }

    public interface Listener {

        void onSeeking(long time);

        void onSeekEnd(long time);

        void onSpeedUp();

        void onSpeedEnd();

        void onBright(int progress);

        default void onBrightEnd(float value) {
        }

        void onVolume(int progress);

        void onFlingUp();

        void onFlingDown();

        void onSingleTap();

        void onDoubleTap();

        void onTouchEnd();
    }
}
