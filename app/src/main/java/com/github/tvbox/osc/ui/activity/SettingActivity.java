package com.github.tvbox.osc.ui.activity;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

/**
 * 设置页：深色模式开关、清除缓存、版本信息。
 */
public class SettingActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_setting;
    }

    @Override
    protected void init() {
        find(R.id.ivBack).setOnClickListener(v -> finish());

        SwitchCompat swDark = find(R.id.swDark);
        swDark.setChecked(Hawk.get(HawkConfig.DARK_MODE, false));
        swDark.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> {
            Hawk.put(HawkConfig.DARK_MODE, checked);
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        find(R.id.rowClearCache).setOnClickListener(v -> {
            Hawk.delete(HawkConfig.PLAY_HISTORY);
            Hawk.delete(HawkConfig.SEARCH_HISTORY);
            ((TextView) find(R.id.tvCacheHint)).setText("已清除");
        });

        ((TextView) find(R.id.tvVersion)).setText("影图+ v1.0.0");
    }
}
