package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.view.View;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.ui.activity.HistoryActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.ui.activity.SourceActivity;

/**
 * 「我的」页：九宫格入口（视频源、观看历史、设置、关于等）。
 */
public class UserFragment extends BaseLazyFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_user;
    }

    @Override
    protected void init(View root) {
        root.findViewById(R.id.cardSource).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SourceActivity.class)));
        root.findViewById(R.id.cardHistory).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), HistoryActivity.class)));
        root.findViewById(R.id.cardSetting).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SettingActivity.class)));
        root.findViewById(R.id.cardAbout).setOnClickListener(v ->
                showAbout());
    }

    private void showAbout() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("关于 影图+")
                .setMessage("影图+ 是一个开源 TVBox 风格的影视聚合播放器。\n\n" +
                        "版本：1.0.0\n" +
                        "本项目仅供学习与交流，所有视频内容来自用户自行配置的第三方源，" +
                        "开发者不提供、不存储任何影视资源。")
                .setPositiveButton("知道了", null)
                .show();
    }
}
