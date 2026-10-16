package com.yingtu.plus.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.yingtu.plus.ui.fragment.DirectLinkFragment;
import com.yingtu.plus.ui.fragment.FavVodFragment;
import com.yingtu.plus.ui.fragment.HomeMainFragment;
import com.yingtu.plus.ui.fragment.LocalFileFragment;
import com.yingtu.plus.ui.fragment.WebHomeFragment;

/**
 * 首页 5 个顶部 Tab：主页 / 网页首页 / 本地文件 / 直链收藏 / 分享收藏。
 */
public class HomePagerAdapter extends FragmentStateAdapter {

    public HomePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:  return new WebHomeFragment();
            case 2:  return new LocalFileFragment();
            case 3:  return new DirectLinkFragment();
            case 4:  return new FavVodFragment();
            default: return new HomeMainFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
