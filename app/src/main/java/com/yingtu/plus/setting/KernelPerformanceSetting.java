package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class KernelPerformanceSetting {
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_HIGH_PERF = 1;
    public static final int MODE_LOW_POWER = 2;
    
    public static int getMode() { return Prefers.getInt("kernel_perf_mode", MODE_DEFAULT); }
    public static void putMode(int m) { Prefers.put("kernel_perf_mode", m); }
}
