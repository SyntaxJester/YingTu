package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class LiveSetting {
    private static final int LIST_STYLE_GLASS = 0;
    private static final int LIST_STYLE_CLASSIC = 1;

    public static boolean isBoot() { return Prefers.getBoolean("boot_live"); }
    public static void putBoot(boolean boot) { Prefers.put("boot_live", boot); }
    public static boolean isAcross() { return Prefers.getBoolean("across", true); }
    public static void putAcross(boolean v) { Prefers.put("across", v); }
    public static boolean isChange() { return Prefers.getBoolean("change", true); }
    public static void putChange(boolean v) { Prefers.put("change", v); }
    public static boolean isInvert() { return Prefers.getBoolean("invert"); }
    public static void putInvert(boolean v) { Prefers.put("invert", v); }
    public static int getScale() { return Prefers.getInt("scale_live", PlayerSetting.getScale()); }
    public static void putScale(int s) { Prefers.put("scale_live", s); }
    public static boolean isListStyleClassic() { return getListStyle() == LIST_STYLE_CLASSIC; }
    public static void putListStyle(int style) { Prefers.put("live_list_style", style == LIST_STYLE_CLASSIC ? LIST_STYLE_CLASSIC : LIST_STYLE_GLASS); }
    public static void toggleListStyle() { putListStyle(isListStyleClassic() ? LIST_STYLE_GLASS : LIST_STYLE_CLASSIC); }
    public static int getListStyle() { int s = Prefers.getInt("live_list_style", LIST_STYLE_CLASSIC); return s == LIST_STYLE_CLASSIC ? LIST_STYLE_CLASSIC : LIST_STYLE_GLASS; }
}
