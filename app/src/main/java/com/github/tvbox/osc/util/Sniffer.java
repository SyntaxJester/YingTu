package com.github.tvbox.osc.util;

import android.net.Uri;
import android.text.TextUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 直链嗅探工具（参考 FongMi/webhtv 的 Sniffer 规则精简而来）。
 *
 * 用于判断某个 URL 是否已经是可直接播放的视频直链，
 * 以及在 WebView 嗅探时拦截网络请求判断是否命中视频资源。
 */
public class Sniffer {

    /** 常见视频直链特征 */
    public static final Pattern SNIFFER = Pattern.compile(
            "https?://[^\\s]{12,}\\.(?:m3u8|mp4|mkv|flv|mp3|m4a|aac|mpd|ts|avi|wmv|rmvb|mov)(?:\\?.*)?"
                    + "|https?://.*?video/tos[^\\s]*"
                    + "|rtmp:[^\\s]+");

    /** WebView 拦截时用于初步识别播放器请求 */
    public static final Pattern PLAYER = Pattern.compile("player.*https?://");

    private Sniffer() {}

    /**
     * 判断是否为可直接播放的视频直链。
     */
    public static boolean isVideoFormat(String url) {
        if (TextUtils.isEmpty(url)) return false;
        String lower = url.toLowerCase(Locale.US);
        // 明显是网页 / 跳转参数的排除
        if (lower.contains("url=http") || lower.contains("v=http") || lower.endsWith(".html")) {
            return false;
        }
        return SNIFFER.matcher(url).find();
    }

    /**
     * 粗略判断一个播放地址是否需要二次解析。
     *
     * 规则：
     *   - 空地址 → 不需要（无法处理）
     *   - 直链（isVideoFormat）→ 不需要
     *   - 以 http(s) 开头的网页地址 → 需要
     *   - 其它（magnet/ed2k/自定义协议）→ 不需要（交给播放器/外部处理）
     */
    public static boolean needParse(String url) {
        if (TextUtils.isEmpty(url)) return false;
        if (isVideoFormat(url)) return false;
        String lower = url.toLowerCase(Locale.US);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    public static String host(String url) {
        try {
            return Uri.parse(url).getHost();
        } catch (Exception e) {
            return "";
        }
    }
}
