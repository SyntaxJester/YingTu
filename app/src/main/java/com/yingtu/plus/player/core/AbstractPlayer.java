package com.yingtu.plus.player.core;

import android.view.Surface;

import java.util.Map;

/**
 * 播放内核抽象。ExoPlayer / IJK / MPV 三种实现统一到这个接口，
 * 便于 PlayActivity 用同一套 UI 控制不同内核。
 */
public interface AbstractPlayer {

    interface Listener {
        void onPrepared();
        void onCompletion();
        void onBuffering(boolean buffering);
        void onError(String message);
        void onVideoSizeChanged(int width, int height);
    }

    /** 内核类型常量（与 Hawk 持久化值一致）。 */
    int CORE_EXO = 0;
    int CORE_IJK = 1;
    int CORE_MPV = 2;

    void setListener(Listener listener);

    void setSurface(Surface surface);

    void setDataSource(String url, Map<String, String> headers);

    void prepare();

    void start();

    void pause();

    boolean isPlaying();

    void seekTo(long positionMs);

    long getCurrentPosition();

    long getDuration();

    void setSpeed(float speed);

    void release();

    /** 该内核标识，用于日志/UI 显示。 */
    int core();
}
