package com.yingtu.plus.ui.fragment;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.gridlayout.widget.GridLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.api.ApiConfig;
import com.yingtu.plus.base.BaseLazyFragment;
import com.yingtu.plus.bean.MovieSort;
import com.yingtu.plus.bean.SourceBean;
import com.yingtu.plus.ui.activity.DetailActivity;
import com.yingtu.plus.ui.activity.HistoryActivity;
import com.yingtu.plus.ui.activity.LiveActivity;
import com.yingtu.plus.ui.activity.SearchActivity;
import com.yingtu.plus.ui.activity.SettingActivity;
import com.yingtu.plus.ui.activity.SourceActivity;
import com.yingtu.plus.ui.adapter.GridMovieAdapter;
import com.yingtu.plus.viewmodel.SourceViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * 「主页」Tab。
 *
 * 分两种状态：
 *  - 未选源：显示 2x4 磨砂快捷入口（历史/直播/网页/文件/站源/搜索/放映/设置）+ 引导提示。
 *  - 已选源：顶部显示该源的分类标签（主页/分类...），下方海报墙加载首页推荐 / 分类列表。
 *
 * 这解决了「加载视频源后首页空白」的问题：之前主页只是静态快捷入口，从不请求内容；
 * 现在会在可见时读取当前源并通过 {@link SourceViewModel} 拉取 homeContent/categoryContent。
 */
public class HomeMainFragment extends BaseLazyFragment {

    private final int[] icons = {
            R.drawable.ic_history, R.drawable.ic_live, R.drawable.ic_web, R.drawable.ic_file,
            R.drawable.ic_source, R.drawable.ic_search, R.drawable.ic_cast, R.drawable.ic_setting
    };
    private final int[] labels = {
            R.string.quick_history, R.string.quick_live, R.string.quick_web, R.string.quick_file,
            R.string.quick_source, R.string.quick_search, R.string.quick_cast, R.string.quick_setting
    };

    private GridLayout quickGrid;
    private View sortScroll;
    private ChipGroup sortGroup;
    private RecyclerView rvContent;
    private ProgressBar progress;
    private TextView tvHint;
    private GridMovieAdapter adapter;
    private SourceViewModel viewModel;

    private String currentSourceKey = "";
    private boolean observersBound = false;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home_main;
    }

    @Override
    protected void init(View root) {
        quickGrid = root.findViewById(R.id.quickGrid);
        sortScroll = root.findViewById(R.id.sortScroll);
        sortGroup = root.findViewById(R.id.sortGroup);
        rvContent = root.findViewById(R.id.rvContent);
        progress = root.findViewById(R.id.progress);
        tvHint = root.findViewById(R.id.tvHint);

        buildQuickGrid();

        rvContent.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new GridMovieAdapter();
        adapter.setOnItemClick(movie -> {
            Intent i = new Intent(getActivity(), DetailActivity.class);
            i.putExtra(DetailActivity.KEY_SOURCE, movie.sourceKey);
            i.putExtra(DetailActivity.KEY_VOD_ID, movie.vodId);
            startActivity(i);
        });
        rvContent.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(SourceViewModel.class);
    }

    private void buildQuickGrid() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int columns = 4;
        for (int i = 0; i < icons.length; i++) {
            View item = inflater.inflate(R.layout.item_quick_entry, quickGrid, false);
            ((ImageView) item.findViewById(R.id.ivIcon)).setImageResource(icons[i]);
            ((TextView) item.findViewById(R.id.tvLabel)).setText(labels[i]);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(i % columns, 1f);
            lp.rowSpec = GridLayout.spec(i / columns);
            item.setLayoutParams(lp);
            final int index = i;
            item.setOnClickListener(v -> onQuickClick(index));
            quickGrid.addView(item);
        }
    }

    private void bindObservers() {
        if (observersBound) return;
        observersBound = true;
        viewModel.sortResult.observe(this, sorts -> {
            progress.setVisibility(View.GONE);
            renderSorts(sorts);
        });
        viewModel.listResult.observe(this, movies -> {
            progress.setVisibility(View.GONE);
            boolean empty = movies == null || movies.isEmpty();
            tvHint.setVisibility(empty && adapter.getDataCount() == 0 ? View.VISIBLE : View.GONE);
            if (empty) {
                if (adapter.getDataCount() == 0) tvHint.setText("该分类暂无内容");
                return;
            }
            tvHint.setVisibility(View.GONE);
            adapter.submit(movies);
            rvContent.scrollToPosition(0);
        });
    }

    @Override
    protected void onVisible() {
        SourceBean home = ApiConfig.get().getHomeSource();
        String key = home == null ? "" : home.getKey();
        if (home == null) {
            // 未选源：只显示快捷入口 + 引导
            sortScroll.setVisibility(View.GONE);
            tvHint.setVisibility(View.VISIBLE);
            tvHint.setText(R.string.home_no_source);
            adapter.submit(null);
            currentSourceKey = "";
            return;
        }
        // 已选源且与上次相同则不重复加载
        if (key.equals(currentSourceKey)) return;
        currentSourceKey = key;
        bindObservers();
        loadHome();
    }

    private void loadHome() {
        tvHint.setVisibility(View.GONE);
        adapter.submit(null);
        sortGroup.removeAllViews();
        sortScroll.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        viewModel.getSort();
    }

    private void renderSorts(List<MovieSort> sorts) {
        sortGroup.removeAllViews();
        if (sorts == null || sorts.isEmpty()) {
            sortScroll.setVisibility(View.GONE);
            return;
        }
        sortScroll.setVisibility(View.VISIBLE);

        // 「主页」项：展示首页推荐
        addChip("主页", null, true);
        for (MovieSort s : sorts) addChip(s.typeName, s.typeId, false);
    }

    private void addChip(String label, String typeId, boolean checked) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setClickable(true);
        chip.setOnClickListener(v -> {
            progress.setVisibility(View.VISIBLE);
            adapter.submit(null);
            if (typeId == null) {
                viewModel.getSort();      // 回到首页推荐
            } else {
                viewModel.getList(typeId, 1);
            }
        });
        sortGroup.addView(chip);
    }

    private void onQuickClick(int index) {
        switch (index) {
            case 0: start(HistoryActivity.class); break;
            case 1: start(LiveActivity.class); break;
            case 2: switchTab(1); break;              // 网页 → 网页首页 Tab
            case 3: switchTab(2); break;              // 文件 → 本地文件 Tab
            case 4: start(SourceActivity.class); break;
            case 5: start(SearchActivity.class); break;
            case 6: toast("投屏放映功能开发中"); break;
            case 7: start(SettingActivity.class); break;
        }
    }

    private void switchTab(int index) {
        if (getActivity() == null) return;
        androidx.viewpager2.widget.ViewPager2 vp = getActivity().findViewById(R.id.viewPager);
        if (vp != null) vp.setCurrentItem(index, true);
    }

    private void start(Class<?> cls) {
        startActivity(new Intent(getActivity(), cls));
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
