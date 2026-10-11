package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.ui.adapter.GridMovieAdapter;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.List;

/**
 * 观看历史页：从本地 Hawk 读取历史记录并以网格展示。
 */
public class HistoryActivity extends BaseActivity {

    private GridMovieAdapter adapter;
    private View emptyView;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_history;
    }

    @Override
    protected void init() {
        emptyView = find(R.id.tvEmpty);
        find(R.id.ivBack).setOnClickListener(v -> finish());

        RecyclerView rv = find(R.id.recyclerView);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new GridMovieAdapter();
        adapter.setOnItemClick(movie -> {
            Intent i = new Intent(this, DetailActivity.class);
            i.putExtra(DetailActivity.KEY_SOURCE, movie.sourceKey);
            i.putExtra(DetailActivity.KEY_VOD_ID, movie.vodId);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        List<Movie> history = Hawk.get(HawkConfig.PLAY_HISTORY, new ArrayList<>());
        emptyView.setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submit(history);
    }
}
