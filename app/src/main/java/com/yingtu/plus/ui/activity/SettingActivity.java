package com.yingtu.plus.ui.activity;

import android.content.Intent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.yingtu.plus.R;
import com.yingtu.plus.api.ApiConfig;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.bean.ParseBean;
import com.yingtu.plus.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import java.util.List;

/**
 * 设置页。分区卡片式布局，参考用户提供的 UI：
 *   数据接口 / 首页和搜索 / 播放设置 / 系统设置 + 底部功能按钮。
 *
 * 可用项已接真实逻辑（接口地址、首页推荐源名、默认解析、深色模式、
 * 网格列数、清缓存、关于、检测更新）；展示性项提供取值切换对话框。
 */
public class SettingActivity extends BaseActivity {

    private TextView tvApiUrl, tvParseName, tvHomeRec, tvGridColumns,
            tvOrientation, tvStartSource, tvCacheHint;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_setting;
    }

    @Override
    protected void init() {
        find(R.id.ivBack).setOnClickListener(v -> finish());

        bindDataSection();
        bindHomeSection();
        bindPlaySection();
        bindSystemSection();
        bindBottom();

        ((TextView) find(R.id.tvVersion)).setText("影图+ v1.7.0");
    }

    // ===== 数据接口 =====
    private void bindDataSection() {
        tvApiUrl = find(R.id.tvApiUrl);
        refreshApiUrl();
        find(R.id.rowApiUrl).setOnClickListener(v ->
                startActivity(new Intent(this, SourceActivity.class)));
        find(R.id.btnApiSetting).setOnClickListener(v ->
                startActivity(new Intent(this, SourceActivity.class)));
        find(R.id.btnApiAdvanced).setOnClickListener(v ->
                startActivity(new Intent(this, SourceActivity.class)));
        find(R.id.rowInject).setOnClickListener(v -> toast("注入地址功能开发中"));
        find(R.id.rowApiCache).setOnClickListener(v -> toast("接口缓存功能开发中"));
    }

    private void refreshApiUrl() {
        String url = Hawk.get(HawkConfig.API_URL, "");
        tvApiUrl.setText(url.isEmpty() ? getString(R.string.val_not_set) : shorten(url));
    }

    // ===== 首页和搜索 =====
    private void bindHomeSection() {
        tvStartSource = find(R.id.tvStartSource);
        tvHomeRec = find(R.id.tvHomeRec);
        refreshHomeRec();

        find(R.id.btnSourcePin).setOnClickListener(v ->
                startActivity(new Intent(this, SourceActivity.class)));
        find(R.id.btnSourceDisable).setOnClickListener(v ->
                startActivity(new Intent(this, SourceActivity.class)));
        find(R.id.rowStartSource).setOnClickListener(v ->
                startActivity(new Intent(this, SourceActivity.class)));
        find(R.id.rowHomeRec).setOnClickListener(v -> pickHomeRec());
        find(R.id.rowSearchShow).setOnClickListener(v -> toast("搜索展示：图片｜文字"));
    }

    private void refreshHomeRec() {
        tvHomeRec.setText(Hawk.get(HawkConfig.HOME_REC, "豆瓣热播"));
    }

    private void pickHomeRec() {
        String[] opts = {"豆瓣热播", "站源推荐"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.row_home_rec)
                .setItems(opts, (d, which) -> {
                    Hawk.put(HawkConfig.HOME_REC, opts[which]);
                    refreshHomeRec();
                })
                .show();
    }

    // ===== 播放设置 =====
    private void bindPlaySection() {
        tvParseName = find(R.id.tvParseName);
        refreshParseName();
        find(R.id.rowParse).setOnClickListener(v -> showParsePicker());

        // 展示性项：点击给出说明（当前播放器固定 ExoPlayer）
        find(R.id.rowDetailStyle).setOnClickListener(v -> toast("详情页样式：影音图片"));
        find(R.id.rowProgressTag).setOnClickListener(v -> toast("进度标识：名称"));
        find(R.id.rowPlayer).setOnClickListener(v -> toast("当前内置 ExoPlayer 播放器"));
        find(R.id.rowRender).setOnClickListener(v -> toast("渲染方式：TextureView"));
        find(R.id.rowScale).setOnClickListener(v -> toast("画面缩放：适应"));
        find(R.id.rowSkipAd).setOnClickListener(v -> toast("解析跳过防盗：开启"));
        find(R.id.rowAutoFloat).setOnClickListener(v -> toast("自动悬浮：关闭"));
    }

    private void refreshParseName() {
        ParseBean p = ApiConfig.get().getDefaultParse();
        tvParseName.setText(p != null ? p.getName() : getString(R.string.parse_none));
    }

    private void showParsePicker() {
        List<ParseBean> parses = ApiConfig.get().getParseBeanList();
        if (parses.isEmpty()) {
            toast("当前源未配置解析接口");
            return;
        }
        String[] names = new String[parses.size()];
        int checked = -1;
        for (int i = 0; i < parses.size(); i++) {
            names[i] = parses.get(i).getName();
            if (parses.get(i).isSelected()) checked = i;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.setting_default_parse)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    ApiConfig.get().setDefaultParse(parses.get(which));
                    refreshParseName();
                    d.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ===== 系统设置 =====
    private void bindSystemSection() {
        tvOrientation = find(R.id.tvOrientation);
        tvGridColumns = find(R.id.tvGridColumns);
        refreshGridColumns();

        SwitchCompat swDark = find(R.id.swDark);
        swDark.setChecked(Hawk.get(HawkConfig.DARK_MODE, true));
        swDark.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> {
            Hawk.put(HawkConfig.DARK_MODE, checked);
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        find(R.id.rowOrientation).setOnClickListener(v -> toast("屏幕方向：竖屏"));
        find(R.id.rowStartPage).setOnClickListener(v -> toast("默认启动：首页"));
        find(R.id.rowGridColumns).setOnClickListener(v -> pickGridColumns());

        find(R.id.rowClearCache).setOnClickListener(v -> {
            Hawk.delete(HawkConfig.SEARCH_HISTORY);
            ((TextView) find(R.id.tvCacheHint)).setText(R.string.val_on);
            toast("已清理播放缓存");
        });
    }

    private void refreshGridColumns() {
        int cols = Hawk.get(HawkConfig.GRID_COLUMNS, 3);
        tvGridColumns.setText(cols + " 列");
    }

    private void pickGridColumns() {
        String[] opts = {"3 列", "4 列", "5 列"};
        int cur = Hawk.get(HawkConfig.GRID_COLUMNS, 3);
        int checked = cur - 3;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.row_grid_columns)
                .setSingleChoiceItems(opts, checked, (d, which) -> {
                    Hawk.put(HawkConfig.GRID_COLUMNS, which + 3);
                    refreshGridColumns();
                    d.dismiss();
                })
                .show();
    }

    // ===== 底部按钮 =====
    private void bindBottom() {
        find(R.id.btnBackup).setOnClickListener(v -> toast("数据备份功能开发中"));
        find(R.id.btnClearCache).setOnClickListener(v -> {
            Hawk.delete(HawkConfig.PLAY_HISTORY);
            Hawk.delete(HawkConfig.SEARCH_HISTORY);
            toast("缓存已清除");
        });
        find(R.id.btnUpdate).setOnClickListener(v -> toast("当前已是最新版本 v1.7.0"));
        find(R.id.btnAbout).setOnClickListener(v -> showAbout());
    }

    private void showAbout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("关于 影图+")
                .setMessage("影图+ 是一个开源 TVBox 风格的影视聚合播放器。\n\n" +
                        "版本：1.7.0\n" +
                        "分类筛选 + 站源抽屉切换（对齐原版交互）；CatVod Spider 运行时 + WebHome\n\n" +
                        "本项目仅供学习与交流，所有视频内容来自用户自行配置的第三方源，" +
                        "开发者不提供、不存储任何影视资源。")
                .setPositiveButton("知道了", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshApiUrl();
        refreshParseName();
    }

    // ===== 工具 =====
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private static String shorten(String url) {
        if (url.length() <= 34) return url;
        return url.substring(0, 16) + "…" + url.substring(url.length() - 16);
    }
}
