package com.github.tvbox.osc.bean;

import java.io.Serializable;

/**
 * 播放/观看历史记录。本地 Hawk 持久化，用于「观看历史」页与续播。
 */
public class History implements Serializable {

    /** 唯一键：sourceKey + "@@" + vodId */
    public String key;
    public String sourceKey;
    public String vodId;
    public String vodName;
    public String vodPic;

    /** 当前线路下标 / 剧集下标 */
    public int playFlag;
    public int playIndex;
    /** 当前集名，用于历史列表角标显示（如「观看至 第3集」） */
    public String episodeName;

    /** 播放进度与时长（毫秒），用于续播 */
    public long position;
    public long duration;

    /** 最近一次观看时间戳 */
    public long updateTime;

    public History() {}

    public static String buildKey(String sourceKey, String vodId) {
        return sourceKey + "@@" + vodId;
    }

    /** 已观看百分比（0-100），无时长时返回 0 */
    public int percent() {
        if (duration <= 0) return 0;
        long p = Math.min(position, duration);
        return (int) (p * 100 / duration);
    }
}
