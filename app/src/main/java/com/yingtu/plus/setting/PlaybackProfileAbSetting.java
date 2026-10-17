package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;

public class PlaybackProfileAbSetting {
    public static boolean isEnrolled() { return PlaybackLightweightAssessmentSetting.isEnrolled(); }
    public static int getEnrollmentResolution() { return isEnrolled() ? 1 : 0; }
}
