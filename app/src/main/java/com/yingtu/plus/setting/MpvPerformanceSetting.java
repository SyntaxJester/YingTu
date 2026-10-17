package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class MpvPerformanceSetting {
    public static final int OUTPUT_AUTO = 0;
    public static final int OUTPUT_GPU = 1;
    public static final int OUTPUT_SURFACE_DIRECT = 2;
    public static final int HWDEC_AUTO = 0;
    public static final int HWDEC_DIRECT = 1;
    public static final int SYNC_AUDIO = 0;
    public static final int FRAME_DROP_OFF = 1;
    public static final int VULKAN_BACKEND_AUTO = 0;

    public static int getOutputMode() { return Prefers.getInt("perf_mpv_output_mode", OUTPUT_AUTO); }
    public static String getOutputModeText() { return "auto"; }
    public static boolean shouldUseSurfaceDirect() { return getOutputMode() == OUTPUT_SURFACE_DIRECT; }
    
    public static int getHwdecOption() { return Prefers.getInt("perf_mpv_hwdec", HWDEC_AUTO); }
    public static String getHwdecOptionText() { return "auto"; }
    
    public static int getSyncOption() { return Prefers.getInt("perf_mpv_sync", SYNC_AUDIO); }
    public static String getSyncOptionText() { return "audio"; }
    
    public static int getFrameDropOption() { return Prefers.getInt("perf_mpv_frame_drop", FRAME_DROP_OFF); }
    public static String getFrameDropOptionText() { return "off"; }
    
    public static boolean isInterpolation() { return Prefers.getBoolean("perf_mpv_interpolation", false); }
    
    public static int getSoftTuneMode() { return Prefers.getInt("perf_mpv_soft_tune", 0); }
    public static String getSoftTuneOptionText() { return "off"; }
    
    public static int getRebufferMs() { return Prefers.getInt("perf_mpv_rebuffer_ms", 500); }
    
    public static int getHlsBitrateOption() { return Prefers.getInt("perf_mpv_hls_bitrate", 0); }
    public static String getHlsBitrateOptionText() { return "highest"; }
    
    public static int getVulkanBackendOption() { return Prefers.getInt("perf_mpv_vulkan_backend", VULKAN_BACKEND_AUTO); }
    public static String getVulkanBackendOptionText() { return "auto"; }
    
    public static boolean isPerformancePriority() { return Prefers.getBoolean("perf_mpv_option_priority", false); }
    public static boolean isMultichannelPcm() { return Prefers.getBoolean("perf_mpv_multichannel_pcm", false); }
}
