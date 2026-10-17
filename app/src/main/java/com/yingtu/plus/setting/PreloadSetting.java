package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class PreloadSetting {
    public static final int MIN_THREADS = 1;
    public static final int MAX_THREADS = 4;
    public static final int DEFAULT_THREADS = 1;
    public static final int MIN_SIZE_MB = 128;
    public static final int MAX_SIZE_MB = 32768;
    public static final int MIN_TIME_SECONDS = 20;
    public static final int MAX_TIME_SECONDS = 120;
    public static final int DEFAULT_TIME_SECONDS = 20;
    public static final int WHOLE_MEDIA_AHEAD_SECONDS = 0;
    public static final int DEFAULT_AHEAD_SECONDS = 300;

    public static boolean isPreload() { return Prefers.getBoolean("preload_enabled", false); }
    public static boolean isPreload(int kernel) { return isPreload(); }
    public static void putPreload(boolean v) { Prefers.put("preload_enabled", v); }
    
    public static int getPreloadThreads() { return Prefers.getInt("preload_threads", DEFAULT_THREADS); }
    public static int getPreloadThreads(int kernel) { return getPreloadThreads(); }
    public static void putPreloadThreads(int t) { Prefers.put("preload_threads", t); }
    
    public static int getPreloadSizeMb() { return Prefers.getInt("preload_size_mb", 512); }
    public static int getPreloadSizeMb(int kernel) { return getPreloadSizeMb(); }
    public static void putPreloadSizeMb(int size) { Prefers.put("preload_size_mb", size); }
    
    public static int getPreloadDurationMs() { return Prefers.getInt("preload_duration_ms", DEFAULT_TIME_SECONDS * 1000); }
    public static int getPreloadAheadDurationMs() { return Prefers.getInt("preload_ahead_ms", DEFAULT_AHEAD_SECONDS * 1000); }
    public static long getPreloadSizeBytes() { return (long) getPreloadSizeMb() * 1024 * 1024; }
    
    public static int getPausePreloadPolicy() { return Prefers.getInt("preload_pause_policy", 2); }
}
