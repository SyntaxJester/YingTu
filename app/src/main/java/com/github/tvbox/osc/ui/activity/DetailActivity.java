package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.FavVod;
import com.github.tvbox.osc.bean.History;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.ui.adapter.SeriesAdapter;
import com.github.tvbox.osc.ui.adapter.SeriesFlagAdapter;
import com.github.tvbox.osc.util.FavHelper;
import com.github.tvbox.osc.util.HistoryHelper;
import com.github.tvbox.osc.viewmodel.SourceViewModel;

import android.widget.ImageView;
import android.widget.ProgressBar;

/**
 * 详情页：顶部封面 + 影片信息，中部线路切换，底部选集网格。
 * 点击某一集 → 跳转 PlayActivity 播放。
 */
public class DetailActivity extends BaseActivity {

    public static final String KEY_SOURCE = "sourceKey";
    public static final String KEY_VOD_ID = "vodId";

    private SourceViewModel viewModel;
    private SeriesFlagAdapter flagAdapter;
    private SeriesAdapter seriesAdapter;
    private ProgressBar progress;

    private VodInfo vodInfo;
    private String sourceKey;
    private String vodId;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_detail;
    }

    @Override
    protected void init() {
        sourceKey = getIntent().getStringExtra(KEY_SOURCE);
        vodId = getIntent().getStringExtra(KEY_VOD_ID);

        progress = find(R.id.progress);
        find(R.id.ivBack).setOnClickListener(v -> finish());
        find(R.id.ivFav).setOnClickListener(v -> toggleFav());
        find(R.id.ivShare).setOnClickListener(v -> shareVod());
        refreshFavIcon();

        RecyclerView rvFlag = find(R.id.rvFlag);
        rvFlag.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
                this, RecyclerView.HORIZONTAL, false));
        flagAdapter = new SeriesFlagAdapter();
        flagAdapter.setOnFlagSelected(this::onFlagSelected);
        rvFlag.setAdapter(flagAdapter);

        RecyclerView rvSeries = find(R.id.rvSeries);
        rvSeries.setLayoutManager(new GridLayoutManager(this, 5));
        seriesAdapter = new SeriesAdapter();
        seriesAdapter.setOnSeriesClick(this::playSeries);
        rvSeries.setAdapter(seriesAdapter);

        viewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        viewModel.detailResult.observe(this, this::bind);

        progress.setVisibility(View.VISIBLE);
        viewModel.getDetail(sourceKey, vodId);
    }

    private void bind(VodInfo info) {
        progress.setVisibility(View.GONE);
        if (info == null) {
            ((TextView) find(R.id.tvName)).setText("加载失败");
            return;
        }
        this.vodInfo = info;

        ((TextView) find(R.id.tvName)).setText(info.name);
        setText(R.id.tvSub, join(" · ", info.year, info.area, info.type));
        setText(R.id.tvActor, TextUtils.isEmpty(info.actor) ? "" : "主演：" + info.actor);
        setText(R.id.tvDirector, TextUtils.isEmpty(info.director) ? "" : "导演：" + info.director);
        setText(R.id.tvDesc, TextUtils.isEmpty(info.des) ? "暂无简介" : info.des);

        ImageView iv = find(R.id.ivPoster);
        Glide.with(this).load(info.pic)
                .placeholder(R.drawable.bg_poster_placeholder)
                .centerCrop().into(iv);

        flagAdapter.submit(info.seriesFlags);
        if (!info.seriesFlags.isEmpty()) {
            onFlagSelected(0);
        }
    }

    private void onFlagSelected(int index) {
        vodInfo.playFlag = index;
        flagAdapter.setSelected(index);
        seriesAdapter.submit(vodInfo.seriesFlags.get(index).list);
    }

    private void playSeries(int seriesIndex) {
        VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(vodInfo.playFlag);
        VodInfo.VodSeries series = flag.list.get(seriesIndex);

        // 若与历史记录是同一集，带上续播进度
        long resume = 0;
        History h = HistoryHelper.find(History.buildKey(sourceKey, vodId));
        if (h != null && h.playFlag == vodInfo.playFlag && h.playIndex == seriesIndex) {
            resume = h.position;
        }

        Intent i = new Intent(this, PlayActivity.class);
        i.putExtra(PlayActivity.KEY_TITLE, vodInfo.name + " " + series.name);
        i.putExtra(PlayActivity.KEY_URL, series.url);
        i.putExtra(PlayActivity.KEY_SOURCE, sourceKey);
        i.putExtra(PlayActivity.KEY_VOD_ID, vodId);
        i.putExtra(PlayActivity.KEY_VOD_NAME, vodInfo.name);
        i.putExtra(PlayActivity.KEY_VOD_PIC, vodInfo.pic);
        i.putExtra(PlayActivity.KEY_FLAG, vodInfo.playFlag);
        i.putExtra(PlayActivity.KEY_INDEX, seriesIndex);
        i.putExtra(PlayActivity.KEY_EPISODE, series.name);
        i.putExtra(PlayActivity.KEY_POSITION, resume);
        startActivity(i);
    }

    private void setText(int id, String text) {
        ((TextView) find(id)).setText(text == null ? "" : text);
    }

    private void refreshFavIcon() {
        boolean fav = FavHelper.isFav(sourceKey, vodId);
        ((ImageView) find(R.id.ivFav)).setImageResource(
                fav ? R.drawable.ic_fav_on : R.drawable.ic_fav_off);
    }

    private void toggleFav() {
        String name = vodInfo != null ? vodInfo.name : "";
        String pic = vodInfo != null ? vodInfo.pic : "";
        FavHelper.toggleVod(new FavVod(sourceKey, vodId, name, pic));
        refreshFavIcon();
        boolean fav = FavHelper.isFav(sourceKey, vodId);
        android.widget.Toast.makeText(this, fav ? "已收藏" : "已取消收藏",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void shareVod() {
        String name = vodInfo != null ? vodInfo.name : "影片";
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, name);
        send.putExtra(Intent.EXTRA_TEXT, "【影图+ 推荐】" + name
                + "\n来源：" + sourceKey + "\n影片ID：" + vodId);
        startActivity(Intent.createChooser(send, "分享影片"));
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (TextUtils.isEmpty(p)) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }
}
