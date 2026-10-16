package com.yingtu.plus.com.yingtu.plus.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 直播分组。对应 m3u 的 group-title / txt 的 #genre#。
 */
public class LiveGroup implements Serializable {

    public String name;
    public final List<LiveChannel> channels = new ArrayList<>();

    public LiveGroup() {}

    public LiveGroup(String name) {
        this.name = name;
    }

    /** 按频道名查找，没有则新建并加入 */
    public LiveChannel findOrCreate(String channelName) {
        for (LiveChannel c : channels) {
            if (c.name != null && c.name.equals(channelName)) return c;
        }
        LiveChannel c = new LiveChannel(channelName);
        channels.add(c);
        return c;
    }
}
