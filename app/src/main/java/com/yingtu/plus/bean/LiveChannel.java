package com.yingtu.plus.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 直播频道。一个频道可包含多条播放地址（多线路）。
 */
public class LiveChannel implements Serializable {

    public String name;
    public String logo;
    public String groupName;
    public String ua;          // 自定义 User-Agent（m3u http-user-agent）
    public final List<String> urls = new ArrayList<>();
    public int lineIndex = 0;  // 当前选中线路

    public LiveChannel() {}

    public LiveChannel(String name) {
        this.name = name;
    }

    public String currentUrl() {
        if (urls.isEmpty()) return "";
        int i = Math.max(0, Math.min(lineIndex, urls.size() - 1));
        return urls.get(i);
    }

    public boolean hasMultiLine() {
        return urls.size() > 1;
    }
}
