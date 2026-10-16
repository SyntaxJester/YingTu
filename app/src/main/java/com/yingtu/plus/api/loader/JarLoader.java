package com.yingtu.plus.api.loader;

import com.yingtu.plus.base.App;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import java.io.File;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * CatVod JAR Spider 加载器（参考 webhtv/FongMi 的 JarLoader 精简实现）。
 *
 * 负责：下载/缓存 spider.jar → DexClassLoader 加载 → 反射调用
 * com.github.catvod.spider.Init.init(Context) 完成壳 so 释放与子 dex 加载 →
 * 按 api（csp_XxxGuard）实例化具体 Spider。
 */
public class JarLoader {

    private final ConcurrentHashMap<String, DexClassLoader> loaders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Spider> spiders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Method> proxyMethods = new ConcurrentHashMap<>();
    private String recent;

    public void clear() {
        for (Spider s : spiders.values()) {
            try { s.destroy(); } catch (Throwable ignored) {}
        }
        loaders.clear();
        spiders.clear();
        proxyMethods.clear();
        recent = null;
    }

    public void setRecent(String jar) {
        this.recent = md5(jar);
    }

    /** 下载并加载 jar（key 为 jar 的 md5），已加载则跳过。 */
    public synchronized void parseJar(String key, String jar) {
        if (loaders.containsKey(key)) return;
        try {
            // 去掉 ;md5;... 校验段
            String[] texts = jar.split(";md5;");
            jar = texts[0];
            File file;
            if (jar.startsWith("http")) {
                file = download(jar);
            } else if (jar.startsWith("file")) {
                file = new File(toLocalPath(jar));
            } else if (jar.startsWith("assets")) {
                return; // 本工程不内置 assets jar
            } else {
                return;
            }
            if (file == null || !file.exists() || file.length() == 0) return;
            load(key, file);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void load(String key, File file) {
        try {
            file.setReadOnly();
            File cache = new File(App.context().getCacheDir(), "catvod_jar");
            if (!cache.exists()) cache.mkdirs();
            DexClassLoader loader = new DexClassLoader(
                    file.getAbsolutePath(), cache.getAbsolutePath(), null,
                    App.context().getClassLoader());
            invokeInit(loader);
            invokeProxy(key, loader);
            loaders.put(key, loader);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void invokeInit(DexClassLoader loader) {
        try {
            Class<?> clz = loader.loadClass("com.github.catvod.spider.Init");
            Method m = clz.getMethod("init", android.content.Context.class);
            m.invoke(clz, App.context());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void invokeProxy(String key, DexClassLoader loader) {
        try {
            Class<?> clz = loader.loadClass("com.github.catvod.spider.Proxy");
            Method m = clz.getMethod("proxy", Map.class);
            proxyMethods.put(key, m);
        } catch (Throwable ignored) {
        }
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        try {
            String jaKey = md5(jar);
            String spKey = jaKey + key;
            Spider cached = spiders.get(spKey);
            if (cached != null) return cached;

            parseJar(jaKey, jar);
            DexClassLoader loader = loaders.get(jaKey);
            if (loader == null) return new SpiderNull();

            String clsName = "com.github.catvod.spider." + api.split("csp_")[1];
            Spider spider = (Spider) loader.loadClass(clsName).newInstance();
            spider.siteKey = key;
            spider.init(App.context(), ext == null ? "" : ext);
            spiders.put(spKey, spider);
            return spider;
        } catch (Throwable e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    public Object[] proxy(Map<String, String> params) {
        try {
            Method m = recent != null ? proxyMethods.get(recent) : null;
            if (m == null && !proxyMethods.isEmpty()) m = proxyMethods.values().iterator().next();
            if (m == null) return null;
            return (Object[]) m.invoke(null, params);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    // ===== helpers =====

    private File download(String url) {
        try {
            File dir = new File(App.context().getCacheDir(), "catvod_jar");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, md5(url) + ".jar");
            if (out.exists() && out.length() > 0) return out;

            OkHttpClient client = new OkHttpClient.Builder().build();
            Request req = new Request.Builder().url(url).build();
            try (Response resp = client.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return null;
                byte[] bytes = resp.body().bytes();
                java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
                fos.write(bytes);
                fos.close();
            }
            return out;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private String toLocalPath(String fileUrl) {
        // file://xxx or file:xxx -> 绝对路径（结合配置目录由调用方处理，这里简单剥壳）
        String p = fileUrl.replaceFirst("^file://", "").replaceFirst("^file:", "");
        return p;
    }

    public static String md5(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Throwable e) {
            return String.valueOf(s.hashCode());
        }
    }
}
