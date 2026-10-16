package com.yingtu.plus.api;

import android.text.TextUtils;

import com.yingtu.plus.bean.LiveChannel;
import com.yingtu.plus.bean.LiveGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 直播源解析器（参考 FongMi/webhtv 的 LiveParser 精简）。
 *
 * 同时支持两种常见格式：
 *   1. M3U(#EXTM3U / #EXTINF)：带 group-title / tvg-logo / http-user-agent
 *   2. TXT（diyp/txt 格式）：使用 "分组名,#genre#" 分组，"频道名,地址#地址2" 行
 *
 * 同名频道的多个地址会合并为多线路。
 */
public class LiveParser {

    private static final Pattern M3U_HEADER = Pattern.compile("#EXTM3U", Pattern.MULTILINE);
    private static final Pattern GROUP = Pattern.compile(".*group-title=\"(.*?)\".*");
    private static final Pattern LOGO = Pattern.compile(".*tvg-logo=\"(.*?)\".*");
    private static final Pattern UA = Pattern.compile(".*http-user-agent=\"(.*?)\".*");
    private static final Pattern NAME = Pattern.compile(".*,(.+?)$");

    private LiveParser() {}

    public static List<LiveGroup> parse(String text) {
        if (TextUtils.isEmpty(text)) return new ArrayList<>();
        text = text.replace("\r\n", "\n").replace("\r", "");
        if (M3U_HEADER.matcher(text).find()) {
            return m3u(text);
        }
        return txt(text);
    }

    private static List<LiveGroup> m3u(String text) {
        List<LiveGroup> groups = new ArrayList<>();
        LiveGroup defaultGroup = new LiveGroup("默认分组");

        String pendingGroup = null;
        String pendingName = null;
        String pendingLogo = null;
        String pendingUa = null;

        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#EXTINF:")) {
                pendingName = extract(line, NAME);
                pendingGroup = extract(line, GROUP);
                pendingLogo = extract(line, LOGO);
                pendingUa = extract(line, UA);
            } else if (!line.startsWith("#")) {
                // 播放地址行
                if (TextUtils.isEmpty(pendingName)) continue;
                String groupName = TextUtils.isEmpty(pendingGroup) ? "默认分组" : pendingGroup;
                LiveGroup group = findGroup(groups, groupName, defaultGroup);
                LiveChannel channel = group.findOrCreate(pendingName);
                channel.groupName = groupName;
                if (!TextUtils.isEmpty(pendingLogo)) channel.logo = pendingLogo;
                if (!TextUtils.isEmpty(pendingUa)) channel.ua = pendingUa;
                // 支持 "url|header" 形式，取 url 部分
                String url = line.split("\\|", 2)[0].trim();
                if (url.contains("://")) channel.urls.add(url);
                pendingName = null;
            }
        }
        if (!defaultGroup.channels.isEmpty() && !groups.contains(defaultGroup)) {
            groups.add(defaultGroup);
        }
        return groups;
    }

    private static List<LiveGroup> txt(String text) {
        List<LiveGroup> groups = new ArrayList<>();
        LiveGroup current = null;
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] split = line.split(",", 2);
            if (line.contains("#genre#")) {
                current = new LiveGroup(split[0].trim());
                groups.add(current);
                continue;
            }
            if (split.length < 2) continue;
            if (current == null) {
                current = new LiveGroup("默认分组");
                groups.add(current);
            }
            String channelName = split[0].trim();
            LiveChannel channel = current.findOrCreate(channelName);
            channel.groupName = current.name;
            for (String seg : split[1].split("#")) {
                String url = seg.split("\\|", 2)[0].trim();
                if (url.contains("://")) channel.urls.add(url);
            }
        }
        return groups;
    }

    private static LiveGroup findGroup(List<LiveGroup> groups, String name, LiveGroup fallback) {
        for (LiveGroup g : groups) {
            if (g.name != null && g.name.equals(name)) return g;
        }
        if (name.equals("默认分组")) {
            if (!groups.contains(fallback)) groups.add(fallback);
            return fallback;
        }
        LiveGroup g = new LiveGroup(name);
        groups.add(g);
        return g;
    }

    private static String extract(String line, Pattern pattern) {
        Matcher m = pattern.matcher(line);
        if (m.matches() && m.group(1) != null) return m.group(1).trim();
        return "";
    }
}
