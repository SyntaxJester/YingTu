package com.yingtu.plus.ui.activity;

import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.yingtu.plus.R;
import com.yingtu.plus.base.BaseActivity;

/**
 * 直播播放页。与点播不同，直播地址一般是 m3u8 直链，不需要二次解析，
 * 直接用 ExoPlayer 播放；支持频道自定义 UA。
 */
@OptIn(markerClass = UnstableApi.class)
public class LivePlayActivity extends BaseActivity {

    public static final String KEY_NAME = "name";
    public static final String KEY_URL = "url";
    public static final String KEY_UA = "ua";

    private PlayerView playerView;
    private ExoPlayer player;
    private TextView tvTitle;
    private View loading;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_play;
    }

    @Override
    protected void init() {
        playerView = find(R.id.playerView);
        tvTitle = find(R.id.tvTitle);
        loading = find(R.id.loading);

        String name = getIntent().getStringExtra(KEY_NAME);
        String url = getIntent().getStringExtra(KEY_URL);
        String ua = getIntent().getStringExtra(KEY_UA);
        tvTitle.setText(name == null ? "" : name);

        find(R.id.ivBack).setOnClickListener(v -> finish());

        play(url, ua);
    }

    private void play(String url, String ua) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "频道地址为空", Toast.LENGTH_SHORT).show();
            return;
        }
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                loading.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                loading.setVisibility(View.GONE);
                Toast.makeText(LivePlayActivity.this, "播放失败：" + error.getErrorCodeName(),
                        Toast.LENGTH_LONG).show();
            }
        });

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory();
        if (ua != null && !ua.isEmpty()) httpFactory.setUserAgent(ua);

        MediaItem item = MediaItem.fromUri(Uri.parse(url));
        player.setMediaSource(new DefaultMediaSourceFactory(httpFactory).createMediaSource(item));
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
