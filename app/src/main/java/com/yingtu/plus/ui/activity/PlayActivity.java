package com.yingtu.plus.ui.activity;

import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.yingtu.plus.R;
import com.yingtu.plus.api.ApiConfig;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.bean.History;
import com.yingtu.plus.bean.ParseBean;
import com.yingtu.plus.player.ParseJob;
import com.yingtu.plus.util.HistoryHelper;
import com.yingtu.plus.util.Sniffer;

import java.util.HashMap;
import java.util.Map;

/**
 * 播放页。使用 Media3 ExoPlayer 播放。
 *
 * 相比初版新增：
 *  1. 二次解析：若传入 url 不是直链（Sniffer.needParse），先走 {@link ParseJob}
 *     （JSON 接口解析 + WebView 网页嗅探）拿到真实直链再播放；
 *  2. 自定义请求头：解析结果带回的 UA/Referer/Cookie 用于播放；
 *  3. 观看历史：进入即写入/更新一条历史，退出时保存播放进度用于续播。
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayActivity extends BaseActivity {

    public static final String KEY_TITLE = "title";
    public static final String KEY_URL = "url";
    public static final String KEY_SOURCE = "sourceKey";
    // 历史续播相关
    public static final String KEY_VOD_ID = "vodId";
    public static final String KEY_VOD_NAME = "vodName";
    public static final String KEY_VOD_PIC = "vodPic";
    public static final String KEY_FLAG = "playFlag";
    public static final String KEY_INDEX = "playIndex";
    public static final String KEY_EPISODE = "episodeName";
    public static final String KEY_POSITION = "position";

    private PlayerView playerView;
    private ExoPlayer player;
    private TextView tvTitle;
    private View loading;
    private ParseJob parseJob;

    private History history;
    private long resumePosition = 0;

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
        resumePosition = getIntent().getLongExtra(KEY_POSITION, 0);

        find(R.id.ivBack).setOnClickListener(v -> finish());

        buildHistory();
        initPlayer();
        resolveAndPlay(url);
    }

    /** 根据 Intent 构建/更新历史记录 */
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

    private void initPlayer() {
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
                Toast.makeText(PlayActivity.this, "播放失败：" + error.getErrorCodeName(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /** 判断是否需要二次解析，决定直接播放还是先解析 */
    private void resolveAndPlay(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "播放地址为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Sniffer.needParse(url)) {
            loading.setVisibility(View.VISIBLE);
            ParseBean parse = ApiConfig.get().getDefaultParse();
            parseJob = ParseJob.create(new ParseJob.Callback() {
                @Override
                public void onParseSuccess(Map<String, String> headers, String directUrl) {
                    play(directUrl, headers);
                }

                @Override
                public void onParseError() {
                    loading.setVisibility(View.GONE);
                    Toast.makeText(PlayActivity.this,
                            "解析失败，该资源可能需要专用解析接口", Toast.LENGTH_LONG).show();
                }
            }).start(parse, url);
        } else {
            play(url, new HashMap<>());
        }
    }

    private void play(String url, Map<String, String> headers) {
        if (isFinishing() || player == null) return;
        MediaItem item = new MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(tvTitle.getText().toString()).build())
                .build();

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory();
        if (headers != null && !headers.isEmpty()) {
            String ua = null;
            Map<String, String> extra = new HashMap<>();
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if ("User-Agent".equalsIgnoreCase(e.getKey())) ua = e.getValue();
                else extra.put(e.getKey(), e.getValue());
            }
            if (ua != null) httpFactory.setUserAgent(ua);
            if (!extra.isEmpty()) httpFactory.setDefaultRequestProperties(extra);
        }

        player.setMediaSource(new DefaultMediaSourceFactory(httpFactory).createMediaSource(item));
        player.prepare();
        if (resumePosition > 0) player.seekTo(resumePosition);
        player.setPlayWhenReady(true);
    }

    /** 保存播放进度到历史 */
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

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
        saveProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveProgress();
        if (parseJob != null) {
            parseJob.stop();
            parseJob = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
