package com.yingtu.plus.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.yingtu.plus.ui.custom.PlayerGestureController;
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
 * 播放页（B+ 方案：webhtv 交互层移植）。
 *
 * UI 层移植自 webhtv/webhtv：
 *  - 中央手势反馈小组件（seek/bright/volume/speed 四个 widget，view_widget_player.xml）
 *  - 手势状态机 PlayerGestureController（左滑快退右滑快进 / 左右半屏上下滑亮度音量 /
 *    长按倍速 / 双指捏合缩放 / 双击暂停 / 快速上滑下一集 / 快速下滑上一集）
 *  - PeekPro 风格中间控制组（上一集/播放暂停/下一集）
 *  - 顶部控制栏（返回/标题/内核切换/全屏）+ 底部控制条（播放/进度/倍速）
 *
 * 播放内核保留 ExoPlayer（Media3 1.2.1，经 AbstractPlayer 统一接口驱动），
 * 不引入 webhtv 的 media3 fork / MPV / IJK so。
 */
public class PlayActivity extends BaseActivity
        implements AbstractPlayer.Listener, PlayerGestureController.Listener {

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
    private static final long HIDE_CONTROLS_MS = 5000;
    private static final long HIDE_WIDGET_MS = 1200;

    private SurfaceView surfaceView;
    private TextView tvTitle, tvPosition, tvDuration, btnSpeed, btnCore;
    private ImageView btnPlayPause, ivCenterPlay, btnPrev, btnNext, btnDanmaku, btnFullscreen;
    private SeekBar seekBar;
    private View loading, centerControls, topControlBar, controlBar;

    // 手势反馈小组件（webhtv widget 布局）
    private View seekWidget, brightWidget, volumeWidget, speedWidget;
    private TextView seekTime;
    private ImageView seekIcon, brightIcon, volumeIcon;
    private ProgressBar brightProgress, volumeProgress;
    private Handler osdHideHandler;
    private String toastOsd;

    // 四角 OSD 信息面板（webhtv 风格）
    private View osdTopLeft, osdTopRight, osdBottomLeft, osdBottomRight;
    private TextView tvOsdTitle, tvOsdDecoder, tvOsdSpeed, tvOsdCore;
    private TextView tvOsdSeries, tvOsdTime, tvOsdTotal;

    // 手势
    private PlayerGestureController gesture;
    private long seekBasePosition;

    private AbstractPlayer player;
    private ParseJob parseJob;
    private History history;
    private long resumePosition = 0;
    private int speedIndex = 0;
    private boolean dragging = false;
    private boolean controlsVisible = true;

    // 当前解析出的播放参数（内核热切换时重放）
    private String playUrl;
    private Map<String, String> playHeaders = new HashMap<>();

    // 上下集
    private List<VodInfo.VodSeries> seriesList;
    private int currentEpisodeIndex = 0;
    private String vodName;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            ui.postDelayed(this, 500);
        }
    };

    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private Runnable hideControlsRunnable;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_play;
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
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

        topControlBar = find(R.id.topControlBar);
        controlBar = find(R.id.controlBar);
        centerControls = find(R.id.centerControls);
        ivCenterPlay = find(R.id.ivCenterPlay);
        btnPrev = find(R.id.btnPrev);
        btnNext = find(R.id.btnNext);
        btnDanmaku = find(R.id.btnDanmaku);
        btnFullscreen = find(R.id.btnFullscreen);

        // ===== 手势反馈小组件（webhtv widget 布局）=====
        seekWidget = find(R.id.seekWidget);
        brightWidget = find(R.id.brightWidget);
        volumeWidget = find(R.id.volumeWidget);
        speedWidget = find(R.id.speedWidget);
        seekTime = find(R.id.seekTime);
        seekIcon = find(R.id.seekIcon);
        brightIcon = find(R.id.brightIcon);
        volumeIcon = find(R.id.volumeIcon);
        brightProgress = find(R.id.brightProgress);
        volumeProgress = find(R.id.volumeProgress);

        // ===== 四角 OSD 信息面板（webhtv 风格）=====
        osdTopLeft = find(R.id.osdTopLeft);
        osdTopRight = find(R.id.osdTopRight);
        osdBottomLeft = find(R.id.osdBottomLeft);
        osdBottomRight = find(R.id.osdBottomRight);
        tvOsdTitle = find(R.id.tvOsdTitle);
        tvOsdDecoder = find(R.id.tvOsdDecoder);
        tvOsdSpeed = find(R.id.tvOsdSpeed);
        tvOsdCore = find(R.id.tvOsdCore);
        tvOsdSeries = find(R.id.tvOsdSeries);
        tvOsdTime = find(R.id.tvOsdTime);
        tvOsdTotal = find(R.id.tvOsdTotal);
        // 立即填充四角 OSD 内容（不依赖 player 就绪）
        fillOsdNow();

        String title = getIntent().getStringExtra(KEY_TITLE);
        tvTitle.setText(title == null ? "" : title);
        vodName = title;
        resumePosition = getIntent().getLongExtra(KEY_POSITION, 0);

        // 当前线路全部集数（上下集切换）
        seriesList = (List<VodInfo.VodSeries>) getIntent().getSerializableExtra(KEY_SERIES);
        currentEpisodeIndex = getIntent().getIntExtra(KEY_INDEX, 0);
        if (seriesList != null && !seriesList.isEmpty()) {
            btnPrev.setEnabled(currentEpisodeIndex > 0);
            btnNext.setEnabled(currentEpisodeIndex < seriesList.size() - 1);
        }

        // ===== 点击事件 =====
        find(R.id.ivBack).setOnClickListener(v -> finish());
        btnPlayPause.setOnClickListener(v -> togglePlay());
        ivCenterPlay.setOnClickListener(v -> togglePlay());
        btnPrev.setOnClickListener(v -> playPreviousEpisode());
        btnNext.setOnClickListener(v -> playNextEpisode());
        if (btnDanmaku != null) btnDanmaku.setOnClickListener(v -> toggleDanmaku());
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
        btnSpeed.setOnClickListener(v -> cycleSpeed());
        btnCore.setOnClickListener(v -> switchCore());
        btnCore.setText(PlayerFactory.coreName(PlayerFactory.currentCore()));

        // ===== 手势（webhtv CustomKeyDown 移植版）=====
        View gestureLayer = find(R.id.surfaceContainer);
        gesture = PlayerGestureController.create(this, surfaceView);
        gestureLayer.setOnTouchListener((v, event) -> {
            showControls();
            return gesture.onTouchEvent(event);
        });

        // 进度条拖动（与手势共存：拖动时屏蔽手势 seek 提示）
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean isSeek = false;

            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) {
                    tvPosition.setText(formatTime(p));
                    if (isSeek && player != null) showSeekWidget(player.getCurrentPosition(), p, false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
                isSeek = true;
                dragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                isSeek = false;
                dragging = false;
                if (player != null) player.seekTo(s.getProgress());
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (player != null) player.setSurface(holder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder h, int f, int w, int ht) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (player != null) player.setSurface(null);
            }
        });

        osdHideHandler = new Handler(Looper.getMainLooper());

        buildHistory();
        createPlayer();
        resolveAndPlay(getIntent().getStringExtra(KEY_URL));

        showControls();
    }

    // ================= 历史 =================

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

    // ================= 播放内核 =================

    private void createPlayer() {
        player = PlayerFactory.create(this);
        player.setListener(this);
        if (surfaceView.getHolder().getSurface() != null && surfaceView.getHolder().getSurface().isValid()) {
            player.setSurface(surfaceView.getHolder().getSurface());
        }
    }

    // ================= 解析链路（CSP spider → 二次解析）=================

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
            String u = episodeUrl;
            Map<String, String> headers = new HashMap<>();
            boolean needParse = false;
            try {
                Spider spider = VodSpider.get(source);
                List<String> vipFlags = new ArrayList<>();
                String json = spider.playerContent(flag, episodeUrl, vipFlags);
                if (json != null && json.trim().startsWith("{")) {
                    JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                    String du = optString(o, "url");
                    if (!du.isEmpty()) u = du;
                    if (o.has("parse") && !o.get("parse").isJsonNull()) {
                        try {
                            needParse = o.get("parse").getAsInt() == 1;
                        } catch (Throwable ignored) {
                        }
                    }
                    if (o.has("header")) headers.putAll(parseHeader(o.get("header")));
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            final String fUrl = u;
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
        else startPlay(url, headers);
    }

    private void runParse(String url, Map<String, String> baseHeaders) {
        loading.setVisibility(View.VISIBLE);
        ParseBean parse = ApiConfig.get().getDefaultParse();
        parseJob = ParseJob.create(new ParseJob.Callback() {
            @Override
            public void onParseSuccess(Map<String, String> headers, String directUrl) {
                Map<String, String> merged = new HashMap<>(baseHeaders);
                if (headers != null) merged.putAll(headers);
                startPlay(directUrl, merged);
            }

            @Override
            public void onParseError() {
                loading.setVisibility(View.GONE);
                Toast.makeText(PlayActivity.this, "解析失败，该资源可能需要专用解析接口", Toast.LENGTH_LONG).show();
            }
        }).start(parse, url);
    }

    // ================= 播放控制 =================

    private void startPlay(String url, Map<String, String> headers) {
        if (isFinishing() || player == null) return;
        if (url == null || url.isEmpty()) {
            loading.setVisibility(View.GONE);
            Toast.makeText(this, "未取得可播放地址", Toast.LENGTH_SHORT).show();
            return;
        }
        this.playUrl = url;
        this.playHeaders = headers == null ? new HashMap<>() : headers;
        loading.setVisibility(View.VISIBLE);
        player.setDataSource(url, this.playHeaders);
        if (surfaceView.getHolder().getSurface() != null) player.setSurface(surfaceView.getHolder().getSurface());
        player.prepare();
    }

    private void togglePlay() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
            setPlayIcon(false);
        } else {
            player.start();
            setPlayIcon(true);
        }
    }

    private void setPlayIcon(boolean playing) {
        btnPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        ivCenterPlay.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void cycleSpeed() {
        speedIndex = (speedIndex + 1) % SPEEDS.length;
        float sp = SPEEDS[speedIndex];
        if (player != null) player.setSpeed(sp);
        btnSpeed.setText(sp + "x");
        showWidgetHint(speedWidget, HIDE_WIDGET_MS);
    }

    /** 热切换内核：释放当前内核，用新内核从当前进度重放。 */
    private void switchCore() {
        int next = (PlayerFactory.currentCore() + 1) % 3;
        PlayerFactory.setCore(next);
        btnCore.setText(PlayerFactory.coreName(next));
        Toast.makeText(this, "已切换内核：" + PlayerFactory.coreName(next), Toast.LENGTH_SHORT).show();

        long pos = player != null ? player.getCurrentPosition() : 0;
        if (pos > 0) resumePosition = pos;
        releasePlayer();
        createPlayer();
        if (playUrl != null) startPlay(playUrl, playHeaders);
    }

    private void updateProgress() {
        if (player == null || dragging) return;
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        if (dur > 0) {
            seekBar.setMax((int) dur);
            seekBar.setProgress((int) pos);
            tvPosition.setText(formatTime(pos));
            tvDuration.setText(formatTime(dur));
            refreshOsd(pos, dur);
        }
    }

    /** 立即填充四角 OSD（不依赖 player/duration，init 即可用）。 */
    private void fillOsdNow() {
        if (tvOsdTitle != null && vodName != null) tvOsdTitle.setText(vodName);
        if (tvOsdDecoder != null) tvOsdDecoder.setText("硬解 · " + PlayerFactory.coreName(PlayerFactory.currentCore()));
        if (tvOsdSpeed != null) tvOsdSpeed.setText(String.format(Locale.getDefault(), "%.2fx", SPEEDS[speedIndex]));
        if (tvOsdCore != null) tvOsdCore.setText(PlayerFactory.coreName(PlayerFactory.currentCore()));
        if (tvOsdSeries != null && seriesList != null && !seriesList.isEmpty()) {
            VodInfo.VodSeries s = seriesList.get(Math.min(currentEpisodeIndex, seriesList.size() - 1));
            tvOsdSeries.setText(String.format(Locale.getDefault(), "%s (%d/%d)",
                    s.name, currentEpisodeIndex + 1, seriesList.size()));
        }
        if (tvOsdTime != null) tvOsdTime.setText("00:00");
        if (tvOsdTotal != null) tvOsdTotal.setText("--:--");
        if (osdTopLeft != null) osdTopLeft.setVisibility(View.VISIBLE);
        if (osdTopRight != null) osdTopRight.setVisibility(View.VISIBLE);
        if (osdBottomLeft != null) osdBottomLeft.setVisibility(View.VISIBLE);
        if (osdBottomRight != null) osdBottomRight.setVisibility(View.VISIBLE);
    }

    /**
     * 刷新四角 OSD 内容（webhtv 风格）
     * 左上：片名 + 解码信息；右上：倍速 + 内核；左下：集数；右下：时间/总时长
     */
    private void refreshOsd(long pos, long dur) {
        if (tvOsdTitle != null) tvOsdTitle.setText(vodName == null ? "" : vodName);
        if (tvOsdDecoder != null) tvOsdDecoder.setText("硬解 · " + PlayerFactory.coreName(PlayerFactory.currentCore()));
        if (tvOsdSpeed != null) tvOsdSpeed.setText(String.format(Locale.getDefault(), "%.2fx", SPEEDS[speedIndex]));
        if (tvOsdCore != null) tvOsdCore.setText(PlayerFactory.coreName(PlayerFactory.currentCore()));
        if (tvOsdSeries != null && seriesList != null && !seriesList.isEmpty()) {
            VodInfo.VodSeries s = seriesList.get(Math.min(currentEpisodeIndex, seriesList.size() - 1));
            tvOsdSeries.setText(String.format(Locale.getDefault(), "%s (%d/%d)",
                    s.name, currentEpisodeIndex + 1, seriesList.size()));
        }
        if (tvOsdTime != null) tvOsdTime.setText(formatTime(pos));
        if (tvOsdTotal != null) tvOsdTotal.setText(dur > 0 ? formatTime(dur) : "--:--");
        // 确保四个面板可见（不受控制栏显隐影响）
        if (osdTopLeft != null) osdTopLeft.setVisibility(View.VISIBLE);
        if (osdTopRight != null) osdTopRight.setVisibility(View.VISIBLE);
        if (osdBottomLeft != null) osdBottomLeft.setVisibility(View.VISIBLE);
        if (osdBottomRight != null) osdBottomRight.setVisibility(View.VISIBLE);
    }

    // ================= 手势反馈（PlayerGestureController.Listener）=================

    @Override
    public void onSeeking(long time) {
        if (player == null) return;
        long base = seekBasePosition;
        if (base == 0) {
            base = player.getCurrentPosition();
            seekBasePosition = base;
        }
        long target = Math.max(0, base + time);
        showSeekWidget(base, target, true);
    }

    @Override
    public void onSeekEnd(long time) {
        if (player == null) return;
        long base = seekBasePosition;
        long target = Math.max(0, base + time);
        seekBasePosition = 0;
        if (target > 0 && player.getDuration() > 0) {
            player.seekTo(Math.min(target, player.getDuration()));
        }
        hideWidget(seekWidget);
    }

    @Override
    public void onBright(int progress) {
        if (brightWidget == null) return;
        brightProgress.setProgress(progress);
        if (progress >= 60) {
            brightIcon.setImageResource(R.drawable.ic_brightness_high);
        } else {
            brightIcon.setImageResource(R.drawable.ic_brightness);
        }
        showWidgetHint(brightWidget, HIDE_WIDGET_MS);
    }

    @Override
    public void onBrightEnd(float value) {
    }

    @Override
    public void onVolume(int progress) {
        if (volumeWidget == null) return;
        volumeProgress.setProgress(progress);
        if (progress >= 60) {
            volumeIcon.setImageResource(R.drawable.ic_volume_high);
        } else {
            volumeIcon.setImageResource(R.drawable.ic_volume_up);
        }
        showWidgetHint(volumeWidget, HIDE_WIDGET_MS);
    }

    @Override
    public void onSpeedUp() {
        if (player != null) {
            player.setSpeed(2.0f);
        }
        showWidgetHint(speedWidget, HIDE_WIDGET_MS);
    }

    @Override
    public void onSpeedEnd() {
        if (player != null) {
            player.setSpeed(SPEEDS[speedIndex]);
        }
        hideWidget(speedWidget);
    }

    @Override
    public void onFlingUp() {
        playNextEpisode();
    }

    @Override
    public void onFlingDown() {
        playPreviousEpisode();
    }

    @Override
    public void onSingleTap() {
        toggleControls();
    }

    @Override
    public void onDoubleTap() {
        togglePlay();
    }

    @Override
    public void onTouchEnd() {
        seekBasePosition = 0;
    }

    // ================= 上下集 =================

    private void playPreviousEpisode() {
        if (seriesList == null || seriesList.isEmpty()) return;
        int idx = currentEpisodeIndex;
        if (idx <= 0) return;
        currentEpisodeIndex = idx - 1;
        playEpisodeAt(currentEpisodeIndex);
    }

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
        btnPrev.setEnabled(index > 0);
        btnNext.setEnabled(index < seriesList.size() - 1);
        tvTitle.setText(title);
        releasePlayer();
        createPlayer();
        resolveAndPlay(s.url);
    }

    // ================= 控制层显隐 =================

    private void toggleControls() {
        if (controlsVisible) hideControls();
        else showControls();
    }

    private void showControls() {
        controlsVisible = true;
        topControlBar.setVisibility(View.VISIBLE);
        controlBar.setVisibility(View.VISIBLE);
        centerControls.setVisibility(View.VISIBLE);
        hideHandler.removeCallbacks(hideControlsRunnable);
        hideControlsRunnable = () -> {
            if (controlsVisible) hideControls();
        };
        hideHandler.postDelayed(hideControlsRunnable, HIDE_CONTROLS_MS);
    }

    private void hideControls() {
        controlsVisible = false;
        hideHandler.removeCallbacks(hideControlsRunnable);
        topControlBar.setVisibility(View.GONE);
        controlBar.setVisibility(View.GONE);
        centerControls.setVisibility(View.GONE);
        // 四角 OSD 参考 webhtv：播放期间常驻，不随控制栏隐藏
    }

    // ================= widget 提示 =================

    private void showWidgetHint(View widget, long delay) {
        if (widget == null) return;
        widget.setVisibility(View.VISIBLE);
        osdHideHandler.removeCallbacksAndMessages(null);
        osdHideHandler.postDelayed(() -> {
            if (widget.getVisibility() == View.VISIBLE) widget.setVisibility(View.GONE);
        }, delay);
    }

    private void hideWidget(View widget) {
        if (widget != null) widget.setVisibility(View.GONE);
    }

    private void showSeekWidget(long base, long target, boolean fromGesture) {
        if (seekWidget == null || seekTime == null || seekIcon == null) return;
        long delta = target - base;
        boolean forward = delta >= 0;
        seekIcon.setImageResource(forward ? R.drawable.ic_fast_forward : R.drawable.ic_fast_rewind);
        seekTime.setText(formatTime(Math.abs(delta)));
        seekWidget.setVisibility(View.VISIBLE);
        osdHideHandler.removeCallbacksAndMessages(null);
        if (fromGesture) {
            osdHideHandler.postDelayed(() -> seekWidget.setVisibility(View.GONE), HIDE_WIDGET_MS);
        }
    }

    // ================= 全屏 / 弹幕 =================

    private void toggleFullscreen() {
        int cur = getResources().getConfiguration().orientation;
        setRequestedOrientation(cur == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    private void toggleDanmaku() {
        Toast.makeText(this, "弹幕功能预留", Toast.LENGTH_SHORT).show();
    }

    // ================= AbstractPlayer.Listener =================

    @Override
    public void onPrepared() {
        loading.setVisibility(View.GONE);
        if (resumePosition > 0) player.seekTo(resumePosition);
        player.setSpeed(SPEEDS[speedIndex]);
        player.start();
        setPlayIcon(true);
        // 初始化四角 OSD 内容
        refreshOsd(0, player.getDuration());
        ui.removeCallbacks(ticker);
        ui.post(ticker);
    }

    @Override
    public void onCompletion() {
        setPlayIcon(false);
        saveProgress();
        // 自动连播下一集
        if (seriesList != null && currentEpisodeIndex < seriesList.size() - 1) {
            playNextEpisode();
        }
    }

    @Override
    public void onBuffering(boolean buffering) {
        loading.setVisibility(buffering ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onError(String message) {
        loading.setVisibility(View.GONE);
        Toast.makeText(this, "播放失败：" + message + "（可点右上角切换内核重试）", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        resizeSurface(width, height);
        adjustOrientation(width, height);
    }

    private boolean isPortraitVideo = false;

    private void adjustOrientation(int width, int height) {
        if (width <= 0 || height <= 0) return;
        float aspectRatio = (float) width / height;
        boolean wasPortrait = isPortraitVideo;
        isPortraitVideo = aspectRatio < 0.8f;
        if (wasPortrait == isPortraitVideo) return;
        setRequestedOrientation(isPortraitVideo
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    private void resizeSurface(int vw, int vh) {
        if (vw <= 0 || vh <= 0) return;
        View parent = (View) surfaceView.getParent();
        int pw = parent.getWidth(), ph = parent.getHeight();
        if (pw == 0 || ph == 0) return;
        float ratio = (float) vw / vh;
        int w, h;
        if (pw / (float) ph > ratio) {
            h = ph;
            w = (int) (ph * ratio);
        } else {
            w = pw;
            h = (int) (pw / ratio);
        }
        ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
        lp.width = w;
        lp.height = h;
        surfaceView.setLayoutParams(lp);
    }

    // ================= 生命周期 =================

    @Override
    protected void onPause() {
        super.onPause();
        hideHandler.removeCallbacks(hideControlsRunnable);
        if (player != null && player.isPlaying()) {
            player.pause();
            setPlayIcon(false);
        }
        saveProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showControls();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideHandler.removeCallbacks(hideControlsRunnable);
        osdHideHandler.removeCallbacksAndMessages(null);
        saveProgress();
        if (parseJob != null) {
            parseJob.stop();
            parseJob = null;
        }
        releasePlayer();
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

    // ================= helpers =================

    private static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long s = ms / 1000, m = s / 60, h = m / 60;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m % 60, s % 60);
        return String.format(Locale.US, "%02d:%02d", m, s % 60);
    }

    private static String optString(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static Map<String, String> parseHeader(JsonElement el) {
        Map<String, String> map = new HashMap<>();
        try {
            if (el.isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
                    if (!e.getValue().isJsonNull()) map.put(e.getKey(), e.getValue().getAsString());
                }
            } else if (el.isJsonPrimitive()) {
                JsonObject o = JsonParser.parseString(el.getAsString()).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                    if (!e.getValue().isJsonNull()) map.put(e.getKey(), e.getValue().getAsString());
                }
            }
        } catch (Throwable ignored) {
        }
        return map;
    }
}
