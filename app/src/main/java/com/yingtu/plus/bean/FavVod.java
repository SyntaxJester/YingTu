package com.yingtu.plus.com.yingtu.plus.bean;

import java.io.Serializable;

/**
 * 收藏的影片（分享收藏）。来自某个站源的详情页，可被导出/分享。
 */
public class FavVod implements Serializable {
    public String sourceKey;
    public String vodId;
    public String name;
    public String pic;
    public long createTime;

    public FavVod() {}

    public FavVod(String sourceKey, String vodId, String name, String pic) {
        this.sourceKey = sourceKey;
        this.vodId = vodId;
        this.name = name;
        this.pic = pic;
        this.createTime = System.currentTimeMillis();
    }

    public String key() {
        return sourceKey + "@@" + vodId;
    }
}
