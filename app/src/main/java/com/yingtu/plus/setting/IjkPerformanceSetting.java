package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class IjkPerformanceSetting {
    public static final int SCENE_AUTO = 0;
    public static final int SCENE_VOD = 1;
    public static final int WATER_LOW = 0;
    public static final int WATER_STANDARD = 1;
    public static final int DROP_STANDARD = 1;

    public static int getScene() { return Prefers.getInt("perf_ijk_scene", SCENE_AUTO); }
    public static void putScene(int v) { Prefers.put("perf_ijk_scene", v); }
    public static String getSceneText() { return "auto"; }
    
    public static int getBufferMb() { return Prefers.getInt("perf_ijk_buffer_mb", 3); }
    public static void putBufferMb(int v) { Prefers.put("perf_ijk_buffer_mb", v); }
    
    public static boolean isPacketBuffering() { return Prefers.getBoolean("perf_ijk_packet_buffering", false); }
    public static void putPacketBuffering(boolean v) { Prefers.put("perf_ijk_packet_buffering", v); }
    
    public static int getWaterLevel() { return Prefers.getInt("perf_ijk_water", WATER_STANDARD); }
    public static int getFirstWaterMs() { return Prefers.getInt("perf_ijk_first_water", 200); }
    public static int getNextWaterMs() { return Prefers.getInt("perf_ijk_next_water", 1000); }
    public static int getLastWaterMs() { return Prefers.getInt("perf_ijk_last_water", 8000); }
    
    public static int getPictureQueue() { return Prefers.getInt("perf_ijk_picture_queue", 1); }
    public static int getFrameDropValue() { return Prefers.getInt("perf_ijk_drop", DROP_STANDARD); }
    
    public static int getProbeMode() { return Prefers.getInt("perf_ijk_probe", 0); }
    public static boolean isAccurateSeek() { return Prefers.getBoolean("perf_ijk_accurate_seek", true); }
    
    public static int getSoftTuneMode() { return Prefers.getInt("perf_ijk_soft_tune", 0); }
    public static int getRtspTransport() { return Prefers.getInt("perf_ijk_rtsp_transport", 0); }
    public static boolean isReconnect() { return Prefers.getBoolean("perf_ijk_reconnect", false); }
}
