package com.yingtu.plus.ui.activity;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.yingtu.plus.R;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.ui.adapter.HomePagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 首页外壳。顶部 Logo + 标题 + 日期，下方 5 个 Tab：
 *   主页 / 网页首页 / 本地文件 / 直链收藏 / 分享收藏（ViewPager2 承载）。
 *
 * 「主页」Tab 本身包含快捷入口宫格与（有源时的）海报墙。
 * 「网页首页」Tab 即 webhtv 的 WebHome：用 HTML/CSS/JS 自定义首页，可调用 Native SDK。
 */
public class HomeActivity extends BaseActivity {

    /** Intent extra：打开后直接定位到指定 Tab 下标 */
    public static final String KEY_TAB = "tab";

    private TextView tvDate;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    private final Handler clock = new Handler(Looper.getMainLooper());
    private final String[] tabTitles = {"主页", "网页首页", "本地文件", "直链收藏", "分享收藏"};

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

        int tab = getIntent().getIntExtra(KEY_TAB, 0);
        if (tab > 0 && tab < tabTitles.length) viewPager.setCurrentItem(tab, false);

        startClock();
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        if (intent == null || viewPager == null) return;
        int tab = intent.getIntExtra(KEY_TAB, -1);
        if (tab >= 0 && tab < tabTitles.length) viewPager.setCurrentItem(tab, false);
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
