package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

public class SiteHealthStore {
    private static final String KEY = "site_health";
    
    public static void recordSearch(Object site, boolean success, int count, long cost, String error) {}
    public static void recordPlay(Object site, boolean success, long cost, String error) {}
    public static void recordDetail(Object site, boolean success, int count, long cost, String error) {}
    
    public static void flush() {}
    
    public static List<Object> sortSites(List<Object> sites) {
        return new ArrayList<>(sites);
    }
}
