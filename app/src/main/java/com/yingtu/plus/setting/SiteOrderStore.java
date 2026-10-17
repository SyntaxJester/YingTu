package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;
import java.util.List;
import java.util.ArrayList;

public class SiteOrderStore {
    public static List<String> getOrdered() {
        String s = Prefers.getString("site_order", null);
        if (s == null) return new ArrayList<>();
        return new ArrayList<>(List.of(s.split(",")));
    }
    public static void putOrdered(List<String> order) {
        Prefers.put("site_order", String.join(",", order));
    }
}
