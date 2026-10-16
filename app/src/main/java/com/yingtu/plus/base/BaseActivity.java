package com.yingtu.plus.base;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 所有 Activity 的基类，统一沉浸式状态栏与布局注入方式。
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int layout = getLayoutId();
        if (layout != 0) {
            setContentView(layout);
        }
        init();
    }

    @LayoutRes
    protected abstract int getLayoutId();

    protected abstract void init();

    protected <T extends View> T find(int id) {
        return findViewById(id);
    }
}
