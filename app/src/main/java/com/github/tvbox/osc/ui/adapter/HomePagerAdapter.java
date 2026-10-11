package com.github.tvbox.osc.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.github.tvbox.osc.ui.fragment.CategoryFragment;
import com.github.tvbox.osc.ui.fragment.LiveFragment;
import com.github.tvbox.osc.ui.fragment.RecommendFragment;
import com.github.tvbox.osc.ui.fragment.UserFragment;

/**
 * 首页四个 Tab 的 ViewPager2 适配器。
 */
public class HomePagerAdapter extends FragmentStateAdapter {

    public HomePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:  return new CategoryFragment();
            case 2:  return new LiveFragment();
            case 3:  return new UserFragment();
            default: return new RecommendFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
