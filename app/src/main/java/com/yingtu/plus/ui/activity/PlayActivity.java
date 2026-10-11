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
import com.yingtu.plus.api.VodSpider;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.bean.History;
import com.yingtu.plus.bean.ParseBean;
import com.yingtu.plus.bean.SourceBean;
import com.yingtu.plus.player.ParseJob;
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
import java.util.Map;

/**
 * 播放页。使用 Media3 ExoPlayer 播放。
 *
 * 播放地址解析链路（关键修复）：
 *  1. CSP 站点选集点进来时，{@code url} 往往不是直链，而是 Spider 的 episode id/标识。
 *     需要先在后台线程调用 {@code spider.playerContent(flag, id, …)}，返回 JSON 里取
 *     真实播放 url + header + 是否需二次解析（parse 字段）。
 *  2. 若 playerContent 标记 parse==1 或返回的仍是网页地址，再走 {@link ParseJob}
 *     （JSON 接口 / WebView 嗅探）解析成直链。
 *  3. 普通直链或非 CSP 源保持原有逻辑。
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayActivity extends BaseActivity {

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

    /** 决定直接播放 / 走 CSP Spider 解析 / 走通用二次解析。 */
    private void resolveAndPlay(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "播放地址为空", Toast.LENGTH_SHORT).show();
            return;
        }
        loading.setVisibility(View.VISIBLE);

        String sourceKey = getIntent().getStringExtra(KEY_SOURCE);
        SourceBean source = sourceKey == null ? null : ApiConfig.get().getSource(sourceKey);

        if (source != null && VodSpider.isCsp(source)) {
            resolveCsp(source, url);
        } else {
            // 非 CSP：直接判断是否要通用二次解析
            afterUrlResolved(url, new HashMap<>());
        }
    }

    /** CSP 站点：spider.playerContent 拿真实直链。 */
    private void resolveCsp(SourceBean source, String episodeUrl) {
        String flagName = getIntent().getStringExtra(KEY_FLAG_NAME);
        final String flag = flagName == null ? "" : flagName;
        VodSpider.executor().execute(() -> {
            String playUrl = episodeUrl;
            Map<String, String> headers = new HashMap<>();
            boolean needParse = false;
            try {
                Spider spider = VodSpider.get(source);
                List<String> vipFlags = new ArrayList<>();
                String json = spider.playerContent(flag, episodeUrl, vipFlags);
                if (json != null && json.trim().startsWith("{")) {
                    JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                    String u = optString(o, "url");
                    if (!u.isEmpty()) playUrl = u;
                    // parse==1 表示返回的是网页，需要再嗅探
                    if (o.has("parse") && !o.get("parse").isJsonNull()) {
                        try { needParse = o.get("parse").getAsInt() == 1; } catch (Throwable ignored) {}
                    }
                    // header 可能是对象或字符串
                    if (o.has("header")) headers.putAll(parseHeader(o.get("header")));
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            final String finalUrl = playUrl;
            final Map<String, String> finalHeaders = headers;
            final boolean finalNeedParse = needParse;
            OkGo.main(() -> {
                if (finalNeedParse || Sniffer.needParse(finalUrl)) {
                    runParse(finalUrl, finalHeaders);
                } else {
                    play(finalUrl, finalHeaders);
                }
            });
        });
    }

    /** 非 CSP / CSP 返回仍是网页时的通用二次解析。 */
    private void afterUrlResolved(String url, Map<String, String> headers) {
        if (Sniffer.needParse(url)) {
            runParse(url, headers);
        } else {
            play(url, headers);
        }
    }

    private void runParse(String url, Map<String, String> baseHeaders) {
        loading.setVisibility(View.VISIBLE);
        ParseBean parse = ApiConfig.get().getDefaultParse();
        parseJob = ParseJob.create(new ParseJob.Callback() {
            @Override
            public void onParseSuccess(Map<String, String> headers, String directUrl) {
                Map<String, String> merged = new HashMap<>(baseHeaders);
                if (headers != null) merged.putAll(headers);
                play(directUrl, merged);
            }

            @Override
            public void onParseError() {
                loading.setVisibility(View.GONE);
                Toast.makeText(PlayActivity.this,
                        "解析失败，该资源可能需要专用解析接口", Toast.LENGTH_LONG).show();
            }
        }).start(parse, url);
    }

    private void play(String url, Map<String, String> headers) {
        if (isFinishing() || player == null) return;
        if (url == null || url.isEmpty()) {
            loading.setVisibility(View.GONE);
            Toast.makeText(this, "未取得可播放地址", Toast.LENGTH_SHORT).show();
            return;
        }
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

    // ===== helpers =====

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
