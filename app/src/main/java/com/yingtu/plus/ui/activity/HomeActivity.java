package com.yingtu.plus.ui.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.yingtu.plus.R;
import com.yingtu.plus.api.ApiConfig;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.bean.SourceBean;
import com.yingtu.plus.ui.adapter.HomePagerAdapter;
import com.yingtu.plus.ui.adapter.SourceGridAdapter;
import com.yingtu.plus.util.OkGo;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 首页外壳。顶部 Logo + 标题（当前源名）+ 日期，下方 5 个 Tab：
 *   主页 / 网页首页 / 本地文件 / 直链收藏 / 分享收藏（ViewPager2 承载）。
 *
 * 右侧「站源选择」抽屉：参考原版，点击标题/站源入口从右侧滑出 2 列网格，
 * 点选即切源并刷新首页（无需跳转独立页面，和视频一的交互一致）。
 */
public class HomeActivity extends BaseActivity {

    public static final String KEY_TAB = "tab";

    private TextView tvDate, tvTitle, tvSourceCount;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private DrawerLayout drawerLayout;
    private SourceGridAdapter sourceAdapter;

    private final Handler clock = new Handler(Looper.getMainLooper());
    private final String[] tabTitles = {"主页", "网页首页", "本地文件", "直链收藏", "分享收藏"};

    @Override
    protected int getLayoutId() {
        return R.layout.activity_home;
    }

    @Override
    protected void init() {
        tvDate = find(R.id.tvDate);
        tvTitle = find(R.id.tvTitle);
        tvSourceCount = find(R.id.tvSourceCount);
        viewPager = find(R.id.viewPager);
        tabLayout = find(R.id.tabLayout);
        drawerLayout = find(R.id.drawerLayout);

        viewPager.setAdapter(new HomePagerAdapter(this));
        viewPager.setOffscreenPageLimit(tabTitles.length);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])).attach();

        int tab = getIntent().getIntExtra(KEY_TAB, 0);
        if (tab > 0 && tab < tabTitles.length) viewPager.setCurrentItem(tab, false);

        // 点击标题/Logo 打开站源抽屉
        tvTitle.setOnClickListener(v -> openSourceDrawer());
        find(R.id.ivLogo).setOnClickListener(v -> openSourceDrawer());

        setupDrawer();
        refreshTitle();
        startClock();
    }

    private void setupDrawer() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.END);
        find(R.id.btnManageSource).setOnClickListener(v ->
                startActivity(new Intent(this, SourceActivity.class)));

        androidx.recyclerview.widget.RecyclerView rv = find(R.id.rvSourceDrawer);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        sourceAdapter = new SourceGridAdapter();
        sourceAdapter.setOnSourceClick(this::onSourcePicked);
        rv.setAdapter(sourceAdapter);
    }

    private void openSourceDrawer() {
        List<SourceBean> sources = ApiConfig.get().getSourceBeanList();
        if (sources.isEmpty()) {
            // 没有源 → 直接去源管理页加载订阅
            startActivity(new Intent(this, SourceActivity.class));
            return;
        }
        sourceAdapter.submit(sources);
        SourceBean home = ApiConfig.get().getHomeSource();
        if (home != null) sourceAdapter.setCurrentKey(home.getKey());
        tvSourceCount.setText(sources.size() + " 个源");
        drawerLayout.openDrawer(Gravity.END);
    }

    private void onSourcePicked(SourceBean source) {
        ApiConfig.get().setSourceBean(source);
        sourceAdapter.setCurrentKey(source.getKey());
        // 预加载 spider jar（csp 站点首次用到）
        final String jar = ApiConfig.get().getSpider();
        if (jar != null && !jar.isEmpty()) {
            OkGo.io(() -> com.yingtu.plus.api.SpiderManager.get().parseJar(jar));
        }
        refreshTitle();
        drawerLayout.closeDrawer(Gravity.END);
        // 切到主页 Tab 并强制刷新首页内容
        viewPager.setCurrentItem(0, false);
        // 通知首页 Fragment 重新加载（它在 onVisible 比对 sourceKey，这里切 tab 会触发）
        notifyHomeReload();
    }

    private void notifyHomeReload() {
        viewPager.post(() -> {
            androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentByTag("f0");
            if (f instanceof com.yingtu.plus.ui.fragment.HomeMainFragment) {
                ((com.yingtu.plus.ui.fragment.HomeMainFragment) f).reloadForSourceChange();
            }
        });
    }

    private void refreshTitle() {
        SourceBean home = ApiConfig.get().getHomeSource();
        tvTitle.setText(home != null ? "影图 - " + home.getName() : getString(R.string.home_title));
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        if (intent == null || viewPager == null) return;
        int tab = intent.getIntExtra(KEY_TAB, -1);
        if (tab >= 0 && tab < tabTitles.length) viewPager.setCurrentItem(tab, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTitle();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.END)) {
            drawerLayout.closeDrawer(Gravity.END);
        } else {
            super.onBackPressed();
        }
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
