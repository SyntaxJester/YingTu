package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class ProxySetting {
    public static String getHost() { return Prefers.getString("proxy_host", ""); }
    public static int getPort() { return Prefers.getInt("proxy_port", 0); }
    public static void putHost(String h) { Prefers.put("proxy_host", h); }
    public static void putPort(int p) { Prefers.put("proxy_port", p); }
    public static boolean isEnabled() { return !getHost().isEmpty() && getPort() > 0; }
}
