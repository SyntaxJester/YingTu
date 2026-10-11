package com.github.tvbox.osc.ui.activity;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import java.util.List;

/**
 * 设置页：默认解析器选择、深色模式开关、清除缓存、版本信息。
 */
public class SettingActivity extends BaseActivity {

    private TextView tvParseName;

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

        tvParseName = find(R.id.tvParseName);
        refreshParseName();
        find(R.id.rowParse).setOnClickListener(v -> showParsePicker());

        find(R.id.rowClearCache).setOnClickListener(v -> {
            Hawk.delete(HawkConfig.PLAY_HISTORY);
            Hawk.delete(HawkConfig.SEARCH_HISTORY);
            ((TextView) find(R.id.tvCacheHint)).setText("已清除");
        });

        ((TextView) find(R.id.tvVersion)).setText("影图+ v1.1.0");
    }

    private void refreshParseName() {
        ParseBean p = ApiConfig.get().getDefaultParse();
        tvParseName.setText(p != null ? p.getName() : getString(R.string.parse_none));
    }

    private void showParsePicker() {
        List<ParseBean> parses = ApiConfig.get().getParseBeanList();
        if (parses.isEmpty()) {
            Toast.makeText(this, "当前源未配置解析接口", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[parses.size()];
        int checked = -1;
        for (int i = 0; i < parses.size(); i++) {
            names[i] = parses.get(i).getName();
            if (parses.get(i).isSelected()) checked = i;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("选择默认解析")
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    ApiConfig.get().setDefaultParse(parses.get(which));
                    refreshParseName();
                    d.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
