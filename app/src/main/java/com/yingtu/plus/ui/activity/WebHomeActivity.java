package com.yingtu.plus.ui.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.CookieManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yingtu.plus.R;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.web.HomeWebBridge;
import com.yingtu.plus.web.WebHomeSdk;
import com.yingtu.plus.web.WebServer;
import com.google.gson.JsonObject;

/**
 * WebHome 承载页：用内置 WebView 加载一个「网页首页」（远程 URL / 本地 HTML / 内置示例），
 * 并注入 App Native SDK（window.fm / window.fongmi），实现 webhtv 的 WebHome 能力。
 *
 * 这是本次按 https://github.com/webhtv/webhtv 补全的核心功能：
 * 让 HTML 文件（含用户自己的本地 .html）成为可调用原生搜索/播放/网络能力的首页应用。
 */
public class WebHomeActivity extends BaseActivity implements HomeWebBridge.UiCallback {

    public static final String KEY_URL = "webhome_url";
    public static final String KEY_TITLE = "webhome_title";

    private WebView webView;
    private ProgressBar progress;
    private TextView tvTitle;
    private View bar;

    private String chromeMode = "normal";
    private boolean sdkReady = false;

    // 缓存视口度量（在 UI 线程更新），供后台 bridge 线程安全读取
    private volatile int vpWidth = 0;
    private volatile int vpHeight = 0;
    private volatile int vpSafeTop = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_webhome;
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void init() {
        webView = find(R.id.webView);
        progress = find(R.id.progress);
        tvTitle = find(R.id.tvTitle);
        bar = find(R.id.bar);

        String title = getIntent().getStringExtra(KEY_TITLE);
        String url = getIntent().getStringExtra(KEY_URL);
        tvTitle.setText(TextUtils.isEmpty(title) ? "网页首页" : title);

        find(R.id.ivBack).setOnClickListener(v -> handleBackPressed());
        find(R.id.ivReload).setOnClickListener(v -> reload());

        // 确保本地资源网关已启动（fm.res 依赖）
        WebServer.get().start();

        setupWebView();

        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "首页地址为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        webView.loadUrl(url);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // 第三方 Cookie
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cm.setAcceptThirdPartyCookies(webView, true);
        }

        webView.addJavascriptInterface(
                new HomeWebBridge(this, webView, this), WebHomeSdk.BRIDGE);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false; // 让 WebView 自己加载
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectSdk();
                progress.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage msg) {
                android.util.Log.d("webview", msg.message() + " @" + msg.lineNumber());
                return true;
            }
        });
    }

    private void injectSdk() {
        webView.evaluateJavascript(WebHomeSdk.script(isLeanback()), value -> {
            sdkReady = true;
            pushViewport();
        });
    }

    private void pushViewport() {
        vpWidth = px2dp(webView.getWidth());
        vpHeight = px2dp(webView.getHeight());
        vpSafeTop = px2dp(getStatusBarHeight());
        String js = WebHomeSdk.viewportVars(vpWidth, vpHeight, vpSafeTop, 0, vpSafeTop, chromeMode);
        webView.evaluateJavascript(js, null);
    }

    private int px2dp(int px) {
        float d = getResources().getDisplayMetrics().density;
        return d <= 0 ? px : Math.round(px / d);
    }

    private int getStatusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    private boolean isLeanback() {
        return getPackageManager().hasSystemFeature("android.software.leanback");
    }

    private void handleBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }

    // ===== HomeWebBridge.UiCallback =====

    @Override
    public void setToolbar(boolean visible) {
        bar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setChrome(JsonObject payload) {
        String mode = payload != null && payload.has("mode") && !payload.get("mode").isJsonNull()
                ? payload.get("mode").getAsString() : "";
        if (!TextUtils.isEmpty(mode)) {
            chromeMode = mode;
            boolean edge = "edge".equals(mode) || "immersive".equals(mode)
                    || "tv-full".equals(mode) || "fullscreen".equals(mode);
            bar.setVisibility(edge ? View.GONE : View.VISIBLE);
        }
        pushViewport();
    }

    @Override
    public void restoreChrome() {
        chromeMode = "normal";
        bar.setVisibility(View.VISIBLE);
        pushViewport();
    }

    @Override
    public String getViewportJson() {
        // 该方法可能被 bridge 后台线程调用，读缓存值避免跨线程访问 View
        JsonObject o = new JsonObject();
        o.addProperty("webWidth", vpWidth);
        o.addProperty("webHeight", vpHeight);
        o.addProperty("safeTop", vpSafeTop);
        o.addProperty("safeBottom", 0);
        o.addProperty("statusBarHeight", vpSafeTop);
        o.addProperty("chromeMode", chromeMode);
        return o.toString();
    }

    @Override
    public void back() {
        handleBackPressed();
    }

    @Override
    public void reload() {
        progress.setVisibility(View.VISIBLE);
        if (webView != null) {
            webView.clearCache(true);
            webView.reload();
        }
    }

    @Override
    public void openVod() {
        finish();
    }

    @Override
    public void openSetting() {
        startActivity(new android.content.Intent(this, SettingActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            if (sdkReady) {
                webView.evaluateJavascript(
                        WebHomeSdk.dispatchEvent("fmresume", "{\"time\":" + System.currentTimeMillis() + "}"), null);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            if (sdkReady) {
                webView.evaluateJavascript(
                        WebHomeSdk.dispatchEvent("fmpause", "{\"time\":" + System.currentTimeMillis() + "}"), null);
            }
            webView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.removeJavascriptInterface(WebHomeSdk.BRIDGE);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
