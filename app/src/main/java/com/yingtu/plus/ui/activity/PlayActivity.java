package com.yingtu.plus.ui.activity;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.yingtu.plus.R;
import com.yingtu.plus.api.ApiConfig;
import com.yingtu.plus.api.VodSpider;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.bean.History;
import com.yingtu.plus.bean.ParseBean;
import com.yingtu.plus.bean.SourceBean;
import com.yingtu.plus.player.ParseJob;
import com.yingtu.plus.player.core.AbstractPlayer;
import com.yingtu.plus.player.core.PlayerFactory;
import com.yingtu.plus.util.HistoryHelper;
import com.yingtu.plus.util.OkGo;
import com.yingtu.plus.util.Sniffer;
import com.github.catvod.crawler.Spider;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 播放页。支持可切换播放内核（ExoPlayer / IJK / MPV）。
 *
 * 视频统一渲染到 SurfaceView；内核由 {@link PlayerFactory} 按设置创建，
 * 通过 {@link AbstractPlayer} 统一接口驱动，自绘控制条（播放/进度/倍速/内核切换）。
 *
 * 解析链路同前：CSP 站点先 spider.playerContent 取真实直链，再按需二次解析。
 */
public class PlayActivity extends BaseActivity implements AbstractPlayer.Listener {

    public static final String KEY_TITLE = "title";
    public static final String KEY_URL = "url";
    public static final String KEY_SOURCE = "sourceKey";
    public static final String KEY_VOD_ID = "vodId";
    public static final String KEY_VOD_NAME = "vodName";
    public static final String KEY_VOD_PIC = "vodPic";
    public static final String KEY_FLAG = "playFlag";
    public static final String KEY_FLAG_NAME = "playFlagName";
    public static final String KEY_INDEX = "playIndex";
    public static final String KEY_EPISODE = "episodeName";
    public static final String KEY_POSITION = "position";

    private static final float[] SPEEDS = {1.0f, 1.25f, 1.5f, 2.0f, 0.75f};

    private SurfaceView surfaceView;
    private TextView tvTitle, tvPosition, tvDuration, btnSpeed, btnCore;
    private ImageView btnPlayPause;
    private SeekBar seekBar;
    private View loading;
    
    // New PeekPro-style controls
    private View centerPlay, centerCtrls, toolbar, bottomBar, danmakuPanel;
    private View btnBack, btnFullscreen, btnScreenshot, btnDanmaku;
    private View btnPrevEp, btnCenterPlay, btnNextEp;
    private View btnBottomNext, btnBottomPause, btnBottomPrev;
    private View btnBottomDanmaku, btnBottomFullscreen;
    private EditText etDanmaku;
    private SwitchCompat swDanmakuOn;

    private AbstractPlayer player;
    private ParseJob parseJob;
    private History history;
    private long resumePosition = 0;
    private int speedIndex = 0;
    private boolean dragging = false;
    private boolean prepared = false;

    // 当前解析出的播放参数（用于内核热切换时重放）
    private String playUrl;
    private Map<String, String> playHeaders = new HashMap<>();

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            ui.postDelayed(this, 500);
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_play;
    }

    @Override
    protected void init() {
        // Original SurfaceView player (kept working logic)
        surfaceView = find(R.id.surfaceView);
        tvTitle = find(R.id.tvTitle);
        tvPosition = find(R.id.tvPosition);
        tvDuration = find(R.id.tvDuration);
        btnSpeed = find(R.id.btnSpeed);
        btnCore = find(R.id.btnCore);
        btnPlayPause = find(R.id.btnPlayPause);
        seekBar = find(R.id.seekBar);
        loading = find(R.id.loading);

        // New PeekPro-style controls
        centerPlay = find(R.id.centerPlay);
        centerCtrls = find(R.id.centerCtrls);
        toolbar = find(R.id.toolbar);
        bottomBar = find(R.id.bottomBar);
        danmakuPanel = find(R.id.danmakuPanel);
        btnBack = find(R.id.btnBack);
        btnFullscreen = find(R.id.btnFullscreen);
        btnScreenshot = find(R.id.btnScreenshot);
        btnDanmaku = find(R.id.danmaku);
        btnPrevEp = find(R.id.btnPrevEp);
        btnCenterPlay = find(R.id.btnCenterPlay);
        btnNextEp = find(R.id.btnNextEp);
        btnBottomNext = find(R.id.btnBottomNext);
        btnBottomPause = find(R.id.btnBottomPause);
        btnBottomPrev = find(R.id.btnBottomPrev);
        btnBottomDanmaku = find(R.id.btnBottomDanmaku);
        btnBottomFullscreen = find(R.id.btnBottomFullscreen);
        etDanmaku = find(R.id.etDanmaku);
        swDanmakuOn = find(R.id.swDanmakuOn);

        String title = getIntent().getStringExtra(KEY_TITLE);
        tvTitle.setText(title == null ? "" : title);
        resumePosition = getIntent().getLongExtra(KEY_POSITION, 0);

        // Original click handlers
        btnBack.setOnClickListener(v -> finish());
        btnPlayPause.setOnClickListener(v -> togglePlay());
        btnSpeed.setOnClickListener(v -> cycleSpeed());
        btnCore.setOnClickListener(v -> switchCore());
        btnCore.setText(PlayerFactory.coreName(PlayerFactory.currentCore()));
        
        // New PeekPro-style click handlers
        centerPlay.setOnClickListener(v -> togglePlay());
        btnCenterPlay.setOnClickListener(v -> togglePlay());
        btnPrevEp.setOnClickListener(v -> prevEpisode());
        btnNextEp.setOnClickListener(v -> nextEpisode());
        btnBottomPause.setOnClickListener(v -> togglePlay());
        btnBottomPrev.setOnClickListener(v -> prevEpisode());
        btnBottomNext.setOnClickListener(v -> nextEpisode());
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
        btnBottomFullscreen.setOnClickListener(v -> toggleFullscreen());
        btnDanmaku.setOnClickListener(v -> showDanmakuPanel());
        btnBottomDanmaku.setOnClickListener(v -> showDanmakuPanel());
        btnScreenshot.setOnClickListener(v -> takeScreenshot());
        
        // Danmaku panel handlers
        find(R.id.btnDanmakuClose).setOnClickListener(v -> hideDanmakuPanel());
        find(R.id.btnDanmakuSend).setOnClickListener(v -> sendDanmaku());
        swDanmakuOn.setOnCheckedChangeListener((b, c) -> toggleDanmakuEnabled(c));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar s) { dragging = true; }
            @Override public void onStopTrackingTouch(SeekBar s) {
                dragging = false;
                if (player != null) player.seekTo(s.getProgress());
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) {
                if (player != null) player.setSurface(holder.getSurface());
            }
            @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int ht) {}
            @Override public void surfaceDestroyed(SurfaceHolder holder) {
                if (player != null) player.setSurface(null);
            }
        });

        buildHistory();
        createPlayer();
        resolveAndPlay(getIntent().getStringExtra(KEY_URL));
    }

    private void buildHistory() {
        String sourceKey = getIntent().getStringExtra(KEY_SOURCE);
        String vodId = getIntent().getStringExtra(KEY_VOD_ID);
        if (sourceKey == null || vodId == null) return;
        String key = History.buildKey(sourceKey, vodId);
        History old = HistoryHelper.find(key);
        history = old != null ? old : new History();
        history.key = key;
        history.sourceKey = sourceKey;
        history.vodId = vodId;
        history.vodName = getIntent().getStringExtra(KEY_VOD_NAME);
        history.vodPic = getIntent().getStringExtra(KEY_VOD_PIC);
        history.playFlag = getIntent().getIntExtra(KEY_FLAG, 0);
        history.playIndex = getIntent().getIntExtra(KEY_INDEX, 0);
        history.episodeName = getIntent().getStringExtra(KEY_EPISODE);
        HistoryHelper.save(history);
    }

    private void createPlayer() {
        player = PlayerFactory.create(this);
        player.setListener(this);
        if (surfaceView.getHolder().getSurface() != null && surfaceView.getHolder().getSurface().isValid()) {
            player.setSurface(surfaceView.getHolder().getSurface());
        }
    }

    // ===== 解析 =====

    private void resolveAndPlay(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "播放地址为空", Toast.LENGTH_SHORT).show();
            return;
        }
        loading.setVisibility(View.VISIBLE);
        String sourceKey = getIntent().getStringExtra(KEY_SOURCE);
        SourceBean source = sourceKey == null ? null : ApiConfig.get().getSource(sourceKey);
        if (source != null && VodSpider.isCsp(source)) resolveCsp(source, url);
        else afterUrlResolved(url, new HashMap<>());
    }

    private void resolveCsp(SourceBean source, String episodeUrl) {
        String flagName = getIntent().getStringExtra(KEY_FLAG_NAME);
        final String flag = flagName == null ? "" : flagName;
        VodSpider.executor().execute(() -> {
            String url = episodeUrl;
            Map<String, String> headers = new HashMap<>();
            boolean needParse = false;
            try {
                Spider spider = VodSpider.get(source);
                List<String> vipFlags = new ArrayList<>();
                String json = spider.playerContent(flag, episodeUrl, vipFlags);
                if (json != null && json.trim().startsWith("{")) {
                    JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                    String u = optString(o, "url");
                    if (!u.isEmpty()) url = u;
                    if (o.has("parse") && !o.get("parse").isJsonNull()) {
                        try { needParse = o.get("parse").getAsInt() == 1; } catch (Throwable ignored) {}
                    }
                    if (o.has("header")) headers.putAll(parseHeader(o.get("header")));
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            final String fUrl = url;
            final Map<String, String> fHeaders = headers;
            final boolean fParse = needParse;
            OkGo.main(() -> {
                if (fParse || Sniffer.needParse(fUrl)) runParse(fUrl, fHeaders);
                else afterUrlResolved(fUrl, fHeaders);
            });
        });
    }

    private void afterUrlResolved(String url, Map<String, String> headers) {
        if (Sniffer.needParse(url)) runParse(url, headers);
        else playUrl(url, headers);
    }

    private void runParse(String url, Map<String, String> baseHeaders) {
        loading.setVisibility(View.VISIBLE);
        ParseBean parse = ApiConfig.get().getDefaultParse();
        parseJob = ParseJob.create(new ParseJob.Callback() {
            @Override public void onParseSuccess(Map<String, String> headers, String directUrl) {
                Map<String, String> merged = new HashMap<>(baseHeaders);
                if (headers != null) merged.putAll(headers);
                playUrl(directUrl, merged);
            }
            @Override public void onParseError() {
                loading.setVisibility(View.GONE);
                Toast.makeText(PlayActivity.this, "解析失败，该资源可能需要专用解析接口", Toast.LENGTH_LONG).show();
            }
        }).start(parse, url);
    }

    // ===== 播放 =====

    private void playUrl(String url, Map<String, String> headers) {
        if (isFinishing() || player == null) return;
        if (url == null || url.isEmpty()) {
            loading.setVisibility(View.GONE);
            Toast.makeText(this, "未取得可播放地址", Toast.LENGTH_SHORT).show();
            return;
        }
        this.playUrl = url;
        this.playHeaders = headers == null ? new HashMap<>() : headers;
        prepared = false;
        loading.setVisibility(View.VISIBLE);
        player.setDataSource(url, this.playHeaders);
        if (surfaceView.getHolder().getSurface() != null) player.setSurface(surfaceView.getHolder().getSurface());
        player.prepare();
    }

    private void togglePlay() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play);
        } else {
            player.start();
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        }
    }

    private void cycleSpeed() {
        speedIndex = (speedIndex + 1) % SPEEDS.length;
        float sp = SPEEDS[speedIndex];
        if (player != null) player.setSpeed(sp);
        btnSpeed.setText(String.format(Locale.US, "%.2gx", sp).replace("1.0x", "1.0x"));
        btnSpeed.setText(sp + "x");
    }

    /** 热切换内核：释放当前内核，用新内核从当前进度重放。 */
    private void switchCore() {
        int next = (PlayerFactory.currentCore() + 1) % 3;
        PlayerFactory.setCore(next);
        btnCore.setText(PlayerFactory.coreName(next));
        Toast.makeText(this, "已切换内核：" + PlayerFactory.coreName(next), Toast.LENGTH_SHORT).show();

        long pos = player != null ? player.getCurrentPosition() : 0;
        resumePosition = pos > 0 ? pos : resumePosition;
        releasePlayer();
        createPlayer();
        if (playUrl != null) playUrl(playUrl, playHeaders);
    }

    private void updateProgress() {
        if (player == null || dragging) return;
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        if (dur > 0) {
            seekBar.setMax((int) dur);
            seekBar.setProgress((int) pos);
            tvPosition.setText(fmt(pos));
            tvDuration.setText(fmt(dur));
        }
    }

    private static String fmt(long ms) {
        if (ms < 0) ms = 0;
        long s = ms / 1000, m = s / 60, h = m / 60;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m % 60, s % 60);
        return String.format(Locale.US, "%02d:%02d", m, s % 60);
    }

    // ===== AbstractPlayer.Listener =====

    @Override public void onPrepared() {
        prepared = true;
        loading.setVisibility(View.GONE);
        if (resumePosition > 0) player.seekTo(resumePosition);
        player.setSpeed(SPEEDS[speedIndex]);
        player.start();
        btnPlayPause.setImageResource(R.drawable.ic_pause);
        ui.removeCallbacks(ticker);
        ui.post(ticker);
    }

    @Override public void onCompletion() {
        btnPlayPause.setImageResource(R.drawable.ic_play);
    }

    @Override public void onBuffering(boolean buffering) {
        loading.setVisibility(buffering ? View.VISIBLE : View.GONE);
    }

    @Override public void onError(String message) {
        loading.setVisibility(View.GONE);
        Toast.makeText(this, "播放失败：" + message + "（可点右上角切换内核重试）", Toast.LENGTH_LONG).show();
    }

    @Override public void onVideoSizeChanged(int width, int height) {
        resizeSurface(width, height);
    }

    private void resizeSurface(int vw, int vh) {
        if (vw <= 0 || vh <= 0) return;
        View parent = (View) surfaceView.getParent();
        int pw = parent.getWidth(), ph = parent.getHeight();
        if (pw == 0 || ph == 0) return;
        float ratio = (float) vw / vh;
        int w, h;
        if (pw / (float) ph > ratio) { h = ph; w = (int) (ph * ratio); }
        else { w = pw; h = (int) (pw / ratio); }
        android.view.ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
        lp.width = w; lp.height = h;
        surfaceView.setLayoutParams(lp);
    }

    private void saveProgress() {
        if (history == null || player == null) return;
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        if (pos > 0) {
            history.position = pos;
            if (dur > 0) history.duration = dur;
            HistoryHelper.save(history);
        }
    }

    private void releasePlayer() {
        ui.removeCallbacks(ticker);
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {
            player.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
        saveProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveProgress();
        if (parseJob != null) { parseJob.stop(); parseJob = null; }
        releasePlayer();
    }

    // ===== helpers =====

    private static String optString(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static Map<String, String> parseHeader(JsonElement el) {
        Map<String, String> map = new HashMap<>();
        try {
            if (el.isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet())
                    if (!e.getValue().isJsonNull()) map.put(e.getKey(), e.getValue().getAsString());
            } else if (el.isJsonPrimitive()) {
                JsonObject o = JsonParser.parseString(el.getAsString()).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : o.entrySet())
                    if (!e.getValue().isJsonNull()) map.put(e.getKey(), e.getValue().getAsString());
            }
        } catch (Throwable ignored) {
        }
        return map;
    }

    // ===== New PeekPro-style features =====

    /** 上一集 */
    private void prevEpisode() {
        Toast.makeText(this, "上一集", Toast.LENGTH_SHORT).show();
        // TODO: 调用 DetailActivity 的上一集逻辑
    }

    /** 下一集 */
    private void nextEpisode() {
        Toast.makeText(this, "下一集", Toast.LENGTH_SHORT).show();
        // TODO: 调用 DetailActivity 的下一集逻辑
    }

    /** 全屏切换 */
    private void toggleFullscreen() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    /** 显示弹幕面板 */
    private void showDanmakuPanel() {
        danmakuPanel.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.GONE);
    }

    /** 隐藏弹幕面板 */
    private void hideDanmakuPanel() {
        danmakuPanel.setVisibility(View.GONE);
        bottomBar.setVisibility(View.VISIBLE);
    }

    /** 发送弹幕 */
    private void sendDanmaku() {
        String text = etDanmaku.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "请输入弹幕内容", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "弹幕已发送：" + text, Toast.LENGTH_SHORT).show();
        etDanmaku.setText("");
        // TODO: 接入真实的弹幕服务
    }

    /** 切换弹幕开关 */
    private void toggleDanmakuEnabled(boolean enabled) {
        Toast.makeText(this, enabled ? "弹幕已开启" : "弹幕已关闭", Toast.LENGTH_SHORT).show();
        // TODO: 控制弹幕显示/隐藏
    }

    /** 截图 */
    private void takeScreenshot() {
        if (surfaceView == null || surfaceView.getHolder().getSurface() == null) {
            Toast.makeText(this, "无法截图", Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO: 实现截图保存到相册
        Toast.makeText(this, "截图功能开发中", Toast.LENGTH_SHORT).show();
    }
}
