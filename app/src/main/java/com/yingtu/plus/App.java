package com.yingtu.plus;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.gson.Gson;
import com.yingtu.plus.api.ApiConfig;
import com.yingtu.plus.util.HawkConfig;
import com.yingtu.plus.web.WebServer;
import com.orhanobut.hawk.Hawk;



/**
 * 应用入口。初始化全局单例、持久化存储与源配置管理器。
 */
public class App extends Application {

    private static App instance;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Gson GSON = new Gson();

    /** 前台 Activity 跟踪（生命周期回调）。 */
    private Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // CatVod 运行时需要 Context（Path/Prefers/Asset 等都读 Init.context()）
        com.github.catvod.Init.set(this);
        // 轻量级 KV 持久化
        Hawk.init(this).build();
        // 应用深色模式偏好（默认跟随深色，契合海洋深蓝主题）
        boolean dark = Hawk.get(HawkConfig.DARK_MODE, true);
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
        // 加载视频源配置
        ApiConfig.get().init(this);
        // 启动本地资源网关（WebHome 的 fm.res 依赖；仅绑定 127.0.0.1）
        WebServer.get().start();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                currentActivity = activity;
            }

            @Override
            public void onActivityPaused(@Nullable Activity activity) {
                if (activity != null && activity == currentActivity) {
                    currentActivity = null;
                }
            }

            @Override public void onActivityCreated(@androidx.annotation.NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
            @Override public void onActivityStarted(@androidx.annotation.NonNull Activity activity) {}
            @Override public void onActivityStopped(@androidx.annotation.NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@androidx.annotation.NonNull Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(@androidx.annotation.NonNull Activity activity) {}
        });
    }

    // ---- 原有方法 ----

    public static App getInstance() {
        return instance;
    }

    public static Context context() {
        return instance.getApplicationContext();
    }

    // ---- webhtv 兼容静态助手（player package 等大量引用）----

    /** webhtv 源码中所有 App.get() 调用 → getInstance()。 */
    public static App get() {
        return instance;
    }

    /** 主线程 post（无延迟）。 */
    public static void post(Runnable runnable) {
        MAIN_HANDLER.post(runnable);
    }

    /** 主线程 post（带延迟）。 */
    public static void post(Runnable runnable, long delayMs) {
        MAIN_HANDLER.postDelayed(runnable, delayMs);
    }

    /** 移除主线程 handler 上的回调。 */
    public static void removeCallbacks(Runnable runnable) {
        MAIN_HANDLER.removeCallbacks(runnable);
    }

    /** 全局 Gson 单例。 */
    public static Gson gson() {
        return GSON;
    }

    /** 当前前台 Activity（可能为 null，如 Service 场景）。 */
    @Nullable
    public static Activity activity() {
        return instance != null ? instance.currentActivity : null;
    }

    /** 当前时间戳（毫秒）。 */
    public static long time() {
        return System.currentTimeMillis();
    }
}
