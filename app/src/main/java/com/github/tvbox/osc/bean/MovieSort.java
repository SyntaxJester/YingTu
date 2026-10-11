package com.github.tvbox.osc.bean;

/**
 * 分类数据（首页顶部的分类标签，如 电影/电视剧/综艺）。
 */
public class MovieSort {
    public String typeId;
    public String typeName;

    public MovieSort(String typeId, String typeName) {
        this.typeId = typeId;
        this.typeName = typeName;
    }
}
