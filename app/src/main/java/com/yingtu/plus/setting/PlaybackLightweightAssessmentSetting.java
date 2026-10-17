package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class PlaybackLightweightAssessmentSetting {
    public static boolean isEnrolled() { return Prefers.getBoolean("ab_enrolled", false); }
    public static Object coordinator() { return null; }
}
