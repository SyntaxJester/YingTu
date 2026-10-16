package com.yingtu.plus.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 所有 Fragment 的基类（懒加载：首次可见时触发 lazyLoad）。
 */
public abstract class BaseLazyFragment extends Fragment {

    private boolean loaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!loaded) {
            loaded = true;
            lazyLoad();
        }
        onVisible();
    }

    @LayoutRes
    protected abstract int getLayoutId();

    protected abstract void init(View root);

    /** 首次可见触发一次 */
    protected void lazyLoad() {
    }

    /** 每次回到前台触发 */
    protected void onVisible() {
    }
}
