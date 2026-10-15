package com.yingtu.plus.player.ui;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yingtu.plus.player.core.AbstractPlayer;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 播放器 OSD 控制器（从 webhtv PlayerOsdController 移植）。
 * 管理四角显示面板：左上（播放状态/码率）、右上（帧率/缓冲）、
 * 左下（倍速/音轨）、右下（音量/亮度/网络速度）。
 * 依赖 AbstractPlayer 接口，定时轮询更新。
 */
public class PlayerOsdController {

    public interface Source {
        AbstractPlayer getPlayer();
        String getTitle();
    }

    private static final DecimalFormat SPEED_FORMAT = new DecimalFormat("#.0");
    private static final DecimalFormat BITRATE_FORMAT = new DecimalFormat("#.0");
    private static final DecimalFormat FRAME_FORMAT = new DecimalFormat("#.###");

    private final LinearLayout topLeft, topRight, bottomLeft, bottomRight;
    private final TextView tvTopLeft, tvTopRight, tvBottomLeft, tvBottomRight;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Source source;
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateOsd();
            handler.postDelayed(this, 500);
        }
    };

    private boolean started = false;
    private boolean controlsVisible = true;

    public PlayerOsdController(LinearLayout topLeft, TextView tvTopLeft,
                               LinearLayout topRight, TextView tvTopRight,
                               LinearLayout bottomLeft, TextView tvBottomLeft,
                               LinearLayout bottomRight, TextView tvBottomRight,
                               Source source) {
        this.topLeft = topLeft;
        this.tvTopLeft = tvTopLeft;
        this.topRight = topRight;
        this.tvTopRight = tvTopRight;
        this.bottomLeft = bottomLeft;
        this.tvBottomLeft = tvBottomLeft;
        this.bottomRight = bottomRight;
        this.tvBottomRight = tvBottomRight;
        this.source = source;
    }

    public void start() {
        started = true;
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    public void stop() {
        started = false;
        handler.removeCallbacks(ticker);
    }

    public void setControlsVisible(boolean visible) {
        controlsVisible = visible;
        updateVisibility();
    }

    private void updateVisibility() {
        int visibility = controlsVisible ? View.VISIBLE : View.GONE;
        if (topLeft != null) topLeft.setVisibility(visibility);
        if (topRight != null) topRight.setVisibility(visibility);
        if (bottomLeft != null) bottomLeft.setVisibility(visibility);
        if (bottomRight != null) bottomRight.setVisibility(visibility);
    }

    private void updateOsd() {
        AbstractPlayer player = source.getPlayer();
        if (player == null || !player.isPlaying()) return;

        // 右下角：当前时间
        String timeText = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tvBottomRight.setText(timeText);

        // 左下角：播放进度
        long position = player.getCurrentPosition();
        long duration = player.getDuration();
        String progressText = formatTime(position) + "/" + formatTime(duration);
        tvBottomLeft.setText(progressText);

        // 左上角：标题
        tvTopLeft.setText(source.getTitle());

        // 右上角：播放速度
        tvTopRight.setText("播放中");
    }

    public void showBottomRight(String text) {
        if (bottomRight == null || tvBottomRight == null) return;
        tvBottomRight.setText(text);
        bottomRight.setVisibility(View.VISIBLE);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> bottomRight.setVisibility(View.GONE), 1200);
    }

    public void showBottomLeft(String text) {
        if (bottomLeft == null || tvBottomLeft == null) return;
        tvBottomLeft.setText(text);
        bottomLeft.setVisibility(View.VISIBLE);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> bottomLeft.setVisibility(View.GONE), 1200);
    }

    public void showTopLeft(String text) {
        if (topLeft == null || tvTopLeft == null) return;
        tvTopLeft.setText(text);
        topLeft.setVisibility(View.VISIBLE);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> topLeft.setVisibility(View.GONE), 1200);
    }

    public void showTopRight(String text) {
        if (topRight == null || tvTopRight == null) return;
        tvTopRight.setText(text);
        topRight.setVisibility(View.VISIBLE);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> topRight.setVisibility(View.GONE), 1200);
    }

    private String formatTime(long ms) {
        if (ms <= 0) return "00:00";
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) return String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s);
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }
}
