package com.github.tvbox.osc.ui.activity;

import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;

/**
 * 播放页。使用 Media3 ExoPlayer 直接播放。
 *
 * 注意：对于需要二次解析（嗅探/JS 解析）的源，这里拿到的是解析后直链；
 * 当前实现假设传入 url 为可直接播放的 http(s)/hls 直链。
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayActivity extends BaseActivity {

    public static final String KEY_TITLE = "title";
    public static final String KEY_URL = "url";
    public static final String KEY_SOURCE = "sourceKey";

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

        String title = getIntent().getStringExtra(KEY_TITLE);
        String url = getIntent().getStringExtra(KEY_URL);
        tvTitle.setText(title == null ? "" : title);

        find(R.id.ivBack).setOnClickListener(v -> finish());

        setupPlayer(url);
    }

    private void setupPlayer(String url) {
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
                tvTitle.append("  (播放失败)");
            }
        });

        if (url != null && !url.isEmpty()) {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
            player.prepare();
            player.setPlayWhenReady(true);
        }
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
