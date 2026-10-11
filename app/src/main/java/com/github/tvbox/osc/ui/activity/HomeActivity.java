package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.ui.adapter.HomePagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 首页。顶部标题栏 + 时间 + 当前源名称，中部 ViewPager2 承载
 * 「推荐 / 分类 / 直播 / 我的」四个 Fragment，底部无额外栏。
 */
public class HomeActivity extends BaseActivity {

    private TextView tvDate;
    private TextView tvSource;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    private final Handler clock = new Handler(Looper.getMainLooper());
    private final String[] tabTitles = {"推荐", "分类", "直播", "我的"};

    @Override
    protected int getLayoutId() {
        return R.layout.activity_home;
    }

    @Override
    protected void init() {
        tvDate = find(R.id.tvDate);
        tvSource = find(R.id.tvSource);
        viewPager = find(R.id.viewPager);
        tabLayout = find(R.id.tabLayout);

        find(R.id.ivSearch).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        find(R.id.ivSetting).setOnClickListener(v ->
                startActivity(new Intent(this, SettingActivity.class)));
        tvSource.setOnClickListener(v ->
                startActivity(new Intent(this, SourceActivity.class)));

        viewPager.setAdapter(new HomePagerAdapter(this));
        viewPager.setOffscreenPageLimit(tabTitles.length);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])).attach();

        startClock();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSourceName();
    }

    private void refreshSourceName() {
        SourceBean source = ApiConfig.get().getHomeSource();
        tvSource.setText(source != null ? source.getName() : "点击设置源");
    }

    private void startClock() {
        clock.post(new Runnable() {
            final SimpleDateFormat fmt = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
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
