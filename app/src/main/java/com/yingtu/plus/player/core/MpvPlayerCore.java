package com.yingtu.plus.player.core;

import android.content.Context;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.jdtech.mpv.MPVLib;

/**
 * MPV 内核（基于 libmpv，dev.jdtech.mpv 预编译 .so）。
 *
 * 格式/字幕/硬解兼容性极强，适合疑难直链。通过 MPVLib 的静态 JNI 接口驱动，
 * 以 property 观察器映射到统一 Listener。
 *
 * 注意：libmpv native 使用单例全局状态，App 内同一时刻只应存在一个 MPV 实例，
 * release() 必须 destroy 以便下次重新 create。
 */
public class MpvPlayerCore implements AbstractPlayer, MPVLib.EventObserver {

    private final Context context;
    private Listener listener;
    private Surface surface;
    private boolean created;
    private boolean fileLoaded;
    private String pendingUrl;
    private Map<String, String> pendingHeaders;

    public MpvPlayerCore(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void setListener(Listener l) {
        this.listener = l;
    }

    @Override
    public void setSurface(Surface surface) {
        this.surface = surface;
        if (created && surface != null) MPVLib.attachSurface(surface);
    }

    private void ensureCreated() {
        if (created) return;
        MPVLib.create(context);
        // 基本选项（起播前设置）
        MPVLib.setOptionString("vo", "gpu");
        MPVLib.setOptionString("gpu-context", "android");
        MPVLib.setOptionString("hwdec", "mediacodec-copy");
        MPVLib.setOptionString("ao", "audiotrack");
        MPVLib.setOptionString("force-window", "no");
        MPVLib.setOptionString("idle", "yes");
        MPVLib.setOptionString("cache", "yes");
        MPVLib.setOptionString("save-position-on-quit", "no");
        MPVLib.init();
        MPVLib.addObserver(this);
        MPVLib.observeProperty("time-pos", MPVLib.MPV_FORMAT_INT64);
        MPVLib.observeProperty("duration", MPVLib.MPV_FORMAT_INT64);
        MPVLib.observeProperty("pause", MPVLib.MPV_FORMAT_FLAG);
        MPVLib.observeProperty("eof-reached", MPVLib.MPV_FORMAT_FLAG);
        MPVLib.observeProperty("paused-for-cache", MPVLib.MPV_FORMAT_FLAG);
        MPVLib.observeProperty("width", MPVLib.MPV_FORMAT_INT64);
        MPVLib.observeProperty("height", MPVLib.MPV_FORMAT_INT64);
        created = true;
        if (surface != null) MPVLib.attachSurface(surface);
    }

    @Override
    public void setDataSource(String url, Map<String, String> headers) {
        this.pendingUrl = url;
        this.pendingHeaders = headers;
    }

    @Override
    public void prepare() {
        ensureCreated();
        try {
            if (pendingHeaders != null && !pendingHeaders.isEmpty()) {
                StringBuilder fields = new StringBuilder();
                for (Map.Entry<String, String> e : pendingHeaders.entrySet()) {
                    if ("User-Agent".equalsIgnoreCase(e.getKey())) {
                        MPVLib.setOptionString("user-agent", e.getValue());
                    } else if ("Referer".equalsIgnoreCase(e.getKey())) {
                        MPVLib.setOptionString("referrer", e.getValue());
                    } else {
                        if (fields.length() > 0) fields.append(",");
                        fields.append(e.getKey()).append(": ").append(e.getValue());
                    }
                }
                if (fields.length() > 0) MPVLib.setOptionString("http-header-fields", fields.toString());
            }
            // loadfile 异步加载；FILE_LOADED 事件回调 onPrepared
            List<String> cmd = new ArrayList<>();
            cmd.add("loadfile");
            cmd.add(pendingUrl);
            MPVLib.command(cmd.toArray(new String[0]));
        } catch (Throwable e) {
            if (listener != null) listener.onError("MPV loadfile: " + e.getMessage());
        }
    }

    @Override
    public void start() {
        if (created) MPVLib.setPropertyBoolean("pause", false);
    }

    @Override
    public void pause() {
        if (created) MPVLib.setPropertyBoolean("pause", true);
    }

    @Override
    public boolean isPlaying() {
        if (!created) return false;
        Boolean paused = MPVLib.getPropertyBoolean("pause");
        return paused != null && !paused;
    }

    @Override
    public void seekTo(long positionMs) {
        if (created) MPVLib.command(new String[]{"seek", String.valueOf(positionMs / 1000.0), "absolute"});
    }

    @Override
    public long getCurrentPosition() {
        if (!created) return 0;
        Integer sec = MPVLib.getPropertyInt("time-pos");
        return sec == null ? 0 : sec * 1000L;
    }

    @Override
    public long getDuration() {
        if (!created) return 0;
        Integer sec = MPVLib.getPropertyInt("duration");
        return sec == null ? 0 : sec * 1000L;
    }

    @Override
    public void setSpeed(float speed) {
        if (created) MPVLib.setPropertyDouble("speed", (double) speed);
    }

    @Override
    public void release() {
        if (!created) return;
        try {
            MPVLib.removeObserver(this);
            if (surface != null) MPVLib.detachSurface();
            MPVLib.command(new String[]{"stop"});
            MPVLib.destroy();
        } catch (Throwable ignored) {
        }
        created = false;
        fileLoaded = false;
    }

    @Override
    public int core() {
        return CORE_MPV;
    }

    // ===== MPVLib.EventObserver =====

    @Override
    public void eventProperty(String property) {
    }

    @Override
    public void eventProperty(String property, long value) {
        if (listener == null) return;
        if ("width".equals(property) || "height".equals(property)) {
            Integer w = MPVLib.getPropertyInt("width");
            Integer h = MPVLib.getPropertyInt("height");
            if (w != null && h != null && w > 0 && h > 0) listener.onVideoSizeChanged(w, h);
        }
    }

    @Override
    public void eventProperty(String property, double value) {
    }

    @Override
    public void eventProperty(String property, boolean value) {
        if (listener == null) return;
        if ("paused-for-cache".equals(property)) {
            listener.onBuffering(value);
        } else if ("eof-reached".equals(property)) {
            if (value) listener.onCompletion();
        }
    }

    @Override
    public void eventProperty(String property, String value) {
    }

    @Override
    public void event(int eventId) {
        if (listener == null) return;
        if (eventId == MPVLib.MPV_EVENT_FILE_LOADED) {
            fileLoaded = true;
            listener.onPrepared();
            listener.onBuffering(false);
        } else if (eventId == MPVLib.MPV_EVENT_END_FILE) {
            // 某些结尾只有 END_FILE
            // eof-reached 已处理 completion，这里不重复
        }
    }
}
