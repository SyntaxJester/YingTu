package com.yingtu.plus.util;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.yingtu.plus.App;

public class Prefers {
    public static SharedPreferences getPrefers() {
        return PreferenceManager.getDefaultSharedPreferences(App.context());
    }
    
    public static String getString(String key) { return getString(key, ""); }
    public static String getString(String key, String defaultValue) {
        try { return getPrefers().getString(key, defaultValue); }
        catch (Exception e) { return defaultValue; }
    }
    
    public static int getInt(String key) { return getInt(key, 0); }
    public static int getInt(String key, int defaultValue) {
        try { return getPrefers().getInt(key, defaultValue); }
        catch (Exception e) { return defaultValue; }
    }
    
    public static long getLong(String key) { return getLong(key, 0L); }
    public static long getLong(String key, long defaultValue) {
        try { return getPrefers().getLong(key, defaultValue); }
        catch (Exception e) { return defaultValue; }
    }
    
    public static float getFloat(String key) { return getFloat(key, 0f); }
    public static float getFloat(String key, float defaultValue) {
        try { return getPrefers().getFloat(key, defaultValue); }
        catch (Exception e) { return defaultValue; }
    }
    
    public static boolean getBoolean(String key) { return getBoolean(key, false); }
    public static boolean getBoolean(String key, boolean defaultValue) {
        try { return getPrefers().getBoolean(key, defaultValue); }
        catch (Exception e) { return defaultValue; }
    }
    
    public static double getDouble(String key, double defaultValue) {
        return (double) getInt(key, (int)(defaultValue * 100)) / 100.0;
    }
    
    public static void put(String key, String value) {
        try { getPrefers().edit().putString(key, value).apply(); }
        catch (Exception ignored) {}
    }
    
    public static void put(String key, int value) {
        try { getPrefers().edit().putInt(key, value).apply(); }
        catch (Exception ignored) {}
    }
    
    public static void put(String key, long value) {
        try { getPrefers().edit().putLong(key, value).apply(); }
        catch (Exception ignored) {}
    }
    
    public static void put(String key, float value) {
        try { getPrefers().edit().putFloat(key, value).apply(); }
        catch (Exception ignored) {}
    }
    
    public static void put(String key, boolean value) {
        try { getPrefers().edit().putBoolean(key, value).apply(); }
        catch (Exception ignored) {}
    }
    
    public static void remove(String key) {
        try { getPrefers().edit().remove(key).apply(); }
        catch (Exception ignored) {}
    }
}
