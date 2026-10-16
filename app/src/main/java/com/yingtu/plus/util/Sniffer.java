package com.yingtu.plus.util;

import android.net.Uri;
import android.text.TextUtils;

import com.yingtu.plus.api.ApiConfig;
import com.yingtu.plus.bean.SnifferRule;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 直链嗅探工具（参考 FongMi/webhtv 的 Sniffer 规则精简而来）。
 *
 * 用于判断某个 URL 是否已经是可直接播放的视频直链，
 * 以及在 WebView 嗅探时拦截网络请求判断是否命中视频资源。
 *
 * 支持按域名自定义规则（SnifferRule）：命中某条规则的 host 后，
 * 优先用该规则的 exclude/regex 判定，再回退到通用正则。
 */
public class Sniffer {

    /** 常见视频直链特征（通用兜底规则） */
    public static final Pattern SNIFFER = Pattern.compile(
            "https?://[^\\s]{12,}\\.(?:m3u8|mp4|mkv|flv|mp3|m4a|aac|mpd|ts|avi|wmv|rmvb|mov)(?:\\?.*)?"
                    + "|https?://.*?video/tos[^\\s]*"
                    + "|rtmp:[^\\s]+");

    /** WebView 拦截时用于初步识别播放器请求 */
    public static final Pattern PLAYER = Pattern.compile("player.*https?://");

    private Sniffer() {}

    /**
     * 判断是否为可直接播放的视频直链。
     * 先按命中的域名规则判定，再回退到通用正则。
     */
    public static boolean isVideoFormat(String url) {
        if (TextUtils.isEmpty(url)) return false;

        SnifferRule rule = getRule(url);
        if (rule != null) {
            // exclude 命中则排除
            for (String ex : rule.exclude) {
                if (url.contains(ex)) return false;
                try {
                    if (Pattern.compile(ex).matcher(url).find()) return false;
                } catch (Exception ignored) {
                }
            }
            // regex 命中则确认是直链
            for (String rx : rule.regex) {
                if (url.contains(rx)) return true;
                try {
                    if (Pattern.compile(rx).matcher(url).find()) return true;
                } catch (Exception ignored) {
                }
            }
        }

        String lower = url.toLowerCase(Locale.US);
        if (lower.contains("url=http") || lower.contains("v=http") || lower.endsWith(".html")) {
            return false;
        }
        return SNIFFER.matcher(url).find();
    }

    /**
     * 粗略判断一个播放地址是否需要二次解析。
     */
    public static boolean needParse(String url) {
        if (TextUtils.isEmpty(url)) return false;
        if (isVideoFormat(url)) return false;
        String lower = url.toLowerCase(Locale.US);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    /** 命中某条域名规则时返回该规则的注入脚本列表 */
    public static List<String> getScript(String url) {
        SnifferRule rule = getRule(url);
        return rule == null ? java.util.Collections.emptyList() : rule.script;
    }

    /** 按 URL 的 host 查找匹配的域名规则 */
    public static SnifferRule getRule(String url) {
        String host = host(url);
        if (TextUtils.isEmpty(host)) return null;
        for (SnifferRule rule : ApiConfig.get().getSnifferRules()) {
            for (String h : rule.hosts) {
                if (TextUtils.isEmpty(h)) continue;
                if (host.contains(h)) return rule;
                try {
                    if (Pattern.compile(h).matcher(host).find()) return rule;
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    public static String host(String url) {
        try {
            return Uri.parse(url).getHost();
        } catch (Exception e) {
            return "";
        }
    }
}
