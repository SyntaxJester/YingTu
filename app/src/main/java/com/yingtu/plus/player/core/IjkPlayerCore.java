package com.yingtu.plus.player.core;

import android.content.Context;
import android.view.Surface;

import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * IJKPlayer 内核（基于 bilibili ijkplayer，ffmpeg 软解兼容性强）。
 *
 * 用 DKPlayer(doikki) 打包的 player-ijk AAR 自带的 arm64/v7a 预编译 .so，
 * 不需要本地 NDK 构建。
 */
public class IjkPlayerCore implements AbstractPlayer {

    private IjkMediaPlayer player;
    private Listener listener;
    private Surface surface;
    private String url;
    private Map<String, String> headers;

    public IjkPlayerCore(Context context) {
    }

    @Override
    public void setListener(Listener l) {
        this.listener = l;
    }

    @Override
    public void setSurface(Surface surface) {
        this.surface = surface;
        if (player != null) player.setSurface(surface);
    }

    private void ensure() {
        if (player != null) return;
        player = new IjkMediaPlayer();
        // 常用优化项：硬解自动、精准 seek、起播快
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1);
        if (surface != null) player.setSurface(surface);

        player.setOnPreparedListener(mp -> { if (listener != null) listener.onPrepared(); });
        player.setOnCompletionListener(mp -> { if (listener != null) listener.onCompletion(); });
        player.setOnErrorListener((mp, what, extra) -> {
            if (listener != null) listener.onError("IJK error " + what + "/" + extra);
            return true;
        });
        player.setOnInfoListener((mp, what, extra) -> {
            if (listener == null) return false;
            if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) listener.onBuffering(true);
            else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) listener.onBuffering(false);
            else if (what == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) listener.onBuffering(false);
            return false;
        });
        player.setOnVideoSizeChangedListener((mp, w, h, sarNum, sarDen) -> {
            if (listener != null) listener.onVideoSizeChanged(w, h);
        });
    }

    @Override
    public void setDataSource(String url, Map<String, String> headers) {
        this.url = url;
        this.headers = headers;
    }

    @Override
    public void prepare() {
        ensure();
        try {
            if (headers != null && !headers.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                String ua = null;
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    if ("User-Agent".equalsIgnoreCase(e.getKey())) ua = e.getValue();
                    else sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
                }
                if (ua != null) player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", ua);
                if (sb.length() > 0) player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "headers", sb.toString());
            }
            player.setDataSource(url);
            player.prepareAsync();
        } catch (Throwable e) {
            if (listener != null) listener.onError("IJK setDataSource: " + e.getMessage());
        }
    }

    @Override
    public void start() {
        if (player != null) player.start();
    }

    @Override
    public void pause() {
        try { if (player != null) player.pause(); } catch (Throwable ignored) {}
    }

    @Override
    public boolean isPlaying() {
        try { return player != null && player.isPlaying(); } catch (Throwable e) { return false; }
    }

    @Override
    public void seekTo(long positionMs) {
        try { if (player != null) player.seekTo(positionMs); } catch (Throwable ignored) {}
    }

    @Override
    public long getCurrentPosition() {
        try { return player != null ? player.getCurrentPosition() : 0; } catch (Throwable e) { return 0; }
    }

    @Override
    public long getDuration() {
        try { return player != null ? player.getDuration() : 0; } catch (Throwable e) { return 0; }
    }

    @Override
    public void setSpeed(float speed) {
        try { if (player != null) player.setSpeed(speed); } catch (Throwable ignored) {}
    }

    @Override
    public void release() {
        if (player != null) {
            try {
                player.setSurface(null);
                player.release();
            } catch (Throwable ignored) {}
            player = null;
        }
    }

    @Override
    public int core() {
        return CORE_IJK;
    }
}
