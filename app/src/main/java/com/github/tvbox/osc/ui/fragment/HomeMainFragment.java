package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.gridlayout.widget.GridLayout;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.ui.activity.HistoryActivity;
import com.github.tvbox.osc.ui.activity.LiveActivity;
import com.github.tvbox.osc.ui.activity.SearchActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.ui.activity.SourceActivity;

/**
 * 「主页」Tab：无源加载前的落地页。
 *
 * 2x4 磨砂快捷入口宫格：历史 / 直播 / 文件 / 站源 / 搜索 / 推送 / 放映 / 设置。
 * 下方留白（海洋壁纸），有源后可在此接入海报墙（后续）。
 */
public class HomeMainFragment extends BaseLazyFragment {

    private final int[] icons = {
            R.drawable.ic_history, R.drawable.ic_live, R.drawable.ic_file, R.drawable.ic_source,
            R.drawable.ic_search, R.drawable.ic_push, R.drawable.ic_cast, R.drawable.ic_setting
    };
    private final int[] labels = {
            R.string.quick_history, R.string.quick_live, R.string.quick_file, R.string.quick_source,
            R.string.quick_search, R.string.quick_push, R.string.quick_cast, R.string.quick_setting
    };

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home_main;
    }

    @Override
    protected void init(View root) {
        GridLayout grid = root.findViewById(R.id.quickGrid);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int columns = 4;
        for (int i = 0; i < icons.length; i++) {
            View item = inflater.inflate(R.layout.item_quick_entry, grid, false);
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
            grid.addView(item);
        }
    }

    private void onQuickClick(int index) {
        switch (index) {
            case 0: start(HistoryActivity.class); break;
            case 1: start(LiveActivity.class); break;
            case 2: switchTab(1); break;              // 文件 → 本地文件 Tab
            case 3: start(SourceActivity.class); break;
            case 4: start(SearchActivity.class); break;
            case 5: toast("推送功能开发中"); break;
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
