package com.yingtu.plus.util;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;

/**
 * 屏幕/尺寸工具（移植自 webhtv ResUtil + Util 的最小集）。
 */
public class UiUtil {

    public static int getScreenWidth() {
        return getScreenWidth(com.yingtu.plus.base.App.context());
    }

    public static int getScreenWidth(Context context) {
        return getMetrics(context).widthPixels;
    }

    public static int getScreenHeight() {
        return getScreenHeight(com.yingtu.plus.base.App.context());
    }

    public static int getScreenHeight(Context context) {
        return getMetrics(context).heightPixels;
    }

    private static DisplayMetrics getMetrics(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) wm.getDefaultDisplay().getRealMetrics(dm);
        return dm;
    }

    /** 触点是否靠近屏幕边缘（用于排除边缘返回手势）。 */
    public static boolean isEdge(Context context, MotionEvent e, int edge) {
        return e.getRawX() < edge
                || e.getRawX() > getScreenWidth(context) - edge
                || e.getRawY() < edge
                || e.getRawY() > getScreenHeight(context) - edge;
    }

    public static int dp2px(float dp) {
        float density = com.yingtu.plus.base.App.context().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /** 当前 Activity 窗口亮度（0~1，-1 表示跟随系统）。 */
    public static float getBrightness(Activity activity) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        if (lp.screenBrightness < 0) {
            try {
                int sys = android.provider.Settings.System.getInt(
                        activity.getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS);
                return sys / 255.0f;
            } catch (Exception e) {
                return 0.5f;
            }
        }
        return lp.screenBrightness;
    }
}
