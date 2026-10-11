package com.github.tvbox.osc.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频源站点配置。对应 TVBox 配置里 sites 数组的单个条目。
 */
public class SourceBean {
    private String key;
    private String name;
    private int type;           // 0=xml, 1=json, 3=drpy/js
    private String api;
    private int searchable;     // 是否可搜索
    private int quickSearch;    // 是否支持快速搜索
    private int filterable;     // 是否支持筛选
    private String ext;         // 扩展参数
    private String jar;         // spider jar
    private String playerUrl;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public String getApi() { return api; }
    public void setApi(String api) { this.api = api; }

    public int getSearchable() { return searchable; }
    public void setSearchable(int searchable) { this.searchable = searchable; }

    public int getQuickSearch() { return quickSearch; }
    public void setQuickSearch(int quickSearch) { this.quickSearch = quickSearch; }

    public int getFilterable() { return filterable; }
    public void setFilterable(int filterable) { this.filterable = filterable; }

    public String getExt() { return ext; }
    public void setExt(String ext) { this.ext = ext; }

    public String getJar() { return jar; }
    public void setJar(String jar) { this.jar = jar; }

    public String getPlayerUrl() { return playerUrl; }
    public void setPlayerUrl(String playerUrl) { this.playerUrl = playerUrl; }

    public boolean isSearchable() { return searchable == 1; }

    public static List<SourceBean> emptyList() {
        return new ArrayList<>();
    }
}
