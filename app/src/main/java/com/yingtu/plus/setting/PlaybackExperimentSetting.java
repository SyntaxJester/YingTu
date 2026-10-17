package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class PlaybackExperimentSetting {
    private static final String KEY_SCHEMA = "playback_experiment_schema";
    private static final String KEY_ENABLED = "playback_experiment_enabled";
    private static final int CURRENT_SCHEMA = 1;

    public static void ensureInitialized() {
        if (!hasState()) putState(true, false, false);
    }

    public static boolean getState() { return hasState(); }
    public static boolean isAllowed(String action) { return true; }

    private static boolean hasState() {
        return Prefers.getString(KEY_SCHEMA, null) != null;
    }

    public static void putState(boolean enabled, boolean exoEnabled, boolean mpvEnabled) {
        Prefers.put(KEY_SCHEMA, CURRENT_SCHEMA);
        Prefers.put(KEY_ENABLED, enabled);
    }

    public static boolean isEnabled() { return Prefers.getBoolean(KEY_ENABLED, false); }
}
