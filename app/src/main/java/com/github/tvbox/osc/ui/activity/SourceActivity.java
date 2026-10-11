package com.github.tvbox.osc.ui.activity;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.ui.adapter.SourceListAdapter;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.OkGo;
import com.orhanobut.hawk.Hawk;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 视频源管理页：
 *  - 顶部输入 TVBox 订阅地址 → 拉取解析 → 保存
 *  - 下方列出所有已解析源，点击切换当前源
 */
public class SourceActivity extends BaseActivity {

    private EditText etApi;
    private SourceListAdapter adapter;
    private View progress;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_source;
    }

    @Override
    protected void init() {
        etApi = find(R.id.etApi);
        progress = find(R.id.progress);
        etApi.setText(Hawk.get(HawkConfig.API_URL, ""));

        find(R.id.ivBack).setOnClickListener(v -> finish());
        find(R.id.btnLoad).setOnClickListener(v -> loadConfig(etApi.getText().toString()));

        RecyclerView rv = find(R.id.rvSource);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SourceListAdapter();
        adapter.setOnSourceClick(source -> {
            ApiConfig.get().setSourceBean(source);
            adapter.setCurrentKey(source.getKey());
            Toast.makeText(this, "已切换：" + source.getName(), Toast.LENGTH_SHORT).show();
        });
        rv.setAdapter(adapter);

        refreshList();
    }

    private void refreshList() {
        adapter.submit(ApiConfig.get().getSourceBeanList());
        SourceBean home = ApiConfig.get().getHomeSource();
        if (home != null) adapter.setCurrentKey(home.getKey());
    }

    private void loadConfig(String url) {
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "请输入订阅地址", Toast.LENGTH_SHORT).show();
            return;
        }
        progress.setVisibility(View.VISIBLE);
        Request request = new Request.Builder().url(url.trim()).build();
        OkGo.client().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                OkGo.main(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(SourceActivity.this, "拉取失败：" + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                OkGo.main(() -> {
                    progress.setVisibility(View.GONE);
                    try {
                        ApiConfig.get().parseConfig(body);
                        Hawk.put(HawkConfig.API_URL, url.trim());
                        Hawk.put(HawkConfig.API_CONFIG_CACHE, body);
                        refreshList();
                        Toast.makeText(SourceActivity.this,
                                "解析成功，共 " + ApiConfig.get().getSourceBeanList().size() + " 个源",
                                Toast.LENGTH_SHORT).show();
                    } catch (Throwable t) {
                        Toast.makeText(SourceActivity.this,
                                "解析失败，请检查地址是否为有效 TVBox 配置",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
