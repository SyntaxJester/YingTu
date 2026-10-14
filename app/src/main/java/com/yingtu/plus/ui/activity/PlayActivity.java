package com.yingtu.plus.ui.activity;

import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yingtu.plus.R;
import com.yingtu.plus.api.ApiConfig;
import com.yingtu.plus.api.VodSpider;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.bean.History;
import com.yingtu.plus.bean.ParseBean;
import com.yingtu.plus.bean.SourceBean;
import com.yingtu.plus.bean.VodInfo;
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

import java.io.Serializable;
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
    public static final String KEY_SERIES = "seriesList"; // 当前线路全部集数

    private static final float[] SPEEDS = {1.0f, 1.25f, 1.5f, 2.0f, 0.75f};

    private SurfaceView surfaceView;
    private TextView tvTitle, tvPosition, tvDuration, btnSpeed, btnCore;
private String vodName;
    private ImageView btnPlayPause, ivCenterPlay, btnPrev, btnNext, btnDanmaku, btnFullscreen;
    private SeekBar seekBar;
    private View loading, centerControls, topControlBar, controlBar;
    private Handler hideHandler = new Handler(Looper.getMainLooper());
    private Runnable hideControlsRunnable;

    private AbstractPlayer player;
    private ParseJob parseJob;
    private History history;
    private long resumePosition = 0;
    private int speedIndex = 0;
    private boolean dragging = false;
    private boolean prepared = false;
    private boolean isPortraitVideo = false;
private boolean controlsVisible = true;

    // 当前解析出的播放参数（用于内核热切换时重放）
    private String playUrl;
    private Map<String, String> playHeaders = new HashMap<>();

    // 上下集
    private List<VodInfo.VodSeries> seriesList;
    private int currentEpisodeIndex = 0;

    // OSD
    private View osdView;
    private TextView osdContent;
    private ImageView osdIcon;
    private View osdHintLayout;
    private LinearLayout osdTopLeft, osdTopRight, osdBottomLeft, osdBottomRight;
    private Handler osdHideHandler;
    private AudioManager audioManager;
    private float gestureStartY = 0;
    private boolean isBrightnessGesture = false;

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
        surfaceView = find(R.id.surfaceView);
        tvTitle = find(R.id.tvTitle);
        tvPosition = find(R.id.tvPosition);
        tvDuration = find(R.id.tvDuration);
        btnSpeed = find(R.id.btnSpeed);
        btnCore = find(R.id.btnCore);
        btnPlayPause = find(R.id.btnPlayPause);
        seekBar = find(R.id.seekBar);
        loading = find(R.id.loading);
        
        // 新控制栏
        topControlBar = find(R.id.topControlBar);
        controlBar = find(R.id.controlBar);
        centerControls = find(R.id.centerControls);
        ivCenterPlay = find(R.id.ivCenterPlay);
        btnPrev = find(R.id.btnPrev);
        btnNext = find(R.id.btnNext);
        btnDanmaku = find(R.id.btnDanmaku);
        btnFullscreen = find(R.id.btnFullscreen);

        // OSD 四角显示区域
        osdTopLeft = find(R.id.osdTopLeft);
        osdTopRight = find(R.id.osdTopRight);
        osdBottomLeft = find(R.id.osdBottomLeft);
        osdBottomRight = find(R.id.osdBottomRight);

        String title = getIntent().getStringExtra(KEY_TITLE);
        tvTitle.setText(title == null ? "" : title);
        vodName = title;
        resumePosition = getIntent().getLongExtra(KEY_POSITION, 0);

        // 获取当前线路全部集数，用于上下集切换
        seriesList = (List<VodInfo.VodSeries>) getIntent().getSerializableExtra(KEY_SERIES);
        currentEpisodeIndex = getIntent().getIntExtra(KEY_INDEX, 0);
        if (seriesList != null && !seriesList.isEmpty()) {
            if (btnPrev != null) btnPrev.setEnabled(currentEpisodeIndex > 0);
            if (btnNext != null) btnNext.setEnabled(currentEpisodeIndex < seriesList.size() - 1);
        }

        // 点击事件
        find(R.id.ivBack).setOnClickListener(v -> finish());
        find(R.id.ivBackOld).setOnClickListener(v -> finish());
        btnPlayPause.setOnClickListener(v -> togglePlay());
        if (ivCenterPlay != null) ivCenterPlay.setOnClickListener(v -> togglePlay());
        if (btnPrev != null) btnPrev.setOnClickListener(v -> playPreviousEpisode());
        if (btnNext != null) btnNext.setOnClickListener(v -> playNextEpisode());
        if (btnDanmaku != null) btnDanmaku.setOnClickListener(v -> toggleDanmaku());
        if (btnFullscreen != null) btnFullscreen.setOnClickListener(v -> toggleFullscreen());
        btnSpeed.setOnClickListener(v -> cycleSpeed());
        btnCore.setOnClickListener(v -> switchCore());
        btnCore.setText(PlayerFactory.coreName(PlayerFactory.currentCore()));

        // 点击屏幕任意位置显示/隐藏控制层
        findViewById(R.id.surfaceContainer).setOnClickListener(v -> toggleControls());
        
        // 手势：左半屏上下滑调亮度，右半屏上下滑调音量
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int screenHeight = wm.getDefaultDisplay().getHeight();
        findViewById(R.id.surfaceContainer).setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                showControls();
                gestureStartY = event.getRawY();
                float x = event.getRawX();
                isBrightnessGesture = x < screenHeight * 0.5f ? false : true; // 左屏音量，右屏亮度
                return true;
            }
            if (action == android.view.MotionEvent.ACTION_MOVE && !dragging) {
                float deltaY = gestureStartY - event.getRawY();
                if (Math.abs(deltaY) < 5) return true;
                if (isBrightnessGesture) {
                    float cur = getWindow().getAttributes().screenBrightness;
                    if (cur < 0) cur = 0.5f;
                    float newB = Math.max(0.01f, Math.min(1.0f, cur + deltaY / screenHeight));
                    setBrightness(newB);
                    String hint = "亮度 " + (int) (newB * 100) + "%";
                    showOsdBottomRight(hint);
                    gestureStartY = event.getRawY();
                } else {
                    int curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int deltaVol = (int) (deltaY / screenHeight * maxVolume);
                    int newVol = Math.max(0, Math.min(maxVolume, curVol + deltaVol));
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
                    String hint = "音量 " + newVol + "/" + maxVolume;
                    showOsdBottomRight(hint);
                    gestureStartY = event.getRawY();
                }
                return true;
            }
            return false;
        });

        // 进度条拖动时显示快进/快退 OSD 提示
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean isSeek = false;

            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser && isSeek && player != null) {
                    tvPosition.setText(formatTime(p));
                    showOsdHint(formatSeekHint(p));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {
                isSeek = true;
                dragging = true;
                hideControls();
            }
            @Override public void onStopTrackingTouch(SeekBar s) {
                isSeek = false;
                dragging = false;
                if (player != null) player.seekTo(s.getProgress());
                showControls();
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
        
        // 初始显示控制层
        showControls();
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
        adjustOrientation(width, height);
    }

    /** 根据视频宽高比自动调整屏幕方向：竖屏短剧用竖屏，横屏影视用横屏 */
    private void adjustOrientation(int width, int height) {
        if (width <= 0 || height <= 0) return;
        float aspectRatio = (float) width / height;
        boolean wasPortrait = isPortraitVideo;
        
        // 宽高比 < 0.8 认为是竖屏视频（如 9:16 ≈ 0.56，720x1280 ≈ 0.56）
        isPortraitVideo = aspectRatio < 0.8f;
        
        if (wasPortrait == isPortraitVideo) return; // 方向没变，跳过
        
        int newOrientation;
        if (isPortraitVideo) {
            newOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        } else {
            newOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        }
        
        try {
            setRequestedOrientation(newOrientation);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /** 播放上一集 */
    private void playPreviousEpisode() {
        if (seriesList == null || seriesList.isEmpty()) return;
        int idx = currentEpisodeIndex;
        if (idx <= 0) return;
        currentEpisodeIndex = idx - 1;
        playEpisodeAt(currentEpisodeIndex);
    }

    /** 播放下一集 */
    private void playNextEpisode() {
        if (seriesList == null || seriesList.isEmpty()) return;
        int idx = currentEpisodeIndex;
        if (idx >= seriesList.size() - 1) return;
        currentEpisodeIndex = idx + 1;
        playEpisodeAt(currentEpisodeIndex);
    }

    private void playEpisodeAt(int index) {
        VodInfo.VodSeries s = seriesList.get(index);
        String title = vodName + " " + s.name;
        if (btnPrev != null) btnPrev.setEnabled(index > 0);
        if (btnNext != null) btnNext.setEnabled(index < seriesList.size() - 1);
        tvTitle.setText(title);
        releasePlayer();
        createPlayer();
        resolveAndPlay(s.url);
    }

    /** 切换控制层显示/隐藏 */
    private void toggleControls() {
        if (controlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    /** 显示控制层 */
    private void showControls() {
        controlsVisible = true;
        if (topControlBar != null) topControlBar.setVisibility(View.VISIBLE);
        if (controlBar != null) controlBar.setVisibility(View.VISIBLE);
        if (centerControls != null) centerControls.setVisibility(View.VISIBLE);
        
        // 3 秒后自动隐藏
        hideHandler.removeCallbacks(hideControlsRunnable);
        hideControlsRunnable = this::hideControls;
        hideHandler.postDelayed(hideControlsRunnable, 3000);
    }

    /** 隐藏控制层 */
    private void hideControls() {
        controlsVisible = false;
        hideHandler.removeCallbacks(hideControlsRunnable);
        if (topControlBar != null) topControlBar.setVisibility(View.GONE);
        if (controlBar != null) controlBar.setVisibility(View.GONE);
        if (centerControls != null) centerControls.setVisibility(View.GONE);
    }

    /** 显示 OSD 提示（快进/快退/音量/亮度） */
    private void showOsdHint(String text) {
        if (osdView == null) {
            osdView = LayoutInflater.from(this).inflate(R.layout.view_osd, null);
            // 使用已有的 seekHint 布局显示快进/快退提示
            View seekHint = osdView.findViewById(R.id.seekHint);
            TextView seekTime = osdView.findViewById(R.id.seekTime);
            ImageView seekIcon = osdView.findViewById(R.id.seekIcon);
            osdContent = seekTime;
            osdIcon = seekIcon;
            osdHintLayout = seekHint;
            ((ViewGroup) findViewById(R.id.surfaceContainer)).addView(osdView);
        }
        if (text != null && text.startsWith("快退")) {
            if (osdIcon != null) osdIcon.setImageResource(R.drawable.ic_fast_rewind);
        } else if (osdIcon != null) {
            osdIcon.setImageResource(R.drawable.ic_fast_forward);
        }
        osdContent.setText(text);
        if (osdHintLayout != null) {
            osdHintLayout.setVisibility(View.VISIBLE);
        }
        if (osdHideHandler == null) osdHideHandler = new Handler(Looper.getMainLooper());
        osdHideHandler.removeCallbacksAndMessages(null);
        osdHideHandler.postDelayed(() -> {
            if (osdHintLayout != null) osdHintLayout.setVisibility(View.GONE);
        }, 1200);
    }

    private void setBrightness(float b) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = b;
        getWindow().setAttributes(lp);
    }

    private void showOsdBottomRight(String text) {
        if (osdBottomRight != null) {
            TextView tv = osdBottomRight.findViewById(R.id.tvOsdBottomRight);
            if (tv != null) tv.setText(text);
            osdBottomRight.setVisibility(View.VISIBLE);
            if (osdHideHandler == null) osdHideHandler = new Handler(Looper.getMainLooper());
            osdHideHandler.removeCallbacksAndMessages(null);
            osdHideHandler.postDelayed(() -> osdBottomRight.setVisibility(View.GONE), 1200);
        }
    }

    private int dp2px(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String formatTime(long ms) {
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }

    private String formatSeekHint(int progress) {
        long delta = progress - (player != null ? player.getCurrentPosition() : 0);
        String sign = delta >= 0 ? "快进" : "快退";
        return sign + " " + formatTime(Math.abs(delta));
    }

    /** 切换全屏模式 */
    private void toggleFullscreen() {
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // 当前是横屏，切换到竖屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else {
            // 当前是竖屏，切换到横屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    /** 切换弹幕开关（预留） */
    private void toggleDanmaku() {
        Toast.makeText(this, "弹幕功能预留", Toast.LENGTH_SHORT).show();
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
        hideHandler.removeCallbacks(hideControlsRunnable);
        if (player != null && player.isPlaying()) {
            player.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
        saveProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (controlsVisible) {
            showControls();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideHandler.removeCallbacks(hideControlsRunnable);
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
}
