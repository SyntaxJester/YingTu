package com.yingtu.plus.com.yingtu.plus.bean;

import java.io.Serializable;

/**
 * 解析器（JX）配置。对应 TVBox 配置里 parses 数组的单个条目。
 *
 * type 语义（兼容 TVBox / FongMi）：
 *   0 = 网页嗅探（在 WebView 里加载 url+播放页，拦截网络请求抓直链）
 *   1 = JSON 接口（GET parseUrl + 播放页URL，从返回 JSON 的 url 字段取直链）
 */
public class ParseBean implements Serializable {

    private String name;
    private int type;
    private String url;
    private boolean selected;

    public ParseBean() {}

    public ParseBean(String name, int type, String url) {
        this.name = name;
        this.type = type;
        this.url = url;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUrl() {
        return url == null ? "" : url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isEmpty() {
        return type == 0 && (url == null || url.isEmpty());
    }
}
