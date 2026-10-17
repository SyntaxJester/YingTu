package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;
import java.util.List;
import java.util.ArrayList;

public class SiteBlockSetting {
    public static List<String> getBlocked() {
        String s = Prefers.getString("site_block_list", null);
        if (s == null) return new ArrayList<>();
        return new ArrayList<>(List.of(s.split(",")));
    }
    public static void addBlock(String site) {
        List<String> list = getBlocked();
        if (!list.contains(site)) {
            list.add(site);
            Prefers.put("site_block_list", String.join(",", list));
        }
    }
    public static void removeBlock(String site) {
        List<String> list = getBlocked();
        list.remove(site);
        Prefers.put("site_block_list", String.join(",", list));
    }
}
