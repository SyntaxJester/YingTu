package com.yingtu.plus.ui.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.yingtu.plus.R;
import com.yingtu.plus.api.ApiConfig;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.bean.SourceBean;
import com.yingtu.plus.ui.adapter.HomePagerAdapter;
import com.yingtu.plus.ui.adapter.SourceGridAdapter;
import com.yingtu.plus.util.OkGo;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 首页外壳。顶部 Logo + 标题（当前源名）+ 日期，下方 Tab。
 * 点击标题/Logo 弹出站源选择小窗（带搜索 + 分类筛选）。
 */
public class HomeActivity extends BaseActivity {

    public static final String KEY_TAB = "tab";

    private TextView tvDate, tvTitle, tvSourceCount;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private SourceGridAdapter sourceAdapter;
    private PopupWindow sourcePopup;
    private EditText etSearch;
    private LinearLayout chipContainer;

    private final Handler clock = new Handler(Looper.getMainLooper());
    private final String[] tabTitles = {"主页", "网页首页", "本地文件", "直链收藏", "分享收藏"};

    @Override
    protected int getLayoutId() {
        return R.layout.activity_home;
    }

    @Override
    protected void init() {
        // 应用自定义壁纸
        applyCustomWallpaper();
        tvDate = find(R.id.tvDate);
        tvTitle = find(R.id.tvTitle);
        viewPager = find(R.id.viewPager);
        tabLayout = find(R.id.tabLayout);

        viewPager.setAdapter(new HomePagerAdapter(this));
        viewPager.setOffscreenPageLimit(tabTitles.length);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])).attach();

        int tab = getIntent().getIntExtra(KEY_TAB, 0);
        if (tab > 0 && tab < tabTitles.length) viewPager.setCurrentItem(tab, false);

        // 点击标题/Logo 打开站源弹窗
        tvTitle.setOnClickListener(v -> openSourcePopup());
        find(R.id.ivLogo).setOnClickListener(v -> openSourcePopup());

        setupSourcePopup();
        refreshTitle();
        startClock();
    }

    private void setupSourcePopup() {
        View popupView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_source_picker, null);

        sourcePopup = new PopupWindow(popupView,
                (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        sourcePopup.setOutsideTouchable(true);
        sourcePopup.setBackgroundDrawable(null);
        sourcePopup.setAnimationStyle(android.R.style.Animation_Dialog);

        etSearch = popupView.findViewById(R.id.etSearch);
        chipContainer = popupView.findViewById(R.id.chipContainer);
        tvSourceCount = popupView.findViewById(R.id.tvSourceCount);

        androidx.recyclerview.widget.RecyclerView rv = popupView.findViewById(R.id.rvSource);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        sourceAdapter = new SourceGridAdapter();
        sourceAdapter.setOnSourceClick(this::onSourcePicked);
        rv.setAdapter(sourceAdapter);

        // 构建 chip
        buildChips();

        // 搜索
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                sourceAdapter.setSearch(s.toString());
            }
        });

        // 管理源按钮
        popupView.findViewById(R.id.btnManageSource).setOnClickListener(v -> {
            if (sourcePopup.isShowing()) sourcePopup.dismiss();
            startActivity(new Intent(this, SourceActivity.class));
        });
    }

    private void buildChips() {
        chipContainer.removeAllViews();
        final List<String> chips = sourceAdapter.getChipList();
        for (int i = 0; i < chips.size(); i++) {
            final String chipText = chips.get(i);
            final boolean isDefault = (i == 0);
            Chip c = new Chip(this);
            c.setText(chipText);
            c.setCheckable(true);
            c.setChecked(isDefault);
            c.setOnClickListener(v -> {
                sourceAdapter.setFilterChip(chipText);
            });
            chipContainer.addView(c);
        }
    }

    private void openSourcePopup() {
        List<SourceBean> sources = ApiConfig.get().getSourceBeanList();
        if (sources.isEmpty()) {
            startActivity(new Intent(this, SourceActivity.class));
            return;
        }
        sourceAdapter.submit(sources);
        SourceBean home = ApiConfig.get().getHomeSource();
        if (home != null) sourceAdapter.setCurrentKey(home.getKey());
        tvSourceCount.setText(sources.size() + " 个源");
        etSearch.setText("");
        sourceAdapter.setSearch("");
        sourceAdapter.setFilterChip("全部");
        buildChips();

        if (sourcePopup.isShowing()) {
            sourcePopup.dismiss();
        } else {
            sourcePopup.showAtLocation(find(android.R.id.content),
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    0, 100);
        }
    }

    private void onSourcePicked(SourceBean source) {
        ApiConfig.get().setSourceBean(source);
        sourceAdapter.setCurrentKey(source.getKey());
        final String jar = ApiConfig.get().getSpider();
        if (jar != null && !jar.isEmpty()) {
            OkGo.io(() -> com.yingtu.plus.api.SpiderManager.get().parseJar(jar));
        }
        refreshTitle();
        if (sourcePopup.isShowing()) sourcePopup.dismiss();
        viewPager.setCurrentItem(0, false);
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
    protected void onNewIntent(Intent intent) {
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
        if (sourcePopup != null && sourcePopup.isShowing()) {
            sourcePopup.dismiss();
        } else {
            super.onBackPressed();
        }
    }

    private void applyCustomWallpaper() {
        android.net.Uri uri = com.yingtu.plus.util.WallpaperHelper.getWallpaperUri(this);
        if (uri != null) {
            try {
                android.graphics.Bitmap bmp =
                        android.graphics.BitmapFactory.decodeStream(
                                getContentResolver().openInputStream(uri));
                if (bmp != null) {
                    getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.BitmapDrawable(bmp));
                }
            } catch (Exception ignored) {}
        }
    }

    private void startClock() {
        Runnable tick = new Runnable() {
            final SimpleDateFormat fmt = new SimpleDateFormat("MM-dd EEE", Locale.CHINA);
            @Override
            public void run() {
                tvDate.setText(fmt.format(new Date()));
                clock.postDelayed(this, 1000 * 30);
            }
        };
        clock.post(tick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clock.removeCallbacksAndMessages(null);
        if (sourcePopup != null && sourcePopup.isShowing()) sourcePopup.dismiss();
    }
}
