package com.yingtu.plus.ui.activity;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yingtu.plus.R;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.player.core.AbstractPlayer;
import com.yingtu.plus.player.core.PlayerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 直播 / 本地视频 / 直链播放页。直链一般无需二次解析，直接用所选内核播放。
 * 复用可切换内核（ExoPlayer / IJK / MPV）。
 */
public class LivePlayActivity extends BaseActivity implements AbstractPlayer.Listener {

    public static final String KEY_NAME = "name";
    public static final String KEY_URL = "url";
    public static final String KEY_UA = "ua";

    private SurfaceView surfaceView;
    private TextView tvTitle, btnCore;
    private ImageView btnPlayPause;
    private View loading;
    private SeekBar seekBar;
    private TextView tvPosition, tvDuration, btnSpeed;

    private AbstractPlayer player;
    private String url, ua;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_play;
    }

    @Override
    protected void init() {
        surfaceView = find(R.id.surfaceView);
        tvTitle = find(R.id.tvTitle);
        btnCore = find(R.id.btnCore);
        btnPlayPause = find(R.id.btnPlayPause);
        loading = find(R.id.loading);
        seekBar = find(R.id.seekBar);
        tvPosition = find(R.id.tvPosition);
        tvDuration = find(R.id.tvDuration);
        btnSpeed = find(R.id.btnSpeed);

        String name = getIntent().getStringExtra(KEY_NAME);
        url = getIntent().getStringExtra(KEY_URL);
        ua = getIntent().getStringExtra(KEY_UA);
        tvTitle.setText(name == null ? "" : name);
        btnCore.setText(PlayerFactory.coreName(PlayerFactory.currentCore()));

        // 直播通常是流，隐藏进度/倍速意义不大但保留播放暂停
        btnSpeed.setVisibility(View.GONE);

        find(R.id.ivBack).setOnClickListener(v -> finish());
        btnPlayPause.setOnClickListener(v -> {
            if (player == null) return;
            if (player.isPlaying()) { player.pause(); btnPlayPause.setImageResource(R.drawable.ic_play); }
            else { player.start(); btnPlayPause.setImageResource(R.drawable.ic_pause); }
        });
        btnCore.setOnClickListener(v -> switchCore());

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder h) { if (player != null) player.setSurface(h.getSurface()); }
            @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int ht) {}
            @Override public void surfaceDestroyed(SurfaceHolder h) { if (player != null) player.setSurface(null); }
        });

        play();
    }

    private void play() {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "地址为空", Toast.LENGTH_SHORT).show();
            return;
        }
        loading.setVisibility(View.VISIBLE);
        player = PlayerFactory.create(this);
        player.setListener(this);
        if (surfaceView.getHolder().getSurface() != null) player.setSurface(surfaceView.getHolder().getSurface());
        Map<String, String> headers = new HashMap<>();
        if (ua != null && !ua.isEmpty()) headers.put("User-Agent", ua);
        player.setDataSource(url, headers);
        player.prepare();
    }

    private void switchCore() {
        int next = (PlayerFactory.currentCore() + 1) % 3;
        PlayerFactory.setCore(next);
        btnCore.setText(PlayerFactory.coreName(next));
        Toast.makeText(this, "已切换内核：" + PlayerFactory.coreName(next), Toast.LENGTH_SHORT).show();
        if (player != null) { player.release(); player = null; }
        play();
    }

    @Override public void onPrepared() {
        loading.setVisibility(View.GONE);
        player.start();
        btnPlayPause.setImageResource(R.drawable.ic_pause);
    }
    @Override public void onCompletion() { btnPlayPause.setImageResource(R.drawable.ic_play); }
    @Override public void onBuffering(boolean b) { loading.setVisibility(b ? View.VISIBLE : View.GONE); }
    @Override public void onError(String msg) {
        loading.setVisibility(View.GONE);
        Toast.makeText(this, "播放失败：" + msg + "（可切换内核重试）", Toast.LENGTH_LONG).show();
    }
    @Override public void onVideoSizeChanged(int w, int h) {
        if (w <= 0 || h <= 0) return;
        View parent = (View) surfaceView.getParent();
        int pw = parent.getWidth(), ph = parent.getHeight();
        if (pw == 0 || ph == 0) return;
        float ratio = (float) w / h;
        int nw, nh;
        if (pw / (float) ph > ratio) { nh = ph; nw = (int) (ph * ratio); }
        else { nw = pw; nh = (int) (pw / ratio); }
        android.view.ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
        lp.width = nw; lp.height = nh;
        surfaceView.setLayoutParams(lp);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) { player.pause(); btnPlayPause.setImageResource(R.drawable.ic_play); }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) { player.release(); player = null; }
    }
}
