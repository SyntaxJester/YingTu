package com.yingtu.plus.player;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.net.http.SslError;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.yingtu.plus.base.App;
import com.yingtu.plus.util.Sniffer;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 隐藏式 WebView 嗅探器（参考 FongMi/webhtv 的 CustomWebView 精简）。
 *
 * 原理：用一个不显示的 WebView 加载网页播放页，
 * 在 {@link WebViewClient#shouldInterceptRequest} 里拦截所有子请求，
 * 命中视频直链特征（Sniffer.isVideoFormat）即回调成功并带上请求头。
 *
 * 这是“网页解析（type=0）”的核心，用来处理无法直接拿到直链、
 * 需要在浏览器环境里执行 JS 才会发出真实视频请求的站点。
 */
public class WebParser {

    public interface Callback {
        void onResult(Map<String, String> headers, String url);

        void onError();
    }

    private WebView webView;
    private final AtomicReference<Callback> callbackRef = new AtomicReference<>();
    private final Set<String> seen = new LinkedHashSet<>();
    private WebResourceResponse empty;

    public static WebParser create() {
        return new WebParser();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void start(String url, Map<String, String> headers, Callback callback) {
        callbackRef.set(callback);
        empty = new WebResourceResponse("text/plain", "utf-8",
                new ByteArrayInputStream("".getBytes()));

        webView = new WebView(App.context());
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(false);

        // 自定义 UA
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if ("User-Agent".equalsIgnoreCase(e.getKey())) {
                    s.setUserAgentString(e.getValue());
                } else if ("Cookie".equalsIgnoreCase(e.getKey())) {
                    CookieManager.getInstance().setCookie(url, e.getValue());
                }
            }
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String reqUrl = request.getUrl().toString();
                String host = request.getUrl().getHost();
                if (TextUtils.isEmpty(host)) return empty;
                if (Sniffer.isVideoFormat(reqUrl) && seen.add(reqUrl)) {
                    Map<String, String> h = new HashMap<>(request.getRequestHeaders());
                    // 补一个 Referer 便于播放
                    if (!h.containsKey("Referer")) h.put("Referer", view.getUrl() == null ? reqUrl : view.getUrl());
                    deliver(h, reqUrl);
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String pageUrl) {
                super.onPageFinished(view, pageUrl);
                if (pageUrl == null || pageUrl.equals("about:blank")) return;
                // 注入命中域名规则的辅助脚本（如自动点击播放按钮）
                List<String> scripts = Sniffer.getScript(pageUrl);
                for (String js : scripts) {
                    if (!TextUtils.isEmpty(js)) {
                        try {
                            view.evaluateJavascript(js, null);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }

            @Override
            @SuppressLint("WebViewClientOnReceivedSslError")
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        Map<String, String> loadHeaders = headers == null ? new HashMap<>() : headers;
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.loadUrl(url, loadHeaders);
    }

    private void deliver(Map<String, String> headers, String url) {
        Callback cb = callbackRef.getAndSet(null);
        if (cb != null) cb.onResult(headers, url);
    }

    public void destroy() {
        callbackRef.set(null);
        if (webView != null) {
            try {
                webView.stopLoading();
                webView.loadUrl("about:blank");
                webView.removeAllViews();
                webView.destroy();
            } catch (Throwable ignored) {
            }
            webView = null;
        }
    }
}
