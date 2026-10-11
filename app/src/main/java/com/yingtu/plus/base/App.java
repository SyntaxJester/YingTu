package com.yingtu.plus.base;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.yingtu.plus.api.ApiConfig;
import com.yingtu.plus.util.HawkConfig;
import com.yingtu.plus.web.WebServer;
import com.orhanobut.hawk.Hawk;

/**
 * 应用入口。初始化全局单例、持久化存储与源配置管理器。
 */
public class App extends Application {

    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
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
    }

    public static App getInstance() {
        return instance;
    }

    public static Context context() {
        return instance.getApplicationContext();
    }
}
