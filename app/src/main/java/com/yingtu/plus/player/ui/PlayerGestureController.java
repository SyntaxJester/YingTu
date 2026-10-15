package com.yingtu.plus.player.ui;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

/**
 * 播放器手势控制器（从 webhtv CustomKeyDown 移植）。
 * 支持：双击暂停、长按倍速、左右侧滑调亮度/音量、快进快退、双指缩放。
 */
public class PlayerGestureController extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener {

    public interface Listener {
        void onSeeking(long deltaMs);
        void onSeekEnd(long deltaMs);
        void onSpeedUp();
        void onSpeedEnd();
        void onBrightness(int percent);
        void onVolume(int percent);
        void onFlingUp();
        void onFlingDown();
        void onSingleTap();
        void onDoubleTap();
        void onScale(double scale);
        void onTouchEnd();
    }

    private static final int DISTANCE = 100;
    private final Activity activity;
    private final View videoView;
    private final AudioManager manager;
    private final Listener listener;
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector detector;

    private boolean changeBright, changeVolume, changeSpeed, changeTime, changeScale;
    private boolean multiTouch, animating, touch, lock;
    private float bright, currentBright, volume, scale = 1.0f;
    private long time = 0;

    public static PlayerGestureController create(Activity activity, View videoView, Listener listener) {
        return new PlayerGestureController(activity, videoView, listener);
    }

    private PlayerGestureController(Activity activity, View videoView, Listener listener) {
        this.manager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        this.scaleDetector = new ScaleGestureDetector(activity, this);
        this.detector = new GestureDetector(activity, this);
        this.listener = listener;
        this.activity = activity;
        this.videoView = videoView;
        applyBrightness();
    }

    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) multiTouch = false;
        if (action == MotionEvent.ACTION_POINTER_DOWN) multiTouch = true;
        if (action == MotionEvent.ACTION_UP) {
            listener.onTouchEnd();
            if (changeBright) saveBrightness();
            if (changeSpeed) listener.onSpeedEnd();
            if (changeTime) listener.onSeekEnd(time);
        }
        return e.getPointerCount() == 2 ? scaleDetector.onTouchEvent(e) : detector.onTouchEvent(e);
    }

    private void saveBrightness() {
        WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
        attrs.screenBrightness = currentBright;
        activity.getWindow().setAttributes(attrs);
    }

    private void applyBrightness() {
        WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
        if (attrs.screenBrightness >= 0) {
            currentBright = attrs.screenBrightness;
        }
    }

    public void resetScale() {
        if (scale == 1.0f) return;
        videoView.animate().scaleX(1.0f).scaleY(1.0f).translationX(0f).translationY(0f)
                .setDuration(250).withEndAction(() -> {
                    videoView.setPivotX(videoView.getWidth() / 2f);
                    videoView.setPivotY(videoView.getHeight() / 2f);
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
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int edgeMargin = 60; // ~24dp
        float x = e.getRawX();
        float y = e.getRawY();
        return x < edgeMargin || x > screenWidth - edgeMargin || y < edgeMargin;
    }

    private boolean isSide(MotionEvent e) {
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int four = screenWidth / 4;
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
        bright = currentBright;
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
        if (isMultiple(e1) || isEdge(e1) || changeScale || lock || changeSpeed) return true;
        float deltaX = e2.getX() - e1.getX();
        float deltaY = e1.getY() - e2.getY();
        if (touch) checkFunc(Math.abs(deltaX), Math.abs(deltaY), e2);
        if (changeTime) listener.onSeeking(time = (long) (deltaX * 50));
        if (changeBright) setBrightness(deltaY);
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
        if (isMultiple(e1) || isEdge(e1) || isSide(e1) || changeScale || lock || animating) return true;
        checkFling(e1, e2);
        return true;
    }

    private void checkFunc(float distX, float distY, MotionEvent e2) {
        float dist = (float) Math.sqrt(distX * distX + distY * distY);
        if (dist < 50) return;
        if (distX >= distY) changeTime = true;
        else if (isSide(e2)) checkSide(e2);
        touch = false;
    }

    private void checkFling(MotionEvent e1, MotionEvent e2) {
        float dx = e2.getX() - e1.getX();
        float dy = e2.getY() - e1.getY();
        double angle = Math.toDegrees(Math.atan2(Math.abs(dy), Math.abs(dx)));
        if (angle > 70 && e1.getY() - e2.getY() > DISTANCE) {
            videoView.animate().translationYBy(24).setDuration(150)
                    .withStartAction(() -> animating = true)
                    .withEndAction(() -> {
                        videoView.animate().translationY(0).setDuration(100)
                                .withStartAction(listener::onFlingUp)
                                .withEndAction(() -> animating = false).start();
                    }).start();
        } else if (angle > 70 && e2.getY() - e1.getY() > DISTANCE) {
            videoView.animate().translationYBy(-24).setDuration(150)
                    .withStartAction(() -> animating = true)
                    .withEndAction(() -> {
                        videoView.animate().translationY(0).setDuration(100)
                                .withStartAction(listener::onFlingDown)
                                .withEndAction(() -> animating = false).start();
                    }).start();
        }
    }

    private void checkSide(MotionEvent e2) {
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int half = screenWidth / 2;
        if (e2.getRawX() > half) changeVolume = true;
        else changeBright = true;
    }

    private void setBrightness(float deltaY) {
        int height = videoView.getMeasuredHeight();
        float brightness = deltaY * 2.0f / height + bright;
        if (brightness < 0) brightness = 0f;
        if (brightness > 1.0f) brightness = 1.0f;
        WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
        attrs.screenBrightness = brightness;
        activity.getWindow().setAttributes(attrs);
        currentBright = brightness;
        listener.onBrightness((int) (brightness * 100));
    }

    private void setVolume(float deltaY) {
        int height = videoView.getMeasuredHeight();
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
        if (changeBright || changeVolume || changeSpeed || changeTime || lock) {
            changeScale = false;
            return false;
        }
        return changeScale = true;
    }

    @Override
    public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
        activity.runOnUiThread(() -> {
            if (scale < 1.0f) scale = 1.0f;
            if (scale > 5.0f) scale = 5.0f;
            changeScale = false;
        });
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
}
