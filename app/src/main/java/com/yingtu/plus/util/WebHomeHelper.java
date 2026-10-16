package com.yingtu.plus.com.yingtu.plus.util;

import com.yingtu.plus.bean.WebHomeEntry;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * WebHome 网页主页条目存取（Hawk 本地持久化）。
 *
 * 首次使用时自动植入两个内置示例主页（assets/webhome 下）。
 */
public class WebHomeHelper {

    private WebHomeHelper() {}

    public static List<WebHomeEntry> getAll() {
        seedIfNeeded();
        return Hawk.get(HawkConfig.WEBHOME_ENTRIES, new ArrayList<>());
    }

    public static void add(WebHomeEntry entry) {
        if (entry == null || entry.target == null || entry.target.isEmpty()) return;
        List<WebHomeEntry> list = getAll();
        // 去重（同 target）
        Iterator<WebHomeEntry> it = list.iterator();
        while (it.hasNext()) {
            if (entry.target.equals(it.next().target)) it.remove();
        }
        list.add(0, entry);
        Hawk.put(HawkConfig.WEBHOME_ENTRIES, list);
    }

    public static void delete(String target) {
        List<WebHomeEntry> list = getAll();
        Iterator<WebHomeEntry> it = list.iterator();
        while (it.hasNext()) {
            if (target.equals(it.next().target)) it.remove();
        }
        Hawk.put(HawkConfig.WEBHOME_ENTRIES, list);
    }

    /** 首次运行植入内置示例，仅执行一次（即使用户删光也不再自动恢复）。 */
    private static void seedIfNeeded() {
        if (Hawk.get(HawkConfig.WEBHOME_SEEDED, false)) return;
        List<WebHomeEntry> list = Hawk.get(HawkConfig.WEBHOME_ENTRIES, new ArrayList<>());
        WebHomeEntry showcase = new WebHomeEntry("能力演示主页",
                "webhome/showcase.html", WebHomeEntry.TYPE_ASSET);
        showcase.builtin = true;
        WebHomeEntry basic = new WebHomeEntry("基础模板主页",
                "webhome/basic.html", WebHomeEntry.TYPE_ASSET);
        basic.builtin = true;
        list.add(showcase);
        list.add(basic);
        Hawk.put(HawkConfig.WEBHOME_ENTRIES, list);
        Hawk.put(HawkConfig.WEBHOME_SEEDED, true);
    }
}
