package com.yingtu.plus.com.yingtu.plus.util;

import android.content.Context;
import android.net.Uri;

import com.orhanobut.hawk.Hawk;

/**
 * 注入地址（自定义直播源）管理。
 * Hawk key: INJECT_URL。
 */
public final class InjectHelper {

    public static final String KEY = "inject_url";

    private InjectHelper() {}

    public static void setUrl(Context ctx, String url) {
        Hawk.put(KEY, url == null ? "" : url.trim());
    }

    /** 返回已保存的注入地址；null/empty = 未设置 */
    public static String getUrl(Context ctx) {
        return Hawk.get(KEY, "");
    }

    public static boolean hasCustom(Context ctx) {
        String s = getUrl(ctx);
        return s != null && !s.isEmpty();
    }
}
