package com.yingtu.plus.util;

import android.content.Context;
import android.net.Uri;

import com.orhanobut.hawk.Hawk;

/**
 * 壁纸管理工具：把用户选择的图片 URI 存到 Hawk，
 * 各 Activity/Fragment 调用 getWallpaper() 判断是否要应用。
 */
public final class WallpaperHelper {

    /** 用户选定的壁纸 URI 字符串（null/empty=默认） */
    public static final String KEY = "custom_wallpaper";

    private WallpaperHelper() {}

    /** 设置自定义壁纸 */
    public static void setWallpaper(Context ctx, Uri uri) {
        if (uri == null) return;
        Hawk.put(KEY, uri.toString());
    }

    /** 恢复默认 */
    public static void resetWallpaper(Context ctx) {
        Hawk.put(KEY, "");
    }

    /** 返回当前自定义壁纸 URI；null=使用默认渐变 */
    public static Uri getWallpaperUri(Context ctx) {
        String s = Hawk.get(KEY, "");
        if (s.isEmpty()) return null;
        return Uri.parse(s);
    }

    /** 是否有自定义壁纸 */
    public static boolean hasCustomWallpaper(Context ctx) {
        return getWallpaperUri(ctx) != null;
    }
}
