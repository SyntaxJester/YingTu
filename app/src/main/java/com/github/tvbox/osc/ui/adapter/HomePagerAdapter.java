package com.github.tvbox.osc.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.github.tvbox.osc.ui.fragment.DirectLinkFragment;
import com.github.tvbox.osc.ui.fragment.FavVodFragment;
import com.github.tvbox.osc.ui.fragment.HomeMainFragment;
import com.github.tvbox.osc.ui.fragment.LocalFileFragment;

/**
 * 首页 4 个顶部 Tab：主页 / 本地文件 / 直链收藏 / 分享收藏。
 */
public class HomePagerAdapter extends FragmentStateAdapter {

    public HomePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:  return new LocalFileFragment();
            case 2:  return new DirectLinkFragment();
            case 3:  return new FavVodFragment();
            default: return new HomeMainFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
