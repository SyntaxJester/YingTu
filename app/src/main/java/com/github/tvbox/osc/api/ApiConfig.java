package com.github.tvbox.osc.api;

import android.content.Context;
import android.text.TextUtils;

import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.util.HawkConfig;
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
                sb.setExt(obj.has("ext") && obj.get("ext").isJsonPrimitive()
                        ? obj.get("ext").getAsString() : "");
                sb.setJar(optString(obj, "jar"));
                sourceBeanList.put(sb.getKey(), sb);
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

    private static String optString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
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
