package com.yingtu.plus.com.yingtu.plus.bean;

import java.io.Serializable;

/**
 * WebHome 网页主页条目。
 *
 * WebHome 是「用 HTML/CSS/JS 定制的网页首页」，运行在内置 WebView 中，
 * 并注入 App Native SDK（window.fm / window.fongmi），页面可调用搜索、
 * 播放、跨域请求、缓存、最近观看等原生能力。
 *
 * 一个条目可以是三种来源之一：
 *   - TYPE_URL   远程网址（http/https）
 *   - TYPE_LOCAL 本地 HTML 文件（SAF content:// uri 或 file:// 路径）
 *   - TYPE_ASSET 内置示例（assets 下的 html）
 */
public class WebHomeEntry implements Serializable {

    public static final int TYPE_URL = 0;
    public static final int TYPE_LOCAL = 1;
    public static final int TYPE_ASSET = 2;

    public String name;
    /** 远程 URL / content:// / file:// / asset 相对路径（如 webhome/basic.html） */
    public String target;
    public int type;
    public boolean builtin;
    public long createTime;

    public WebHomeEntry() {}

    public WebHomeEntry(String name, String target, int type) {
        this.name = name;
        this.target = target;
        this.type = type;
        this.createTime = System.currentTimeMillis();
    }

    public boolean isLocal() {
        return type == TYPE_LOCAL;
    }

    public boolean isAsset() {
        return type == TYPE_ASSET;
    }

    /** 列表副标题展示用 */
    public String subtitle() {
        switch (type) {
            case TYPE_URL:
                return target;
            case TYPE_LOCAL:
                return "本地 HTML 文件";
            case TYPE_ASSET:
                return "内置示例";
            default:
                return target;
        }
    }
}
