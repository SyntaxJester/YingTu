package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.gridlayout.widget.GridLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.ui.adapter.GridMovieAdapter;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 首页（单屏结构，参考用户提供的 UI）：
 *   顶部标题 + 日期
 *   分类标签 pills（主页 + 各分类，青色药丸选中）
 *   快捷入口 2x4 宫格（历史/直播/文件/站源/搜索/推送/放映/设置）
 *   海报墙（3 列，带豆瓣角标/评分/备注，上拉加载更多）
 */
public class HomeActivity extends BaseActivity {

    private TextView tvDate;
    private TextView tvSource;
    private TabLayout tabLayout;
    private GridLayout quickGrid;
    private RecyclerView recyclerView;
    private View progress;
    private TextView tvEmpty;

    private SourceViewModel viewModel;
    private GridMovieAdapter adapter;
    private GridLayoutManager layoutManager;

    private final Handler clock = new Handler(Looper.getMainLooper());
    private final List<MovieSort> sorts = new ArrayList<>();

    private String currentTypeId = null; // null = 主页/推荐
    private int currentPage = 1;
    private boolean loading = false;

    // 快捷入口定义：标签 + 图标
    private final int[] quickIcons = {
            R.drawable.ic_history, R.drawable.ic_live, R.drawable.ic_file, R.drawable.ic_source,
            R.drawable.ic_search, R.drawable.ic_push, R.drawable.ic_cast, R.drawable.ic_setting
    };
    private final int[] quickLabels = {
            R.string.quick_history, R.string.quick_live, R.string.quick_file, R.string.quick_source,
            R.string.quick_search, R.string.quick_push, R.string.quick_cast, R.string.quick_setting
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_home;
    }

    @Override
    protected void init() {
        tvDate = find(R.id.tvDate);
        tvSource = find(R.id.tvSource);
        tabLayout = find(R.id.tabLayout);
        quickGrid = find(R.id.quickGrid);
        recyclerView = find(R.id.recyclerView);
        progress = find(R.id.progress);
        tvEmpty = find(R.id.tvEmpty);

        tvSource.setOnClickListener(v ->
                startActivity(new Intent(this, SourceActivity.class)));

        setupGrid();
        setupQuickEntries();
        setupTabs();

        viewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        viewModel.sortResult.observe(this, this::onSort);
        viewModel.listResult.observe(this, this::onList);

        startClock();
        loadSort();
    }

    private void setupGrid() {
        int cols = com.orhanobut.hawk.Hawk.get(com.github.tvbox.osc.util.HawkConfig.GRID_COLUMNS, 3);
        layoutManager = new GridLayoutManager(this, cols);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new GridMovieAdapter();
        adapter.setOnItemClick(movie -> {
            Intent i = new Intent(this, DetailActivity.class);
            i.putExtra(DetailActivity.KEY_SOURCE, movie.sourceKey);
            i.putExtra(DetailActivity.KEY_VOD_ID, movie.vodId);
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || loading || currentTypeId == null) return;
                if (layoutManager.findLastVisibleItemPosition() >= adapter.getDataCount() - 3) {
                    loading = true;
                    currentPage++;
                    viewModel.getList(currentTypeId, currentPage);
                }
            }
        });
    }

    private void setupQuickEntries() {
        LayoutInflater inflater = LayoutInflater.from(this);
        int columns = 4;
        for (int i = 0; i < quickIcons.length; i++) {
            View item = inflater.inflate(R.layout.item_quick_entry, quickGrid, false);
            ((ImageView) item.findViewById(R.id.ivIcon)).setImageResource(quickIcons[i]);
            ((TextView) item.findViewById(R.id.tvLabel)).setText(quickLabels[i]);

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

    private void onQuickClick(int index) {
        switch (index) {
            case 0: startActivity(new Intent(this, HistoryActivity.class)); break;
            case 1: startActivity(new Intent(this, LiveActivity.class)); break;
            case 2: toast("文件管理功能开发中"); break;
            case 3: startActivity(new Intent(this, SourceActivity.class)); break;
            case 4: startActivity(new Intent(this, SearchActivity.class)); break;
            case 5: toast("推送功能开发中"); break;
            case 6: toast("投屏放映功能开发中"); break;
            case 7: startActivity(new Intent(this, SettingActivity.class)); break;
        }
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) {
                    loadCategory(null); // 主页/推荐
                } else {
                    loadCategory(sorts.get(pos - 1).typeId);
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadSort() {
        showProgress();
        viewModel.getSort();
    }

    private void onSort(List<MovieSort> result) {
        sorts.clear();
        if (result != null) sorts.addAll(result);

        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("主页"));
        for (MovieSort s : sorts) {
            tabLayout.addTab(tabLayout.newTab().setText(s.typeName));
        }
        // 默认加载主页（首个分类内容做推荐）
        loadCategory(null);
    }

    private void loadCategory(String typeId) {
        currentTypeId = typeId;
        currentPage = 1;
        showProgress();
        if (typeId == null) {
            // 主页：取第一个分类作为推荐内容
            if (!sorts.isEmpty()) {
                currentTypeId = sorts.get(0).typeId;
                viewModel.getList(currentTypeId, 1);
            } else {
                onList(null);
            }
        } else {
            viewModel.getList(typeId, 1);
        }
    }

    private void onList(List<com.github.tvbox.osc.bean.Movie> movies) {
        progress.setVisibility(View.GONE);
        loading = false;
        boolean firstPage = currentPage == 1;
        if (firstPage) {
            boolean empty = movies == null || movies.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            adapter.submit(movies);
            recyclerView.scrollToPosition(0);
        } else {
            adapter.append(movies);
        }
    }

    private void showProgress() {
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSourceName();
    }

    private void refreshSourceName() {
        SourceBean source = ApiConfig.get().getHomeSource();
        String name = source != null ? source.getName() : "点击设置源";
        tvSource.setText(getString(R.string.app_name) + " · " + name);
    }

    private void startClock() {
        clock.post(new Runnable() {
            final SimpleDateFormat fmt = new SimpleDateFormat("MM-dd EEE", Locale.CHINA);
            @Override
            public void run() {
                tvDate.setText(fmt.format(new Date()));
                clock.postDelayed(this, 1000 * 30);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clock.removeCallbacksAndMessages(null);
    }
}
