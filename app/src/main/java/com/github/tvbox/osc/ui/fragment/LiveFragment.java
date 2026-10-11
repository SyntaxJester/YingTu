package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.api.LiveParser;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.LiveChannel;
import com.github.tvbox.osc.bean.LiveGroup;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;
import com.github.tvbox.osc.ui.adapter.LiveChannelAdapter;
import com.github.tvbox.osc.ui.adapter.LiveGroupAdapter;
import com.github.tvbox.osc.util.OkGo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 直播页：左侧分组 + 右侧频道列表。
 *
 * 从配置里的 lives 地址拉取 m3u / txt 频道列表（LiveParser 解析），
 * 点击频道跳转 LivePlayActivity 播放。
 */
public class LiveFragment extends BaseLazyFragment {

    private ProgressBar progress;
    private TextView tvEmpty;
    private LiveGroupAdapter groupAdapter;
    private LiveChannelAdapter channelAdapter;

    private final List<LiveGroup> groups = new ArrayList<>();

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_live;
    }

    @Override
    protected void init(View root) {
        progress = root.findViewById(R.id.progress);
        tvEmpty = root.findViewById(R.id.tvEmpty);

        RecyclerView rvGroup = root.findViewById(R.id.rvGroup);
        rvGroup.setLayoutManager(new LinearLayoutManager(getContext()));
        groupAdapter = new LiveGroupAdapter();
        groupAdapter.setOnGroupSelected(this::showGroup);
        rvGroup.setAdapter(groupAdapter);

        RecyclerView rvChannel = root.findViewById(R.id.rvChannel);
        rvChannel.setLayoutManager(new LinearLayoutManager(getContext()));
        channelAdapter = new LiveChannelAdapter();
        channelAdapter.setOnChannelClick(this::playChannel);
        rvChannel.setAdapter(channelAdapter);
    }

    @Override
    protected void lazyLoad() {
        loadLives();
    }

    private void loadLives() {
        List<String> urls = ApiConfig.get().getLiveUrls();
        if (urls.isEmpty()) {
            showEmpty("当前源未配置直播地址\n可在带 lives 字段的 TVBox 订阅中获取");
            return;
        }
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        // 取第一个直播地址
        String url = urls.get(0);
        Request request = new Request.Builder().url(url).build();
        OkGo.client().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                OkGo.main(() -> showEmpty("直播列表加载失败：" + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                List<LiveGroup> parsed = LiveParser.parse(body);
                OkGo.main(() -> bind(parsed));
            }
        });
    }

    private void bind(List<LiveGroup> parsed) {
        progress.setVisibility(View.GONE);
        groups.clear();
        groups.addAll(parsed);
        if (groups.isEmpty()) {
            showEmpty("直播列表为空或格式不支持");
            return;
        }
        tvEmpty.setVisibility(View.GONE);
        groupAdapter.submit(groups);
        showGroup(0);
    }

    private void showGroup(int index) {
        if (index < 0 || index >= groups.size()) return;
        groupAdapter.setSelected(index);
        channelAdapter.submit(groups.get(index).channels);
    }

    private void playChannel(LiveChannel channel) {
        if (channel.urls.isEmpty()) return;
        Intent i = new Intent(getActivity(), LivePlayActivity.class);
        i.putExtra(LivePlayActivity.KEY_NAME, channel.name);
        i.putExtra(LivePlayActivity.KEY_URL, channel.currentUrl());
        i.putExtra(LivePlayActivity.KEY_UA, channel.ua);
        startActivity(i);
    }

    private void showEmpty(String msg) {
        progress.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(msg);
    }
}
