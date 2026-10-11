package com.yingtu.plus.web;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.yingtu.plus.bean.History;
import com.yingtu.plus.ui.activity.DetailActivity;
import com.yingtu.plus.ui.activity.HomeActivity;
import com.yingtu.plus.ui.activity.LiveActivity;
import com.yingtu.plus.ui.activity.PlayActivity;
import com.yingtu.plus.ui.activity.SearchActivity;
import com.yingtu.plus.util.HistoryHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * WebHome 的 Native 桥接对象（@JavascriptInterface），对应 webhtv 的 HomeWebBridge。
 *
 * JS 侧通过 {@code fongmiBridge.invoke(id, method, payloadJson)} 调用，
 * 这里在后台线程处理后用 {@code window.fongmiNative.resolve/reject} 回调。
 *
 * 相比 webhtv 原版做了裁剪以匹配「影图+」现有能力：
 *   - player.playUrl / pan.play  → PlayActivity 播放直链（支持 header 注入）
 *   - player.playVod             → DetailActivity 进入站源详情
 *   - player.playVodInline       → 取首集/mark 集直链用 PlayActivity 播放
 *   - app.search                 → SearchActivity
 *   - app.history                → 本地观看历史
 *   - cache.*                    → Hawk 持久化
 *   - net.request / net.resourceUrl → OkHttp 代理（绕过 CORS）
 */
public class HomeWebBridge {

    private static final int INLINE_LIMIT = 12000;
    private static final int CHUNK_SIZE = 60000;

    public interface UiCallback {
        void setToolbar(boolean visible);
        void setChrome(JsonObject payload);
        void restoreChrome();
        String getViewportJson();
        void back();
        void reload();
        void openVod();
        void openSetting();
    }

    private final Activity activity;
    private final WebView webView;
    private final UiCallback ui;
    private final Gson gson = new Gson();
    private final Map<String, String> results = new ConcurrentHashMap<>();

    public HomeWebBridge(Activity activity, WebView webView, UiCallback ui) {
        this.activity = activity;
        this.webView = webView;
        this.ui = ui;
    }

    @JavascriptInterface
    public void invoke(String requestId, String method, String payload) {
        OkGoExec(() -> handle(requestId, method, WebCall.object(payload)));
    }

    @JavascriptInterface
    public void console(String level, String message) {
        android.util.Log.d("webview", (level == null ? "log" : level) + " " + message);
    }

    @JavascriptInterface
    public void network(String type, String method, String url, int status, long durationMs, String detail) {
        android.util.Log.d("webview-net", type + " " + method + " " + url + " " + status);
    }

    @JavascriptInterface
    public String resourceUrl(String url, String options) {
        JsonObject object = WebCall.object(options);
        StringBuilder builder = new StringBuilder(
                WebServer.get().getAddress("/webResource?url=")).append(encode(url));
        if (object.has("headers"))
            builder.append("&headers=").append(encode(object.get("headers").toString()));
        if ("include".equals(safe(object, "credentials")))
            builder.append("&credentials=include");
        return builder.toString();
    }

    @JavascriptInterface
    public int resultLength(String id) {
        String result = results.get(id);
        return result == null ? 0 : result.length();
    }

    @JavascriptInterface
    public String resultChunk(String id, int start) {
        String result = results.get(id);
        if (result == null || start < 0 || start >= result.length()) return "";
        return result.substring(start, Math.min(start + CHUNK_SIZE, result.length()));
    }

    @JavascriptInterface
    public void clearResult(String id) {
        results.remove(id);
    }

    private void handle(String requestId, String method, JsonObject payload) {
        try {
            String result;
            switch (method) {
                case "net.request": result = WebCall.request(payload); break;
                case "net.resourceUrl": result = quote(resourceUrl(safe(payload, "url"), payload.toString())); break;
                case "player.playUrl": result = playUrl(payload); break;
                case "player.playVod": result = playVod(payload); break;
                case "player.playVodInline": result = playVodInline(payload); break;
                case "player.preloadArtwork": result = "{}"; break;
                case "player.control": result = "{}"; break;
                case "player.status": result = "{}"; break;
                case "app.search": result = search(payload); break;
                case "app.openVod": result = openVod(); break;
                case "app.openLive": result = openLive(); break;
                case "app.openKeep": result = openKeep(); break;
                case "app.openSetting": result = openSetting(); break;
                case "app.history": result = history(); break;
                case "pan.check": result = "[]"; break;
                case "pan.play": result = playPan(payload); break;
                case "cache.get": result = quote(com.orhanobut.hawk.Hawk.get(cacheKey(payload), "")); break;
                case "cache.set": result = cacheSet(payload); break;
                case "cache.del": result = cacheDel(payload); break;
                case "device.info": result = device(); break;
                case "site.info": result = site(); break;
                case "config.info": result = config(); break;
                case "ext.info": result = extInfo(); break;
                case "ext.log": result = "{}"; break;
                case "ext.toast": result = toast(payload); break;
                case "ui.setToolbar": result = setToolbar(payload); break;
                case "ui.setChrome": result = setChrome(payload); break;
                case "ui.restoreChrome": post(ui::restoreChrome); result = "{}"; break;
                case "ui.getViewport": result = ui.getViewportJson(); break;
                case "navigation.back": post(ui::back); result = "{}"; break;
                case "navigation.reload": post(ui::reload); result = "{}"; break;
                default: throw new IllegalArgumentException("Unknown method: " + method);
            }
            resolve(requestId, result);
        } catch (Throwable e) {
            reject(requestId, e.getMessage());
        }
    }

    // ===== player =====

    private String playUrl(JsonObject payload) {
        String url = safe(payload, "url");
        String title = safe(payload, "title");
        String pic = safe(payload, "pic");
        if (payload.has("headers") || "include".equals(safe(payload, "credentials")))
            url = resourceUrl(url, payload.toString());
        final String playUrl = url;
        final String playTitle = TextUtils.isEmpty(title) ? playUrl : title;
        post(() -> {
            Intent i = new Intent(activity, PlayActivity.class);
            i.putExtra(PlayActivity.KEY_URL, playUrl);
            i.putExtra(PlayActivity.KEY_TITLE, playTitle);
            i.putExtra(PlayActivity.KEY_VOD_PIC, pic);
            activity.startActivity(i);
        });
        return "{}";
    }

    private String playVod(JsonObject payload) {
        String siteKey = safe(payload, "siteKey");
        String vodId = safe(payload, "vodId");
        post(() -> {
            Intent i = new Intent(activity, DetailActivity.class);
            i.putExtra(DetailActivity.KEY_SOURCE, siteKey);
            i.putExtra(DetailActivity.KEY_VOD_ID, vodId);
            activity.startActivity(i);
        });
        return "{}";
    }

    private String playVodInline(JsonObject payload) {
        String title = safe(payload, "title");
        if (TextUtils.isEmpty(title)) title = safe(payload, "vod_name");
        String pic = safe(payload, "pic");
        if (TextUtils.isEmpty(pic)) pic = safe(payload, "vod_pic");
        String mark = safe(payload, "mark");

        String url = "";
        String epName = "";
        if (payload.has("episodes") && payload.get("episodes").isJsonArray()) {
            JsonArray eps = payload.getAsJsonArray("episodes");
            JsonObject chosen = null;
            for (int i = 0; i < eps.size(); i++) {
                JsonObject ep = eps.get(i).getAsJsonObject();
                if (!TextUtils.isEmpty(mark) && mark.equals(safe(ep, "name"))) {
                    chosen = ep;
                    break;
                }
            }
            if (chosen == null && eps.size() > 0) chosen = eps.get(0).getAsJsonObject();
            if (chosen != null) {
                url = safe(chosen, "url");
                epName = safe(chosen, "name");
            }
        }
        if (payload.has("headers")) {
            // 为直链注入 header
            JsonObject wrap = new JsonObject();
            wrap.addProperty("url", url);
            wrap.add("headers", payload.get("headers"));
            url = resourceUrl(url, wrap.toString());
        }
        final String playUrl = url;
        final String playTitle = (TextUtils.isEmpty(title) ? "" : title)
                + (TextUtils.isEmpty(epName) ? "" : " " + epName);
        final String playPic = pic;
        if (!TextUtils.isEmpty(playUrl)) {
            post(() -> {
                Intent i = new Intent(activity, PlayActivity.class);
                i.putExtra(PlayActivity.KEY_URL, playUrl);
                i.putExtra(PlayActivity.KEY_TITLE, playTitle.trim());
                i.putExtra(PlayActivity.KEY_VOD_PIC, playPic);
                activity.startActivity(i);
            });
        }
        JsonObject result = new JsonObject();
        result.addProperty("vodId", safe(payload, "vod_id"));
        return result.toString();
    }

    private String playPan(JsonObject payload) {
        String url = safe(payload, "url");
        if (url.regionMatches(true, 0, "push://", 0, 7)) url = url.substring(7);
        String title = safe(payload, "title");
        if (TextUtils.isEmpty(url)) throw new IllegalArgumentException("url不能为空");
        final String playUrl = url;
        final String playTitle = TextUtils.isEmpty(title) ? playUrl : title;
        post(() -> {
            Intent i = new Intent(activity, PlayActivity.class);
            i.putExtra(PlayActivity.KEY_URL, playUrl);
            i.putExtra(PlayActivity.KEY_TITLE, playTitle);
            activity.startActivity(i);
        });
        return "{}";
    }

    // ===== app =====

    private String search(JsonObject payload) {
        String keyword = safe(payload, "keyword");
        post(() -> {
            Intent i = new Intent(activity, SearchActivity.class);
            i.putExtra("keyword", keyword);
            activity.startActivity(i);
        });
        return "{}";
    }

    private String openVod() {
        post(ui::openVod);
        return "{}";
    }

    private String openLive() {
        post(() -> activity.startActivity(new Intent(activity, LiveActivity.class)));
        return "{}";
    }

    private String openKeep() {
        post(() -> {
            Intent i = new Intent(activity, HomeActivity.class);
            i.putExtra(HomeActivity.KEY_TAB, 4); // 分享收藏 Tab
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(i);
        });
        return "{}";
    }

    private String openSetting() {
        post(ui::openSetting);
        return "{}";
    }

    private String history() {
        List<History> list = HistoryHelper.getAll();
        JsonArray arr = new JsonArray();
        for (History h : list) {
            JsonObject o = new JsonObject();
            o.addProperty("key", h.key);
            o.addProperty("vodPic", h.vodPic);
            o.addProperty("vodName", h.vodName);
            o.addProperty("vodFlag", String.valueOf(h.playFlag));
            o.addProperty("vodRemarks", h.episodeName);
            o.addProperty("position", h.position);
            o.addProperty("duration", h.duration);
            o.addProperty("createTime", h.updateTime);
            arr.add(o);
        }
        return arr.toString();
    }

    // ===== cache =====

    private String cacheSet(JsonObject payload) {
        com.orhanobut.hawk.Hawk.put(cacheKey(payload), safe(payload, "value"));
        return "{}";
    }

    private String cacheDel(JsonObject payload) {
        com.orhanobut.hawk.Hawk.delete(cacheKey(payload));
        return "{}";
    }

    private String cacheKey(JsonObject payload) {
        String rule = safe(payload, "rule");
        String key = safe(payload, "key");
        return "cache_" + (TextUtils.isEmpty(rule) ? "" : rule + "_") + key;
    }

    // ===== info =====

    private String device() {
        JsonObject object = new JsonObject();
        object.addProperty("uuid", android.provider.Settings.Secure.getString(
                activity.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));
        object.addProperty("name", android.os.Build.MODEL);
        object.addProperty("ip", WebServer.get().getAddress(""));
        object.addProperty("type", 1);
        object.addProperty("time", System.currentTimeMillis());
        return object.toString();
    }

    private String site() {
        com.yingtu.plus.bean.SourceBean s = com.yingtu.plus.api.ApiConfig.get().getHomeSource();
        JsonObject object = new JsonObject();
        object.addProperty("key", s != null ? s.getKey() : "");
        object.addProperty("name", s != null ? s.getName() : "");
        object.addProperty("type", s != null ? s.getType() : 1);
        return object.toString();
    }

    private String config() {
        JsonObject object = new JsonObject();
        object.addProperty("id", 0);
        object.addProperty("url", com.orhanobut.hawk.Hawk.get(
                com.yingtu.plus.util.HawkConfig.API_URL, ""));
        object.addProperty("desc", "影图+");
        object.addProperty("driveCheck", false);
        return object.toString();
    }

    private String extInfo() {
        JsonObject object = new JsonObject();
        object.addProperty("enabled", false);
        object.addProperty("matched", 0);
        object.addProperty("ready", 0);
        return object.toString();
    }

    private String toast(JsonObject payload) {
        String message = safe(payload, "message");
        if (!TextUtils.isEmpty(message)) post(() ->
                android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show());
        return "{}";
    }

    // ===== ui =====

    private String setToolbar(JsonObject payload) {
        boolean visible = !payload.has("visible") || payload.get("visible").getAsBoolean();
        post(() -> ui.setToolbar(visible));
        return "{}";
    }

    private String setChrome(JsonObject payload) {
        post(() -> ui.setChrome(payload));
        return "{}";
    }

    // ===== resolve / reject =====

    private void resolve(String requestId, String data) {
        String payload = TextUtils.isEmpty(data) ? "null" : data;
        if (payload.length() > INLINE_LIMIT) {
            String resultId = requestId + "_" + System.nanoTime();
            results.put(resultId, payload);
            payload = "{\"__fmResultId\":" + quote(resultId) + "}";
        }
        eval("window.fongmiNative&&window.fongmiNative.resolve(" + quote(requestId) + "," + payload + ")");
    }

    private void reject(String requestId, String error) {
        eval("window.fongmiNative&&window.fongmiNative.reject(" + quote(requestId) + "," + quote(error) + ")");
    }

    private void eval(String script) {
        post(() -> webView.evaluateJavascript(script, null));
    }

    private void post(Runnable r) {
        activity.runOnUiThread(r);
    }

    private static void OkGoExec(Runnable r) {
        com.yingtu.plus.util.OkGo.io(r);
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            return value == null ? "" : value;
        }
    }

    private static String safe(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString() : "";
    }

    private String quote(String text) {
        return gson.toJson(text == null ? "" : text);
    }
}
