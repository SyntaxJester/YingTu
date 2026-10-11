package com.github.tvbox.osc.bean;

import java.io.Serializable;

/**
 * 影片条目。列表、搜索结果、详情页通用的数据载体。
 */
public class Movie implements Serializable {
    public String vodId;
    public String name;
    public String pic;      // 封面图
    public String note;     // 右下角角标，如 "更新至20集" / "HD"
    public String year;
    public String area;
    public String type;
    public String actor;
    public String director;
    public String content; // 剧情简介
    public String sourceKey;

    public Movie() {}

    public Movie(String vodId, String name, String pic, String note) {
        this.vodId = vodId;
        this.name = name;
        this.pic = pic;
        this.note = note;
    }
}
