package com.yingtu.plus.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类数据（首页顶部的分类标签，如 围观短剧/河马短剧/好看短剧）。
 *
 * 每个分类可带「筛选条件」(filters)：TVBox 标准 class + filters 结构。
 * 选中某分类后，下方展开一行筛选 chip（分类/地区/年份...），
 * 选择后作为 extend 参数传给 spider.categoryContent 实现分类筛选。
 */
public class MovieSort implements Serializable {
    public String typeId;
    public String typeName;

    /** 该分类的筛选分组（可能多组：分类/剧情/地区...） */
    public List<Filter> filters = new ArrayList<>();

    public MovieSort(String typeId, String typeName) {
        this.typeId = typeId;
        this.typeName = typeName;
    }

    public boolean hasFilter() {
        return filters != null && !filters.isEmpty();
    }

    /** 一个筛选分组，如 key=class name=分类 value=[全部,都市,逆袭...] */
    public static class Filter implements Serializable {
        public String key;
        public String name;
        public List<Value> values = new ArrayList<>();

        public Filter(String key, String name) {
            this.key = key;
            this.name = name;
        }
    }

    /** 一个筛选项，n=显示名 v=提交值 */
    public static class Value implements Serializable {
        public String n;
        public String v;

        public Value(String n, String v) {
            this.n = n;
            this.v = v;
        }
    }
}
