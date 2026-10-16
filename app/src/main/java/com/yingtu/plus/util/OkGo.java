package com.yingtu.plus.com.yingtu.plus.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Looper;

import okhttp3.OkHttpClient;

/**
 * 全局线程池 + OkHttp 单例 + 主线程切换工具。
 */
public class OkGo {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    private static final Executor IO = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static OkHttpClient client() {
        return CLIENT;
    }

    public static void io(Runnable r) {
        IO.execute(r);
    }

    public static void main(Runnable r) {
        MAIN.post(r);
    }
}
