package com.yingtu.plus.api.loader;

import com.yingtu.plus.base.App;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.fongmi.quickjs.crawler.Loader;

import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

/**
 * JS Spider（csp_ + api 以 .js 结尾 / 或 .js 文件）加载器。
 *
 * 依赖 QuickJS 运行时模块（:quickjs），通过 com.fongmi.quickjs.crawler.Loader
 * 构建 JS Spider 实例。需要 catvod jar 提供的 DexClassLoader（供 require 原生扩展），
 * 没有则传入 app 自身的 ClassLoader 兜底。
 */
public class JsLoader {

    private Loader loader;
    private final ConcurrentHashMap<String, Spider> spiders = new ConcurrentHashMap<>();
    private String recent;

    /** 懒加载 QuickJS 运行时：仅在真正用到 JS Spider 时才初始化 native 库。 */
    private Loader loader() {
        if (loader == null) {
            synchronized (this) {
                if (loader == null) loader = new Loader();
            }
        }
        return loader;
    }

    public void clear() {
        for (Spider s : spiders.values()) {
            try { s.destroy(); } catch (Throwable ignored) {}
        }
        spiders.clear();
        recent = null;
    }

    public void setRecent(String key) {
        this.recent = key;
    }

    public Spider getSpider(String key, String api, String ext, DexClassLoader dex) {
        try {
            Spider cached = spiders.get(key);
            if (cached != null) return cached;
            Spider spider = loader().spider(api, dex);
            spider.siteKey = key;
            spider.init(App.context(), ext == null ? "" : ext);
            spiders.put(key, spider);
            return spider;
        } catch (Throwable e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }
}
