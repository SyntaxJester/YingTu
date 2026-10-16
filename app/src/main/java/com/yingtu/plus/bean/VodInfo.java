package com.yingtu.plus.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 播放线路 + 剧集分组。详情页用来渲染「线路切换 + 选集」。
 */
public class VodInfo implements Serializable {

    public String id;
    public String name;
    public String pic;
    public String note;
    public String year;
    public String area;
    public String type;
    public String actor;
    public String director;
    public String des;
    public String sourceKey;

    /** 当前选中的线路下标 */
    public int playFlag = 0;
    /** 当前选中的剧集下标 */
    public int playIndex = 0;

    public List<VodSeriesFlag> seriesFlags = new ArrayList<>();

    public static class VodSeriesFlag implements Serializable {
        public String name;                     // 线路名，如「线路1」
        public List<VodSeries> list = new ArrayList<>();
        public boolean selected = false;

        public VodSeriesFlag(String name) { this.name = name; }
    }

    public static class VodSeries implements Serializable {
        public String name;    // 集名，如「第1集」
        public String url;     // 播放地址 / 需二次解析的参数
        public boolean selected = false;

        public VodSeries(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }
}
