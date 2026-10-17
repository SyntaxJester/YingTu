package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class PlaybackPerformanceSetting {
    public static final int DEFAULT_PROFILE = 0;
    
    public static boolean isAuto() { return Prefers.getBoolean("perf_auto", true); }
    public static boolean isAdaptiveDowngradeEnabled() { return Prefers.getBoolean("perf_adaptive_downgrade", true); }
    public static boolean isBandwidthMeterEnabled() { return Prefers.getBoolean("perf_bandwidth_meter", true); }
    public static boolean isDecoderFallbackEnabled() { return Prefers.getBoolean("perf_decoder_fallback", false); }
    public static boolean isDv7P81Enabled() { return Prefers.getBoolean("perf_dv7_p81", false); }
    public static boolean isDv7Hdr10FallbackEnabled() { return Prefers.getBoolean("perf_dv7_hdr10_fallback", false); }
    public static boolean isDynamicSchedulingEnabled() { return Prefers.getBoolean("perf_dynamic_sched", true); }
    public static boolean isHighBufferEnabled() { return Prefers.getBoolean("perf_high_buffer", false); }
    public static boolean isLateDropInputEnabled() { return Prefers.getBoolean("perf_late_drop", false); }
    public static boolean isLoadOnlySelectedTracksEnabled() { return Prefers.getBoolean("perf_load_only_selected", false); }
    public static boolean isOverridden() { return Prefers.getBoolean("perf_overridden", false); }
    public static boolean isSoftVideoTuneEnabled() { return Prefers.getBoolean("perf_soft_video_tune", false); }
    public static boolean isTrackLimitEnabled() { return Prefers.getBoolean("perf_track_limit", false); }
    public static int getMpvDv7HandlingMode() { return Prefers.getInt("perf_mpv_dv7", 0); }
    public static int getProfile() { return Prefers.getInt("perf_profile", DEFAULT_PROFILE); }
    public static boolean hasAutomaticOptions() { return isAuto(); }
}
