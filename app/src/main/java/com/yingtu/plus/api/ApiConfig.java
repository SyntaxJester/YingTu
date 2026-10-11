package com.yingtu.plus.api;

import android.content.Context;
import android.text.TextUtils;

import com.yingtu.plus.bean.LiveChannel;
import com.yingtu.plus.bean.LiveGroup;
import com.yingtu.plus.bean.ParseBean;
import com.yingtu.plus.bean.SnifferRule;
import com.yingtu.plus.bean.SourceBean;
import com.yingtu.plus.util.HawkConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orhanobut.hawk.Hawk;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频源配置管理器（单例）。
 *
 * 负责从远程/本地 TVBox 订阅地址拉取 JSON，解析出 sites 列表，
 * 并维护「当前选中源」。真实的 TVBox 配置格式兼容。
 */
public class ApiConfig {

    private static ApiConfig instance;

    private final LinkedHashMap<String, SourceBean> sourceBeanList = new LinkedHashMap<>();
    private final List<ParseBean> parseBeanList = new ArrayList<>();
    private final List<SnifferRule> snifferRules = new ArrayList<>();
    private final List<String> liveUrls = new ArrayList<>();
    private ParseBean mDefaultParse;
    private SourceBean mHomeSource;
    private String spider = "";

    private ApiConfig() {}

    public static ApiConfig get() {
        if (instance == null) {
            synchronized (ApiConfig.class) {
                if (instance == null) instance = new ApiConfig();
            }
        }
        return instance;
    }

    public void init(Context context) {
        // 启动时若本地已缓存订阅内容则直接解析
        String cached = Hawk.get(HawkConfig.API_CONFIG_CACHE, "");
        if (!TextUtils.isEmpty(cached)) {
            try {
                parseConfig(cached);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * 解析 TVBox 风格的订阅 JSON。
     */
    public void parseConfig(String json) {
        sourceBeanList.clear();
        parseBeanList.clear();
        snifferRules.clear();
        liveUrls.clear();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        if (root.has("spider")) {
            spider = root.get("spider").getAsString();
        }

        if (root.has("sites")) {
            JsonArray sites = root.getAsJsonArray("sites");
            for (JsonElement e : sites) {
                JsonObject obj = e.getAsJsonObject();
                SourceBean sb = new SourceBean();
                sb.setKey(optString(obj, "key"));
                sb.setName(optString(obj, "name"));
                sb.setType(optInt(obj, "type"));
                sb.setApi(optString(obj, "api"));
                sb.setSearchable(optInt(obj, "searchable", 1));
                sb.setQuickSearch(optInt(obj, "quickSearch", 1));
                sb.setFilterable(optInt(obj, "filterable", 1));
                sb.setExt(parseExt(obj));
                sb.setJar(optString(obj, "jar"));
                sb.setPlayerUrl(optString(obj, "playUrl"));
                sourceBeanList.put(sb.getKey(), sb);
            }
        }

        // 解析 parses（二次解析接口列表）
        if (root.has("parses")) {
            JsonArray parses = root.getAsJsonArray("parses");
            for (JsonElement e : parses) {
                JsonObject obj = e.getAsJsonObject();
                ParseBean pb = new ParseBean();
                pb.setName(optString(obj, "name"));
                pb.setType(optInt(obj, "type"));
                pb.setUrl(optString(obj, "url"));
                parseBeanList.add(pb);
            }
        }
        // 还原上次选中的解析器，否则取第一个
        String lastParse = Hawk.get(HawkConfig.DEFAULT_PARSE, "");
        mDefaultParse = null;
        for (ParseBean pb : parseBeanList) {
            if (pb.getName().equals(lastParse)) {
                mDefaultParse = pb;
                break;
            }
        }
        if (mDefaultParse == null && !parseBeanList.isEmpty()) {
            mDefaultParse = parseBeanList.get(0);
        }
        if (mDefaultParse != null) mDefaultParse.setSelected(true);

        // 解析 rules（按域名的嗅探规则）
        if (root.has("rules")) {
            try {
                for (JsonElement e : root.getAsJsonArray("rules")) {
                    JsonObject obj = e.getAsJsonObject();
                    SnifferRule rule = new SnifferRule();
                    rule.name = optString(obj, "name");
                    rule.hosts = optStringList(obj, "hosts", "host");
                    rule.regex = optStringList(obj, "regex", null);
                    rule.script = optStringList(obj, "script", null);
                    rule.exclude = optStringList(obj, "exclude", null);
                    if (!rule.hosts.isEmpty()) snifferRules.add(rule);
                }
            } catch (Throwable ignored) {
            }
        }

        // 解析 lives（直播源地址）
        if (root.has("lives")) {
            try {
                for (JsonElement e : root.getAsJsonArray("lives")) {
                    if (e.isJsonObject()) {
                        JsonObject obj = e.getAsJsonObject();
                        // 兼容 url 直接给 / channels 内嵌等，这里取顶层 url
                        String url = optString(obj, "url");
                        if (TextUtils.isEmpty(url)) {
                            // group 内嵌 channels：收集每个 channel 的第一条 url 不现实，优先取可直接解析的 url 字段
                            url = optString(obj, "api");
                        }
                        if (!TextUtils.isEmpty(url)) liveUrls.add(url);
                    } else if (e.isJsonPrimitive()) {
                        liveUrls.add(e.getAsString());
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // 还原上次选中的源，否则取第一个
        String lastKey = Hawk.get(HawkConfig.HOME_API, "");
        if (!TextUtils.isEmpty(lastKey) && sourceBeanList.containsKey(lastKey)) {
            mHomeSource = sourceBeanList.get(lastKey);
        } else if (!sourceBeanList.isEmpty()) {
            mHomeSource = sourceBeanList.values().iterator().next();
        }
    }

    public List<SourceBean> getSourceBeanList() {
        return new ArrayList<>(sourceBeanList.values());
    }

    public SourceBean getSource(String key) {
        return sourceBeanList.get(key);
    }

    public SourceBean getHomeSource() {
        return mHomeSource;
    }

    public void setSourceBean(SourceBean sourceBean) {
        this.mHomeSource = sourceBean;
        if (sourceBean != null) {
            Hawk.put(HawkConfig.HOME_API, sourceBean.getKey());
        }
    }

    public String getSpider() {
        return spider;
    }

    public List<ParseBean> getParseBeanList() {
        return parseBeanList;
    }

    public ParseBean getDefaultParse() {
        return mDefaultParse;
    }

    public void setDefaultParse(ParseBean parse) {
        for (ParseBean pb : parseBeanList) pb.setSelected(pb == parse);
        this.mDefaultParse = parse;
        if (parse != null) Hawk.put(HawkConfig.DEFAULT_PARSE, parse.getName());
    }

    public List<SnifferRule> getSnifferRules() {
        return snifferRules;
    }

    public List<String> getLiveUrls() {
        return liveUrls;
    }

    private static List<String> optStringList(JsonObject obj, String key, String altKey) {
        List<String> list = new ArrayList<>();
        String useKey = obj.has(key) ? key : (altKey != null && obj.has(altKey) ? altKey : null);
        if (useKey == null) return list;
        JsonElement el = obj.get(useKey);
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (!e.isJsonNull()) list.add(e.getAsString());
            }
        } else if (el.isJsonPrimitive()) {
            list.add(el.getAsString());
        }
        return list;
    }

    private static String optString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    /** ext 可能是字符串（URL/base64）或对象（如 {"config":"AppV7Dsx"}），统一转成字符串给 Spider.init。 */
    private static String parseExt(JsonObject obj) {
        if (!obj.has("ext") || obj.get("ext").isJsonNull()) return "";
        JsonElement ext = obj.get("ext");
        if (ext.isJsonPrimitive()) return ext.getAsString();
        return ext.toString(); // 对象/数组原样序列化
    }

    private static int optInt(JsonObject obj, String key) {
        return optInt(obj, key, 0);
    }

    private static int optInt(JsonObject obj, String key, int def) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : def;
        } catch (Exception e) {
            return def;
        }
    }
}
