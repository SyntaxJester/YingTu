package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class LyricsSetting {
    public static final int SOURCE_AUTO = 0;
    public static final int SOURCE_LOCAL = 1;
    public static final int SOURCE_TTML = 2;
    public static final int SOURCE_QQ = 3;
    public static final int SOURCE_NETEASE = 4;
    public static final int SOURCE_KUWO = 5;
    public static final int SOURCE_LRCLIB = 6;
    public static final int SOURCE_KUGOU = 7;
    public static final int SOURCE_MIGU = 8;

    public static int getSourceMode() { return Prefers.getInt("lyrics_source_mode", SOURCE_AUTO); }
    public static void putSourceMode(int mode) { Prefers.put("lyrics_source_mode", mode); }
    public static int sanitizeSourceMode(int mode) { return Math.max(SOURCE_AUTO, Math.min(mode, SOURCE_MIGU)); }
    public static String cacheSuffix(int mode) {
        if (mode == SOURCE_LOCAL) return "_local";
        if (mode == SOURCE_TTML) return "_ttml";
        return "";
    }
}
