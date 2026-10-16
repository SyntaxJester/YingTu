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
import androidx.recyclerview.widget.LinearLayoutManager;
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
import com.yingtu.plus.ui.adapter.CategoryTabAdapter;
import com.yingtu.plus.ui.adapter.FilterChipAdapter;
import com.yingtu.plus.ui.adapter.GridMovieAdapter;
import com.yingtu.plus.viewmodel.SourceViewModel;

import java.util.HashMap;
import java.util.List;

/**
 * 「主页」Tab（参考原版 UI）：
 *
 *  - 顶部：当前源的分类标签行（主页 + 各 class；选中且该分类带 filters 时标题显示 ▽）。
 *  - 其下：选中分类的筛选 chip 行（全部/都市/逆袭/古装...），点击作为 extend 传给
 *    categoryContent 实现分类筛选（修复视频二里短剧分类下无筛选的问题）。
 *  - 快捷入口宫格 + 海报墙（推荐/分类列表，上滑到底自动翻页）。
 *  - 未选源：仅宫格 + 引导。
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
    private RecyclerView rvCategory, rvFilter, rvContent;
    private CategoryTabAdapter categoryAdapter;
    private FilterChipAdapter filterAdapter;
    private GridMovieAdapter adapter;
    private ProgressBar progress;
    private TextView tvHint;
    private SourceViewModel viewModel;

    private String currentSourceKey = "";
    private boolean observersBound = false;

    // 分页/筛选状态
    private String curTypeId = null;          // null = 首页推荐
    private int curPage = 1;
    private boolean loading = false;
    private boolean noMore = false;
    private final HashMap<String, String> curExtend = new HashMap<>();

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home_main;
    }

    @Override
    protected void init(View root) {
        quickGrid = root.findViewById(R.id.quickGrid);
        rvCategory = root.findViewById(R.id.rvCategory);
        rvFilter = root.findViewById(R.id.rvFilter);
        rvContent = root.findViewById(R.id.rvContent);
        progress = root.findViewById(R.id.progress);
        tvHint = root.findViewById(R.id.tvHint);

        buildQuickGrid();

        rvCategory.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        categoryAdapter = new CategoryTabAdapter();
        categoryAdapter.setOnTabClick(this::onCategorySelected);
        rvCategory.setAdapter(categoryAdapter);

        rvFilter.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        filterAdapter = new FilterChipAdapter();
        filterAdapter.setOnValueClick(this::onFilterSelected);
        rvFilter.setAdapter(filterAdapter);

        GridLayoutManager glm = new GridLayoutManager(getContext(), 3);
        rvContent.setLayoutManager(glm);
        adapter = new GridMovieAdapter();
        adapter.setOnItemClick(movie -> {
            Intent i = new Intent(getActivity(), DetailActivity.class);
            i.putExtra(DetailActivity.KEY_SOURCE, movie.sourceKey);
            i.putExtra(DetailActivity.KEY_VOD_ID, movie.vodId);
            startActivity(i);
        });
        rvContent.setAdapter(adapter);
        rvContent.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || loading || noMore || curTypeId == null) return;
                int last = glm.findLastVisibleItemPosition();
                if (last >= adapter.getDataCount() - 3) loadMore();
            }
        });

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
            renderCategories(sorts);
        });
        viewModel.listResult.observe(this, movies -> {
            loading = false;
            progress.setVisibility(View.GONE);
            if (curPage > 1) {
                // 翻页：追加
                if (movies == null || movies.isEmpty()) { noMore = true; return; }
                adapter.append(movies);
            } else {
                boolean empty = movies == null || movies.isEmpty();
                tvHint.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (empty) tvHint.setText(curTypeId == null ? "该源暂无推荐内容" : "该分类暂无内容");
                adapter.submit(movies);
                rvContent.scrollToPosition(0);
            }
        });
    }

    @Override
    protected void onVisible() {
        SourceBean home = ApiConfig.get().getHomeSource();
        String key = home == null ? "" : home.getKey();
        if (home == null) {
            rvCategory.setVisibility(View.GONE);
            rvFilter.setVisibility(View.GONE);
            tvHint.setVisibility(View.VISIBLE);
            tvHint.setText(R.string.home_no_source);
            adapter.submit(null);
            currentSourceKey = "";
            return;
        }
        if (key.equals(currentSourceKey)) return;
        currentSourceKey = key;
        bindObservers();
        loadHome();
    }

    /** 由 HomeActivity 在站源抽屉切源后调用，强制重载首页内容。 */
    public void reloadForSourceChange() {
        if (!isAdded()) return;
        SourceBean home = ApiConfig.get().getHomeSource();
        if (home == null) return;
        currentSourceKey = home.getKey();
        bindObservers();
        loadHome();
    }

    private void loadHome() {
        tvHint.setVisibility(View.GONE);
        adapter.submit(null);
        rvCategory.setVisibility(View.GONE);
        rvFilter.setVisibility(View.GONE);
        quickGrid.setVisibility(View.VISIBLE);
        curTypeId = null;
        curPage = 1;
        noMore = false;
        curExtend.clear();
        progress.setVisibility(View.VISIBLE);
        viewModel.getSort();
    }

    private void renderCategories(List<MovieSort> sorts) {
        java.util.List<MovieSort> list = new java.util.ArrayList<>();
        MovieSort homeTab = new MovieSort(null, "主页"); // typeId=null → 推荐
        list.add(homeTab);
        if (sorts != null) list.addAll(sorts);

        categoryAdapter.submit(list);
        rvCategory.setVisibility(View.VISIBLE);
        // 默认选中「主页」：显示快捷入口宫格 + 推荐海报墙
        rvFilter.setVisibility(View.GONE);
        quickGrid.setVisibility(View.VISIBLE);
    }

    /** 点击顶部分类标签。 */
    private void onCategorySelected(int position, MovieSort sort) {
        categoryAdapter.setSelected(position);
        curExtend.clear();
        noMore = false;
        curPage = 1;

        if (sort.typeId == null) {
            // 回到首页推荐：显示宫格、隐藏筛选
            curTypeId = null;
            rvFilter.setVisibility(View.GONE);
            quickGrid.setVisibility(View.VISIBLE);
            progress.setVisibility(View.VISIBLE);
            adapter.submit(null);
            viewModel.getSort();
            return;
        }

        curTypeId = sort.typeId;
        // 分类页：隐藏宫格，腾出空间给内容（和原版一致）
        quickGrid.setVisibility(View.GONE);
        // 展开/隐藏筛选 chip 行：取第一组 filter 的 values
        if (sort.hasFilter()) {
            filterAdapter.submit(sort.filters.get(0).values);
            rvFilter.setVisibility(View.VISIBLE);
        } else {
            rvFilter.setVisibility(View.GONE);
        }
        progress.setVisibility(View.VISIBLE);
        adapter.submit(null);
        viewModel.getList(curTypeId, 1, new HashMap<>(curExtend));
    }

    /** 点击筛选 chip（如 都市/逆袭/古装）。 */
    private void onFilterSelected(MovieSort.Value value) {
        MovieSort sort = categoryAdapter.getItem(selectedCategory());
        if (sort == null || !sort.hasFilter()) return;
        String filterKey = sort.filters.get(0).key;
        curExtend.clear();
        if (value.v != null && !value.v.isEmpty()) curExtend.put(filterKey, value.v);
        curPage = 1;
        noMore = false;
        progress.setVisibility(View.VISIBLE);
        adapter.submit(null);
        viewModel.getList(curTypeId, 1, new HashMap<>(curExtend));
    }

    private int selectedCategory() {
        // CategoryTabAdapter 内部维护 selected，这里用 typeId 反查
        for (int i = 0; i < categoryAdapter.getItemCount(); i++) {
            MovieSort s = categoryAdapter.getItem(i);
            if (s != null && ((s.typeId == null && curTypeId == null)
                    || (s.typeId != null && s.typeId.equals(curTypeId)))) return i;
        }
        return 0;
    }

    private void loadMore() {
        if (curTypeId == null) return;
        loading = true;
        curPage++;
        viewModel.getList(curTypeId, curPage, new HashMap<>(curExtend));
    }

    private void onQuickClick(int index) {
        switch (index) {
            case 0: start(HistoryActivity.class); break;
            case 1: start(LiveActivity.class); break;
            case 2: switchTab(1); break;
            case 3: switchTab(2); break;
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
