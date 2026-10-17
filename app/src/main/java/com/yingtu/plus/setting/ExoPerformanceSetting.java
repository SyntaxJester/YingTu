package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class ExoPerformanceSetting {
    public static final int CODEC_QUEUE_AUTO = 0;
    public static final int CODEC_QUEUE_ASYNC = 1;
    public static final int CODEC_QUEUE_SYNC = 2;
    public static final int FRAME_RATE_OFF = 0;
    public static final int FRAME_RATE_SEAMLESS = 1;

    public static int getCodecQueueMode() { return Prefers.getInt("perf_exo_codec_queue_mode", CODEC_QUEUE_AUTO); }
    public static void putCodecQueueMode(int v) { Prefers.put("perf_exo_codec_queue_mode", v); }
    public static String getCodecQueueText() { return "auto"; }
    
    public static int getFrameRateMode() { return Prefers.getInt("perf_exo_frame_rate_mode", FRAME_RATE_OFF); }
    public static void putFrameRateMode(int v) { Prefers.put("perf_exo_frame_rate_mode", v); }
    public static int getFrameRateStrategy() { return getFrameRateMode(); }
    public static String getFrameRateText() { return "off"; }
    
    public static int getStartBufferMs() { return Prefers.getInt("perf_exo_start_buffer_ms", 1000); }
    public static void putStartBufferMs(int v) { Prefers.put("perf_exo_start_buffer_ms", v); }
    public static int getEffectiveStartBufferMs() { return getStartBufferMs(); }
    
    public static int getRebufferMs() { return Prefers.getInt("perf_exo_rebuffer_ms", 1000); }
    public static void putRebufferMs(int v) { Prefers.put("perf_exo_rebuffer_ms", v); }
    public static int getEffectiveRebufferMs() { return getRebufferMs(); }
    
    public static boolean isPrioritizeTime() { return Prefers.getBoolean("perf_exo_prioritize_time", false); }
    public static int getAutoDefaultStartBufferMs() { return getStartBufferMs(); }
    public static int getAutoSessionRebufferMs() { return getRebufferMs(); }
    
    public static void beginAutoSession() {}
    public static void discardAutoSession() {}
    public static void recordAutoSession(String key, int value) {}
    public static void refreshAutoSession(String key, int value) {}
    public static void updateAutoSession(String key, int value) {}
    public static long getNetworkProtectionMinimumSpeed() { return 0; }
    public static boolean isNetworkProtectionEnabled() { return false; }
}
