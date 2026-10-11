package com.github.tvbox.osc.ui.fragment;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.adapter.GridMovieAdapter;
import com.github.tvbox.osc.viewmodel.SourceViewModel;

/**
 * 推荐页：加载当前源第一个分类的影片做首页海报墙。
 */
public class RecommendFragment extends BaseLazyFragment {

    private SourceViewModel viewModel;
    private GridMovieAdapter adapter;
    private ProgressBar progress;
    private TextView tvEmpty;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_grid;
    }

    @Override
    protected void init(View root) {
        progress = root.findViewById(R.id.progress);
        tvEmpty = root.findViewById(R.id.tvEmpty);
        RecyclerView rv = root.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new GridMovieAdapter();
        adapter.setOnItemClick(movie -> {
            Intent i = new Intent(getActivity(), DetailActivity.class);
            i.putExtra(DetailActivity.KEY_SOURCE, movie.sourceKey);
            i.putExtra(DetailActivity.KEY_VOD_ID, movie.vodId);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        viewModel.sortResult.observe(getViewLifecycleOwner(), sorts -> {
            if (sorts != null && !sorts.isEmpty()) {
                viewModel.getList(sorts.get(0).typeId, 1);
            } else {
                showEmpty();
            }
        });
        viewModel.listResult.observe(getViewLifecycleOwner(), movies -> {
            progress.setVisibility(View.GONE);
            if (movies == null || movies.isEmpty()) {
                showEmpty();
            } else {
                tvEmpty.setVisibility(View.GONE);
                adapter.submit(movies);
            }
        });
    }

    @Override
    protected void lazyLoad() {
        progress.setVisibility(View.VISIBLE);
        viewModel.getSort();
    }

    private void showEmpty() {
        progress.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("暂无内容，请先在「我的」里设置视频源");
    }
}
