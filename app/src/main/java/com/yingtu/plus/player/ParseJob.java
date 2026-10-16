package com.yingtu.plus.player;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.yingtu.plus.bean.ParseBean;
import com.yingtu.plus.util.OkGo;
import com.yingtu.plus.util.Sniffer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 二次解析任务（参考 FongMi/webhtv 的 ParseJob 精简实现）。
 *
 * 支持两种解析方式：
 *   type=1 JSON 接口解析：GET parseUrl + playUrl，从返回 JSON 的 url 字段取直链；
 *   type=0 网页嗅探解析   ：在 WebView 里加载 parseUrl + playUrl，拦截网络请求抓直链。
 *
 * 用法：
 *   ParseJob.create(callback).start(parse, playUrl);
 *
 * 所有回调都保证切回主线程，且只回调一次（done 原子位控制）。
 */
public class ParseJob {

    public interface Callback {
        /** @param headers 播放所需请求头（Referer/UA 等），可能为空 */
        void onParseSuccess(Map<String, String> headers, String url);

        void onParseError();
    }

    /** 嗅探/解析整体超时（毫秒） */
    private static final long TIMEOUT_MS = 15000;

    private final AtomicBoolean done = new AtomicBoolean(false);
    private final Handler main = new Handler(Looper.getMainLooper());
    private Callback callback;
    private WebParser webParser;
    private Call httpCall;

    private ParseJob(Callback callback) {
        this.callback = callback;
    }

    public static ParseJob create(Callback callback) {
        return new ParseJob(callback);
    }

    public ParseJob start(ParseBean parse, String playUrl) {
        // 整体超时
        main.postDelayed(this::onError, TIMEOUT_MS);
        try {
            if (parse != null && !parse.isEmpty() && parse.getType() == 1) {
                // JSON 接口解析
                jsonParse(parse, playUrl);
            } else if (parse != null && !parse.isEmpty() && parse.getType() == 0) {
                // 指定了网页解析器：加载 解析器URL + 播放页URL
                webParse(parse.getUrl() + playUrl, new HashMap<>());
            } else if (Sniffer.isVideoFormat(playUrl)) {
                // 没有可用解析器但本身已是直链
                onSuccess(new HashMap<>(), playUrl);
            } else {
                // 没有解析器，退化为直接嗅探播放页本身
                webParse(playUrl, new HashMap<>());
            }
        } catch (Throwable e) {
            onError();
        }
        return this;
    }

    /** type=1：JSON 接口解析 */
    private void jsonParse(ParseBean parse, String playUrl) {
        String api = parse != null ? parse.getUrl() : "";
        if (TextUtils.isEmpty(api)) {
            onError();
            return;
        }
        Request request = new Request.Builder().url(api + playUrl).build();
        httpCall = OkGo.client().newCall(request);
        httpCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onError();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                    String url = safe(obj, "url");
                    if (url.isEmpty() && obj.has("data") && obj.get("data").isJsonObject()) {
                        url = safe(obj.getAsJsonObject("data"), "url");
                    }
                    Map<String, String> headers = extractHeaders(obj);
                    if (url.length() > 8) {
                        onSuccess(headers, url);
                    } else {
                        onError();
                    }
                } catch (Throwable e) {
                    onError();
                }
            }
        });
    }

    /** type=0：WebView 网页嗅探解析 */
    private void webParse(String url, Map<String, String> headers) {
        main.post(() -> {
            webParser = WebParser.create();
            webParser.start(url, headers, new WebParser.Callback() {
                @Override
                public void onResult(Map<String, String> hs, String directUrl) {
                    onSuccess(hs, directUrl);
                }

                @Override
                public void onError() {
                    ParseJob.this.onError();
                }
            });
        });
    }

    private static String safe(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString() : "";
    }

    /** 从 JSON 返回里提取 UA / Referer / Cookie 等播放头 */
    private static Map<String, String> extractHeaders(JsonObject obj) {
        Map<String, String> headers = new HashMap<>();
        if (obj == null) return headers;
        for (String k : new String[]{"User-Agent", "ua", "Referer", "referer", "Cookie", "cookie"}) {
            if (obj.has(k) && !obj.get(k).isJsonNull()) {
                String name = k.equalsIgnoreCase("ua") ? "User-Agent"
                        : (k.substring(0, 1).toUpperCase() + k.substring(1).toLowerCase());
                headers.put(name, obj.get(k).getAsString());
            }
        }
        // 有的接口把头放在 header 对象里
        if (obj.has("header") && obj.get("header").isJsonObject()) {
            JsonObject h = obj.getAsJsonObject("header");
            for (String k : h.keySet()) {
                if (!h.get(k).isJsonNull()) headers.put(k, h.get(k).getAsString());
            }
        }
        return headers;
    }

    private void onSuccess(Map<String, String> headers, String url) {
        if (!done.compareAndSet(false, true)) return;
        main.post(() -> {
            main.removeCallbacksAndMessages(null);
            if (callback != null) callback.onParseSuccess(headers, url);
            cleanup();
        });
    }

    private void onError() {
        if (!done.compareAndSet(false, true)) return;
        main.post(() -> {
            main.removeCallbacksAndMessages(null);
            if (callback != null) callback.onParseError();
            cleanup();
        });
    }

    public void stop() {
        done.set(true);
        main.removeCallbacksAndMessages(null);
        cleanup();
    }

    private void cleanup() {
        callback = null;
        if (httpCall != null) {
            httpCall.cancel();
            httpCall = null;
        }
        if (webParser != null) {
            webParser.destroy();
            webParser = null;
        }
    }
}
