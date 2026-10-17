package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;
import java.util.List;
import java.util.ArrayList;

public class Setting {
    // Constants
    public static final int LANGUAGE_FOLLOW_SYSTEM = 0;
    public static final int LANGUAGE_SIMPLIFIED = 1;
    public static final int CSP_WARMUP_DISABLED = 0;
    public static final int CSP_WARMUP_DEFAULT = 1;
    public static final int CSP_WARMUP_CUSTOM = 2;
    public static final int UI_SCALE_FOLLOW_SYSTEM = 0;
    public static final int UI_SCALE_STANDARD = 1;
    public static final int UI_SCALE_COMPACT = 2;
    
    public static void putWall(int wall) { Prefers.put("wall", wall); }
    public static void putWallType(int type) { Prefers.put("wall_type", type); }
    public static int getCspWarmupMode() { return Prefers.getInt("csp_warmup_mode", CSP_WARMUP_DEFAULT); }
    public static List<String> getCspWarmupSites() { return new ArrayList<>(); }
    public static boolean isCspWarmup() { return getCspWarmupMode() != CSP_WARMUP_DISABLED; }
    public static boolean isCompactEpisodeTitle() { return Prefers.getBoolean("compact_episode_title", false); }
    public static void putCompactEpisodeTitle(boolean v) { Prefers.put("compact_episode_title", v); }
    public static String getUa() { return Prefers.getString("ua", ""); }
    public static boolean isAdblock() { return Prefers.getBoolean("adblock", false); }
    public static boolean isIncognito() { return Prefers.getBoolean("incognito", false); }
    public static boolean isPlaybackArtworkWall() { return Prefers.getBoolean("playback_artwork_wall", false); }
    public static boolean hasFileAccess() { return true; }
    public static boolean hasFileManager() { return true; }
    
    public static Object create() { return null; }
    public static void putReset(String key) { Prefers.remove(key); }
    public static void putLoginStateFindings(Object findings) {}
    public static void putLoginStatePaths(Object paths) {}
    public static void putLoginStatePendingPaths(Object paths) {}
    public static void putLoginStateSnapshot(Object snapshot) {}
    public static List<Object> getLoginStateFindings() { return new ArrayList<>(); }
    public static List<Object> getLoginStatePaths() { return new ArrayList<>(); }
    public static List<Object> getLoginStatePendingPaths() { return new ArrayList<>(); }
    public static Object getLoginStateSnapshot() { return null; }
    public static List<String> getSyncPaths() { return new ArrayList<>(); }
}
