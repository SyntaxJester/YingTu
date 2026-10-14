package com.yingtu.plus.ui.activity;

import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

import java.util.HashMap;
import java.util.Map;

/**
 * 播放页 - webhtv 风格 UI
 * 参考：https://github.com/webhtv/webhtv
 */
public class PlayActivity extends BaseActivity implements AbstractPlayer.Listener {
    public static final String KEY_SOURCE = "source";
    public static final String KEY_VOD = "vod";
    public static final String KEY_FLAG = "flag";
    public static final String KEY_INDEX = "index";
    public static final String KEY_TITLE = "title";
    public static final String KEY_URL = "url";
    public static final String KEY_POSITION = "position";

    private FrameLayout videoContainer, centerControls, topControl, bottomControl, osdContainer;
    private ImageView btnPlayPause, btnPrev, btnNext, btnBack, btnDanmaku, btnFullscreen, btnEpisode;
    private TextView tvTitle, tvPosition, tvDuration, tvOsd, btnSpeed, btnCore;
    private SeekBar seekBar;
    private View loading;

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
    private int currentEpisodeIndex = 0;
    private String currentSourceKey = "";
    private String currentVodId = "";

    private static final String[] SPEEDS = {"0.5x", "1.0x", "1.25x", "1.5x", "2.0x"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        init();
    }

    private <T extends View> T find(int id) {
        return findViewById(id);
    }

    @Override
    protected void init() {
        // 视频容器
        videoContainer = find(R.id.videoContainer);
        loading = find(R.id.loading);

        // 顶部控制栏
        topControl = find(R.id.topControl);
        btnBack = find(R.id.ivBack);
        tvTitle = find(R.id.tvTitle);
        btnDanmaku = find(R.id.btnDanmaku);
        btnFullscreen = find(R.id.btnFullscreen);

        // 中间控件
        centerControls = find(R.id.centerControls);
        btnPrev = find(R.id.btnPrev);
        btnPlayPause = find(R.id.centerPlay);
        btnNext = find(R.id.btnNext);

        // OSD 提示
        osdContainer = find(R.id.osdContainer);
        tvOsd = find(R.id.tvOsd);

        // 底部控制栏
        bottomControl = find(R.id.bottomControl);
        btnCore = find(R.id.btnCore);
        btnSpeed = find(R.id.btnSpeed);
        btnEpisode = find(R.id.btnEpisode);
        tvPosition = find(R.id.tvPosition);
        tvDuration = find(R.id.tvDuration);
        seekBar = find(R.id.seekBar);

        // 获取数据
        currentSourceKey = getIntent().getStringExtra(KEY_SOURCE);
        currentVodId = getIntent().getStringExtra(KEY_VOD);
        currentEpisodeIndex = getIntent().getIntExtra(KEY_INDEX, 0);
        String title = getIntent().getStringExtra(KEY_TITLE);
        String url = getIntent().getStringExtra(KEY_URL);
        resumePosition = getIntent().getLongExtra(KEY_POSITION, 0);

        tvTitle.setText(title == null ? "" : title);

        // 点击事件
        btnBack.setOnClickListener(v -> finish());
        btnPlayPause.setOnClickListener(v -> togglePlay());
        btnPrev.setOnClickListener(v -> playPreviousEpisode());
        btnNext.setOnClickListener(v -> playNextEpisode());
        btnDanmaku.setOnClickListener(v -> showOsd("弹幕功能开发中"));
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
        btnSpeed.setOnClickListener(v -> cycleSpeed());
        btnCore.setOnClickListener(v -> switchCore());
        btnEpisode.setOnClickListener(v -> showOsd("选集功能开发中"));

        // 点击屏幕显示/隐藏控制层
        videoContainer.setOnClickListener(v -> toggleControls());
        videoContainer.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                showControls();
            }
            return false;
        });

        // 进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar s) { dragging = true; hideControls(); }
            @Override public void onStopTrackingTouch(SeekBar s) {
                dragging = false;
                if (player != null) player.seekTo(s.getProgress());
                showControls();
            }
        });

        // 初始化播放器
        buildHistory();
        createPlayer();
        resolveAndPlay(url);

        // 初始显示控制层
        showControls();
    }

    private void toggleControls() {
        if (controlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        controlsVisible = true;
        if (topControl != null) topControl.setVisibility(View.VISIBLE);
        if (bottomControl != null) bottomControl.setVisibility(View.VISIBLE);
        if (centerControls != null) centerControls.setVisibility(View.VISIBLE);

        hideHandler.removeCallbacks(hideControlsRunnable);
        hideControlsRunnable = this::hideControls;
        hideHandler.postDelayed(hideControlsRunnable, 4000);
    }

    private void hideControls() {
        controlsVisible = false;
        hideHandler.removeCallbacks(hideControlsRunnable);
        if (topControl != null) topControl.setVisibility(View.GONE);
        if (bottomControl != null) bottomControl.setVisibility(View.GONE);
        if (centerControls != null) centerControls.setVisibility(View.GONE);
    }

    private void showOsd(String text) {
        if (tvOsd != null) {
            tvOsd.setText(text);
            osdContainer.setVisibility(View.VISIBLE);
            hideHandler.removeCallbacks(hideControlsRunnable);
            hideHandler.postDelayed(() -> osdContainer.setVisibility(View.GONE), 2000);
        }
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

    private void playPreviousEpisode() {
        if (currentEpisodeIndex > 0) {
            playEpisode(currentEpisodeIndex - 1);
        } else {
            showOsd("已经是第一集");
        }
    }

    private void playNextEpisode() {
        // TODO: 需要传入剧集总数来判断是否是最后一集
        playEpisode(currentEpisodeIndex + 1);
    }

    private void playEpisode(int index) {
        showOsd("跳转到第" + (index + 1) + "集");
        // TODO: 实际跳转逻辑需要从 Intent 获取剧集列表
    }

    private void toggleFullscreen() {
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private void cycleSpeed() {
        speedIndex = (speedIndex + 1) % SPEEDS.length;
        showOsd("倍速 " + SPEEDS[speedIndex]);
        if (player != null) player.setSpeed(Float.parseFloat(SPEEDS[speedIndex].replace("x", "")));
    }

    private void switchCore() {
        int core = PlayerFactory.nextCore();
        showOsd(PlayerFactory.coreName(core));
        recreatePlayer();
    }

    @Override public void onVideoSizeChanged(int width, int height) {
        adjustSurfaceRatio(width, height);
        adjustOrientation(width, height);
    }

    private void adjustSurfaceRatio(int videoWidth, int videoHeight) {
        if (videoWidth <= 0 || videoHeight <= 0) return;

        ViewGroup.LayoutParams lp = videoContainer.getLayoutParams();
        if (lp == null) return;

        int parentWidth = ((ViewGroup) videoContainer.getParent()).getWidth();
        int parentHeight = ((ViewGroup) videoContainer.getParent()).getHeight();

        float videoRatio = (float) videoWidth / videoHeight;
        float screenRatio = (float) parentWidth / parentHeight;

        if (videoRatio > screenRatio) {
            lp.width = parentWidth;
            lp.height = (int) (parentWidth / videoRatio);
        } else {
            lp.height = parentHeight;
            lp.width = (int) (parentHeight * videoRatio);
        }

        videoContainer.setLayoutParams(lp);
    }

    private void adjustOrientation(int width, int height) {
        if (width <= 0 || height <= 0) return;
        float aspectRatio = (float) width / height;
        boolean wasPortrait = isPortraitVideo;

        isPortraitVideo = aspectRatio < 0.8f;

        if (wasPortrait == isPortraitVideo) return;

        int newOrientation = isPortraitVideo
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;

        try {
            setRequestedOrientation(newOrientation);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override public void onPrepared() {
        runOnUiThread(() -> {
            if (loading != null) loading.setVisibility(View.GONE);
            if (btnPlayPause != null) btnPlayPause.setImageResource(R.drawable.ic_pause);
            prepared = true;
        });
    }

    @Override public void onCompletion() {
        runOnUiThread(() -> {
            if (btnPlayPause != null) btnPlayPause.setImageResource(R.drawable.ic_play);
        });
    }

    @Override public void onError(String msg) {
        runOnUiThread(() -> {
            if (loading != null) loading.setVisibility(View.GONE);
            Toast.makeText(this, "播放失败：" + msg, Toast.LENGTH_SHORT).show();
        });
    }

    @Override public void onLoading() {
        runOnUiThread(() -> {
            if (loading != null) loading.setVisibility(View.VISIBLE);
        });
    }

    @Override public void onBuffering(int progress) {
        runOnUiThread(() -> {
            if (loading != null) loading.setVisibility(View.VISIBLE);
        });
    }

    @Override public void onPosition(long pos) {
        if (dragging) return;
        runOnUiThread(() -> {
            if (seekBar != null) {
                seekBar.setProgress((int) pos);
            }
            if (tvPosition != null) {
                tvPosition.setText(formatTime(pos));
            }
            if (tvDuration != null && player != null) {
                tvDuration.setText(formatTime(player.getDuration()));
            }
        });
    }

    private String formatTime(long ms) {
        if (ms <= 0) return "00:00";
        long sec = ms / 1000;
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return h > 0
                ? String.format("%d:%02d:%02d", h, m, s)
                : String.format("%02d:%02d", m, s);
    }

    private void buildHistory() {
        if (currentSourceKey.isEmpty() || currentVodId.isEmpty()) return;
        history = HistoryHelper.get(currentSourceKey + "@@" + currentVodId);
        if (history != null && resumePosition == 0) {
            resumePosition = history.position;
        }
    }

    private void saveProgress() {
        if (history == null || player == null) return;
        history.position = player.getCurrentPosition();
        history.duration = player.getDuration();
        history.updateTime = System.currentTimeMillis();
        HistoryHelper.save(history);
    }

    private void createPlayer() {
        player = PlayerFactory.create(PlayerFactory.currentCore(), this);
        player.setListener(this);
        player.setSurface(videoContainer);
    }

    private void recreatePlayer() {
        if (player != null) {
            player.release();
        }
        createPlayer();
        String url = getIntent().getStringExtra(KEY_URL);
        resolveAndPlay(url);
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void resolveAndPlay(String url) {
        if (url == null || url.isEmpty()) return;

        SourceBean sb = ApiConfig.get().getSource(currentSourceKey);
        if (sb == null) {
            Toast.makeText(this, "源不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        ParseBean pb = ApiConfig.get().getDefaultParse();
        if (pb != null && pb.enable && Sniffer.needParse(url)) {
            parseJob = new ParseJob(pb.url, url, sb.ext, (pUrl, headers) -> {
                runOnUiThread(() -> {
                    player.start(pUrl, headers);
                });
            }, (err) -> {
                runOnUiThread(() -> {
                    player.start(url, null);
                });
            });
            OkGo.io.execute(parseJob);
        } else {
            player.start(url, null);
        }

        if (resumePosition > 0) {
            player.seekTo(resumePosition);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideHandler.removeCallbacks(hideControlsRunnable);
        if (player != null && player.isPlaying()) {
            player.pause();
            if (btnPlayPause != null) btnPlayPause.setImageResource(R.drawable.ic_play);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (controlsVisible) {
                hideControls();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
