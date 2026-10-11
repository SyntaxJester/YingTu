package com.github.tvbox.osc.ui.activity;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.ui.adapter.HomePagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 首页外壳。顶部 Logo + 标题 + 日期，下方 4 个 Tab：
 *   主页 / 本地文件 / 直链收藏 / 分享收藏（ViewPager2 承载）。
 *
 * 「主页」Tab 本身包含快捷入口宫格与（有源时的）海报墙。
 */
public class HomeActivity extends BaseActivity {

    private TextView tvDate;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    private final Handler clock = new Handler(Looper.getMainLooper());
    private final String[] tabTitles = {"主页", "本地文件", "直链收藏", "分享收藏"};

    @Override
    protected int getLayoutId() {
        return R.layout.activity_home;
    }

    @Override
    protected void init() {
        tvDate = find(R.id.tvDate);
        viewPager = find(R.id.viewPager);
        tabLayout = find(R.id.tabLayout);

        viewPager.setAdapter(new HomePagerAdapter(this));
        viewPager.setOffscreenPageLimit(tabTitles.length);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])).attach();

        startClock();
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
