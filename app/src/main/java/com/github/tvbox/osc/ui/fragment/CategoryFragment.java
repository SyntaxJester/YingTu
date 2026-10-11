package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.adapter.GridMovieAdapter;
import com.github.tvbox.osc.ui.adapter.SortTabAdapter;
import com.github.tvbox.osc.viewmodel.SourceViewModel;

import java.util.List;

/**
 * 分类页：左侧（顶部）分类标签，右侧对应分类的影片网格，支持上拉加载更多。
 */
public class CategoryFragment extends BaseLazyFragment {

    private SourceViewModel viewModel;
    private GridMovieAdapter gridAdapter;
    private SortTabAdapter tabAdapter;
    private ProgressBar progress;
    private TextView tvEmpty;

    private String currentTypeId = null;
    private int currentPage = 1;
    private boolean loading = false;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_category;
    }

    @Override
    protected void init(View root) {
        progress = root.findViewById(R.id.progress);
        tvEmpty = root.findViewById(R.id.tvEmpty);

        RecyclerView rvTab = root.findViewById(R.id.rvTab);
        rvTab.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        tabAdapter = new SortTabAdapter();
        tabAdapter.setOnTabSelected(sort -> loadCategory(sort.typeId));
        rvTab.setAdapter(tabAdapter);

        RecyclerView rvGrid = root.findViewById(R.id.rvGrid);
        GridLayoutManager glm = new GridLayoutManager(getContext(), 3);
        rvGrid.setLayoutManager(glm);
        gridAdapter = new GridMovieAdapter();
        gridAdapter.setOnItemClick(movie -> {
            Intent i = new Intent(getActivity(), DetailActivity.class);
            i.putExtra(DetailActivity.KEY_SOURCE, movie.sourceKey);
            i.putExtra(DetailActivity.KEY_VOD_ID, movie.vodId);
            startActivity(i);
        });
        rvGrid.setAdapter(gridAdapter);
        rvGrid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || loading) return;
                int last = glm.findLastVisibleItemPosition();
                if (last >= gridAdapter.getDataCount() - 3) {
                    loading = true;
                    currentPage++;
                    viewModel.getList(currentTypeId, currentPage);
                }
            }
        });

        viewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        viewModel.sortResult.observe(getViewLifecycleOwner(), this::onSort);
        viewModel.listResult.observe(getViewLifecycleOwner(), movies -> {
            progress.setVisibility(View.GONE);
            loading = false;
            if (currentPage == 1) {
                boolean empty = movies == null || movies.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                gridAdapter.submit(movies);
            } else {
                gridAdapter.append(movies);
            }
        });
    }

    @Override
    protected void lazyLoad() {
        progress.setVisibility(View.VISIBLE);
        viewModel.getSort();
    }

    private void onSort(List<MovieSort> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            progress.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tabAdapter.submit(sorts);
        loadCategory(sorts.get(0).typeId);
    }

    private void loadCategory(String typeId) {
        currentTypeId = typeId;
        currentPage = 1;
        progress.setVisibility(View.VISIBLE);
        viewModel.getList(typeId, 1);
    }
}
