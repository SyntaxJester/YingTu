package com.yingtu.plus.util;

import com.yingtu.plus.bean.History;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * 观看历史存取助手（基于 Hawk 本地持久化）。
 *
 * - save：插入或更新一条记录（按 key 去重，更新进度/线路/时间后置顶）
 * - getAll：按最近观看时间倒序返回
 * - find：按 key 查询（用于详情页/播放页续播）
 * - delete / clear：删除单条 / 清空
 */
public class HistoryHelper {

    private static final int MAX = 200;

    private HistoryHelper() {}

    @SuppressWarnings("unchecked")
    public static List<History> getAll() {
        List<History> list = Hawk.get(HawkConfig.PLAY_HISTORY, new ArrayList<>());
        Collections.sort(list, new Comparator<History>() {
            @Override
            public int compare(History a, History b) {
                return Long.compare(b.updateTime, a.updateTime);
            }
        });
        return list;
    }

    public static History find(String key) {
        if (key == null) return null;
        for (History h : getAll()) {
            if (key.equals(h.key)) return h;
        }
        return null;
    }

    public static void save(History history) {
        if (history == null || history.key == null) return;
        history.updateTime = System.currentTimeMillis();
        List<History> list = getAll();
        Iterator<History> it = list.iterator();
        while (it.hasNext()) {
            History h = it.next();
            if (history.key.equals(h.key)) {
                it.remove();
                break;
            }
        }
        list.add(0, history);
        while (list.size() > MAX) list.remove(list.size() - 1);
        Hawk.put(HawkConfig.PLAY_HISTORY, list);
    }

    public static void delete(String key) {
        if (key == null) return;
        List<History> list = getAll();
        Iterator<History> it = list.iterator();
        while (it.hasNext()) {
            if (key.equals(it.next().key)) {
                it.remove();
                break;
            }
        }
        Hawk.put(HawkConfig.PLAY_HISTORY, list);
    }

    public static void clear() {
        Hawk.delete(HawkConfig.PLAY_HISTORY);
    }
}
