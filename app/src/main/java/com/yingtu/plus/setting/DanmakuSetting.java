package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;
import java.util.List;
import java.util.ArrayList;

public class DanmakuSetting {
    public static boolean isLoad() { return Prefers.getBoolean("danmaku_load"); }
    public static void putLoad(boolean v) { Prefers.put("danmaku_load", v); }
    public static boolean isAuto() { return Prefers.getBoolean("danmaku_auto"); }
    public static void putAuto(boolean v) { Prefers.put("danmaku_auto", v); }
    public static boolean isSpiderFirst() { return Prefers.getBoolean("danmaku_spider_first"); }
    public static void putSpiderFirst(boolean v) { Prefers.put("danmaku_spider_first", v); }
    public static boolean isShow() { return Prefers.getBoolean("danmaku_show"); }
    public static void putShow(boolean v) { Prefers.put("danmaku_show", v); }
    public static String getApiUrl() { return Prefers.getString("danmaku_api_url", ""); }
    public static void putApiUrl(String url) { Prefers.put("danmaku_api_url", url == null ? "" : url.trim()); }
    public static float getTextScale() { return Prefers.getFloat("danmaku_text_scale", 1f); }
    public static void putTextScale(float v) { Prefers.put("danmaku_text_scale", v); }
    
    // Methods called by app code
    public static boolean hasValidApiUrl() { return !getApiUrl().isEmpty(); }
    public static String getValidApiUrl() { return hasValidApiUrl() ? getApiUrl() : ""; }
    public static Object getConfig() { return null; }
}
