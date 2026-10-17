package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class BackgroundPlaybackPolicy {
    public static final int MODE_OFF = 0;
    public static final int MODE_ON = 1;
    
    public static int getMode() { return Prefers.getInt("bg_play_mode", MODE_OFF); }
    public static boolean isEnabled() { return getMode() == MODE_ON; }
    public static void putEnabled(boolean v) { Prefers.put("bg_play_mode", v ? MODE_ON : MODE_OFF); }
}
