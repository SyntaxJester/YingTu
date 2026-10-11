package com.yingtu.plus.api;

import android.text.TextUtils;

import com.yingtu.plus.bean.Movie;
import com.yingtu.plus.bean.MovieSort;
import com.yingtu.plus.bean.SourceBean;
import com.yingtu.plus.bean.VodInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 标准 TVBox JSON 源（type=1）解析器。
 *
 * 覆盖四类接口：
 *  - 分类 + 首页推荐  : ?ac=class
 *  - 分类列表翻页      : ?ac=detail&t=分类&pg=页码
 *  - 详情（线路/选集）: ?ac=detail&ids=影片id
 *  - 搜索              : ?wd=关键字
 *
 * 真正网络请求交由 {@link SourceViewModel} 执行，这里只负责纯解析，便于单测。
 */
public class JsonParserHelper {

    /** 解析首页分类标签 */
    public static List<MovieSort> parseSortList(String json) {
        List<MovieSort> list = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("class")) {
                for (JsonElement e : root.getAsJsonArray("class")) {
                    JsonObject o = e.getAsJsonObject();
                    list.add(new MovieSort(
                            getString(o, "type_id"),
                            getString(o, "type_name")));
                }
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    /** 解析影片列表（分类页 / 搜索页通用） */
    public static List<Movie> parseMovieList(String json, String sourceKey) {
        List<Movie> list = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray arr = null;
            if (root.has("list")) arr = root.getAsJsonArray("list");
            if (arr == null) return list;
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                Movie m = new Movie();
                m.vodId = getString(o, "vod_id");
                m.name = getString(o, "vod_name");
                m.pic = getString(o, "vod_pic");
                m.note = getString(o, "vod_remarks");
                m.year = getString(o, "vod_year");
                m.sourceKey = sourceKey;
                // 评分：优先 vod_douban_score，其次 vod_score
                String rate = getString(o, "vod_douban_score");
                if (TextUtils.isEmpty(rate)) rate = getString(o, "vod_score");
                if (!TextUtils.isEmpty(rate) && !rate.equals("0") && !rate.equals("0.0")) {
                    m.rate = rate;
                }
                list.add(m);
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    /** 解析详情（含线路与选集） */
    public static VodInfo parseVodInfo(String json, String sourceKey) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("list");
            if (arr == null || arr.size() == 0) return null;
            JsonObject o = arr.get(0).getAsJsonObject();

            VodInfo info = new VodInfo();
            info.id = getString(o, "vod_id");
            info.name = getString(o, "vod_name");
            info.pic = getString(o, "vod_pic");
            info.note = getString(o, "vod_remarks");
            info.year = getString(o, "vod_year");
            info.area = getString(o, "vod_area");
            info.type = getString(o, "vod_class");
            info.actor = getString(o, "vod_actor");
            info.director = getString(o, "vod_director");
            info.des = stripHtml(getString(o, "vod_content"));
            info.sourceKey = sourceKey;

            String playFrom = getString(o, "vod_play_from");
            String playUrl = getString(o, "vod_play_url");
            if (!TextUtils.isEmpty(playFrom) && !TextUtils.isEmpty(playUrl)) {
                String[] flags = playFrom.split("\\$\\$\\$");
                String[] urlGroups = playUrl.split("\\$\\$\\$");
                for (int i = 0; i < flags.length && i < urlGroups.length; i++) {
                    VodInfo.VodSeriesFlag flag = new VodInfo.VodSeriesFlag(flags[i]);
                    for (String seg : urlGroups[i].split("#")) {
                        String[] kv = seg.split("\\$", 2);
                        String name = kv.length > 1 ? kv[0] : "正片";
                        String url = kv.length > 1 ? kv[1] : kv[0];
                        if (!TextUtils.isEmpty(url)) {
                            flag.list.add(new VodInfo.VodSeries(name, url));
                        }
                    }
                    if (!flag.list.isEmpty()) info.seriesFlags.add(flag);
                }
            }
            if (!info.seriesFlags.isEmpty()) {
                info.seriesFlags.get(0).selected = true;
            }
            return info;
        } catch (Throwable e) {
            return null;
        }
    }

    private static String getString(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static String stripHtml(String s) {
        if (TextUtils.isEmpty(s)) return "";
        return s.replaceAll("<[^>]+>", "").trim();
    }
}
