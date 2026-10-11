package com.github.tvbox.osc.ui.fragment;

import android.view.View;
import android.widget.TextView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;

/**
 * 直播页占位。真实直播需要加载 m3u/txt 频道源并接入播放器，
 * 这里给出清晰的空态与后续接入说明。
 */
public class LiveFragment extends BaseLazyFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_placeholder;
    }

    @Override
    protected void init(View root) {
        TextView tv = root.findViewById(R.id.tvPlaceholder);
        tv.setText("直播\n\n在「我的 → 直播源」里配置 m3u / txt 频道列表后，即可在此播放电视直播。");
    }
}
