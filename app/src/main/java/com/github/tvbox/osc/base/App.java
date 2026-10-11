package com.github.tvbox.osc.base;

import android.app.Application;
import android.content.Context;

import com.github.tvbox.osc.api.ApiConfig;
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
        // 加载视频源配置
        ApiConfig.get().init(this);
    }

    public static App getInstance() {
        return instance;
    }

    public static Context context() {
        return instance.getApplicationContext();
    }
}
