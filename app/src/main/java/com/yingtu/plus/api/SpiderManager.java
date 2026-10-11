package com.yingtu.plus.api;

import android.text.TextUtils;

import com.yingtu.plus.api.loader.JarLoader;
import com.yingtu.plus.api.loader.JsLoader;
import com.yingtu.plus.bean.SourceBean;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * Spider 统一调度（参考 webhtv 的 BaseLoader 精简）。
 *
 * 根据站点 api 前缀选择加载器：
 *  - csp_ 且 api 以 .js 结尾 / jar 以 .js 结尾 → JS Spider（QuickJS）
 *  - csp_ 其余 → JAR Spider（DexClassLoader + catvod 壳）
 *
 * jar 来源：配置顶层 "spider" 字段（可带 ;md5; 校验段）。
 */
public class SpiderManager {

    private static SpiderManager instance;

    private final JarLoader jarLoader = new JarLoader();
    private final JsLoader jsLoader = new JsLoader();

    private SpiderManager() {}

    public static SpiderManager get() {
        if (instance == null) {
            synchronized (SpiderManager.class) {
                if (instance == null) instance = new SpiderManager();
            }
        }
        return instance;
    }

    public void clear() {
        jarLoader.clear();
        jsLoader.clear();
    }

    /** 预加载配置里的 spider jar（在源解析后调用，异步执行更佳）。 */
    public void parseJar(String jar) {
        if (TextUtils.isEmpty(jar)) return;
        jarLoader.setRecent(jar);
        jarLoader.parseJar(JarLoader.md5(jar), jar);
    }

    public boolean isCsp(String api) {
        return api != null && api.startsWith("csp_");
    }

    public boolean isJs(String api) {
        return api != null && (api.startsWith("csp_") && api.endsWith(".js")) || (api != null && api.contains(".js"));
    }

    /**
     * 获取某个站点的 Spider。
     * @param site 站点
     * @param jar  配置里的 spider jar 地址
     */
    public Spider getSpider(SourceBean site, String jar) {
        if (site == null) return new SpiderNull();
        String api = site.getApi();
        String key = site.getKey();
        String ext = site.getExt();
        if (!isCsp(api)) return new SpiderNull();

        if (api.endsWith(".js")) {
            DexClassLoader dex = null; // JS spider 一般不需要 jar dex
            return jsLoader.getSpider(key, api, ext, dex);
        }
        jarLoader.setRecent(jar);
        return jarLoader.getSpider(key, api, ext, jar);
    }

    public Object[] proxy(Map<String, String> params) {
        return jarLoader.proxy(params);
    }
}
