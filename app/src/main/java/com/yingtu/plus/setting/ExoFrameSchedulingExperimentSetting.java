package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class ExoFrameSchedulingExperimentSetting {
    public static boolean getResolution() { return Prefers.getBoolean("perf_exo_frame_sched", false); }
}
