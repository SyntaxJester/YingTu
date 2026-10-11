package com.github.tvbox.osc.bean;

import java.io.Serializable;

/**
 * 直链收藏条目：用户手动保存的可直接播放地址（m3u8/mp4 等）。
 */
public class DirectLink implements Serializable {
    public String name;
    public String url;
    public long createTime;

    public DirectLink() {}

    public DirectLink(String name, String url) {
        this.name = name;
        this.url = url;
        this.createTime = System.currentTimeMillis();
    }
}
