package com.yingtu.plus.util;

import com.yingtu.plus.bean.DirectLink;
import com.yingtu.plus.bean.FavVod;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 收藏存取助手（Hawk 本地持久化）：
 *   - 直链收藏 DirectLink
 *   - 分享收藏 FavVod（影片）
 */
public class FavHelper {

    private FavHelper() {}

    // ========== 直链收藏 ==========

    public static List<DirectLink> getLinks() {
        return Hawk.get(HawkConfig.FAV_LINKS, new ArrayList<>());
    }

    public static void addLink(DirectLink link) {
        if (link == null || link.url == null || link.url.isEmpty()) return;
        List<DirectLink> list = getLinks();
        // 去重（同 url）
        Iterator<DirectLink> it = list.iterator();
        while (it.hasNext()) {
            if (link.url.equals(it.next().url)) it.remove();
        }
        list.add(0, link);
        Hawk.put(HawkConfig.FAV_LINKS, list);
    }

    public static void deleteLink(String url) {
        List<DirectLink> list = getLinks();
        Iterator<DirectLink> it = list.iterator();
        while (it.hasNext()) {
            if (url.equals(it.next().url)) it.remove();
        }
        Hawk.put(HawkConfig.FAV_LINKS, list);
    }

    // ========== 分享收藏（影片） ==========

    public static List<FavVod> getVods() {
        return Hawk.get(HawkConfig.FAV_VODS, new ArrayList<>());
    }

    public static boolean isFav(String sourceKey, String vodId) {
        String key = sourceKey + "@@" + vodId;
        for (FavVod f : getVods()) {
            if (key.equals(f.key())) return true;
        }
        return false;
    }

    public static void toggleVod(FavVod vod) {
        List<FavVod> list = getVods();
        boolean removed = false;
        Iterator<FavVod> it = list.iterator();
        while (it.hasNext()) {
            if (vod.key().equals(it.next().key())) {
                it.remove();
                removed = true;
                break;
            }
        }
        if (!removed) list.add(0, vod);
        Hawk.put(HawkConfig.FAV_VODS, list);
    }

    public static void deleteVod(String key) {
        List<FavVod> list = getVods();
        Iterator<FavVod> it = list.iterator();
        while (it.hasNext()) {
            if (key.equals(it.next().key())) it.remove();
        }
        Hawk.put(HawkConfig.FAV_VODS, list);
    }
}
