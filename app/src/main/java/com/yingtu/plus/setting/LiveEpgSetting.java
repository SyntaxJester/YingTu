package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;
import java.util.List;
import java.util.ArrayList;

public class LiveEpgSetting {
    private static final String KEY_URL = "live_epg_url";
    private static final String KEY_HISTORY = "live_epg_history";
    private static final int MAX_HISTORY = 20;

    public static String getUrl() { return Prefers.getString(KEY_URL, ""); }
    public static void putUrl(String url) { Prefers.put(KEY_URL, url); }
    
    public static List<String> getHistory() {
        String s = Prefers.getString(KEY_HISTORY, null);
        if (s == null) return new ArrayList<>();
        return new ArrayList<>(List.of(s.split("\n")));
    }
    
    public static void addHistory(String url) {
        List<String> list = getHistory();
        list.remove(url);
        list.add(0, url);
        while (list.size() > MAX_HISTORY) list.remove(list.size() - 1);
        Prefers.put(KEY_HISTORY, String.join("\n", list));
    }
    
    public static void removeHistory(String url) {
        List<String> list = getHistory();
        list.remove(url);
        Prefers.put(KEY_HISTORY, String.join("\n", list));
    }
    
    public static void clearHistory() { Prefers.remove(KEY_HISTORY); }
    
    public static List<String> getXmlUrls(Object live) { 
        return new ArrayList<>(); 
    }
    
    public static String getEffectiveUrl(Object live) { 
        return getUrl(); 
    }
    
    public static void apply(Object live) {}
    public static void apply(Object live, Object channel) {}
}
