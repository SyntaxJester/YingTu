package com.yingtu.plus.player.core;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * ExoPlayer（Media3）内核实现。软硬解自适应、HLS 支持好，作为默认内核。
 */
@OptIn(markerClass = UnstableApi.class)
public class ExoPlayerCore implements AbstractPlayer {

    private final Context context;
    private ExoPlayer player;
    private Listener listener;
    private Surface surface;

    public ExoPlayerCore(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void setListener(Listener l) {
        this.listener = l;
    }

    @Override
    public void setSurface(Surface surface) {
        this.surface = surface;
        if (player != null) player.setVideoSurface(surface);
    }

    private void ensure() {
        if (player != null) return;
        player = new ExoPlayer.Builder(context).build();
        if (surface != null) player.setVideoSurface(surface);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (listener == null) return;
                if (state == Player.STATE_BUFFERING) listener.onBuffering(true);
                else listener.onBuffering(false);
                if (state == Player.STATE_READY) listener.onPrepared();
                if (state == Player.STATE_ENDED) listener.onCompletion();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                if (listener != null) listener.onError(error.getErrorCodeName());
            }

            @Override
            public void onVideoSizeChanged(VideoSize vs) {
                if (listener != null) listener.onVideoSizeChanged(vs.width, vs.height);
            }
        });
    }

    @Override
    public void setDataSource(String url, Map<String, String> headers) {
        ensure();
        MediaItem item = new MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMediaMetadata(new MediaMetadata.Builder().build())
                .build();
        DefaultHttpDataSource.Factory f = new DefaultHttpDataSource.Factory();
        if (headers != null && !headers.isEmpty()) {
            String ua = null;
            Map<String, String> extra = new HashMap<>();
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if ("User-Agent".equalsIgnoreCase(e.getKey())) ua = e.getValue();
                else extra.put(e.getKey(), e.getValue());
            }
            if (ua != null) f.setUserAgent(ua);
            if (!extra.isEmpty()) f.setDefaultRequestProperties(extra);
        }
        player.setMediaSource(new DefaultMediaSourceFactory(f).createMediaSource(item));
    }

    @Override
    public void prepare() {
        if (player != null) player.prepare();
    }

    @Override
    public void start() {
        if (player != null) player.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        if (player != null) player.pause();
    }

    @Override
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    @Override
    public void seekTo(long positionMs) {
        if (player != null) player.seekTo(positionMs);
    }

    @Override
    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    @Override
    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    @Override
    public void setSpeed(float speed) {
        if (player != null) player.setPlaybackParameters(new PlaybackParameters(speed));
    }

    @Override
    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public int core() {
        return CORE_EXO;
    }
}
