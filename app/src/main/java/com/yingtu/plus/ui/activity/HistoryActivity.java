package com.yingtu.plus.ui.activity;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.bean.History;
import com.yingtu.plus.ui.adapter.HistoryAdapter;
import com.yingtu.plus.util.HistoryHelper;

import java.util.List;

/**
 * 观看历史页：网格展示历史记录。
 * 点击 → 回到详情页（可继续播放），长按 → 删除该条，右上角清空全部。
 */
public class HistoryActivity extends BaseActivity {

    private HistoryAdapter adapter;
    private View emptyView;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_history;
    }

    @Override
    protected void init() {
        emptyView = find(R.id.tvEmpty);
        find(R.id.ivBack).setOnClickListener(v -> finish());
        find(R.id.ivClear).setOnClickListener(v -> confirmClear());

        RecyclerView rv = find(R.id.recyclerView);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new HistoryAdapter();
        adapter.setOnHistoryClick(this::openDetail);
        adapter.setOnHistoryLongClick(this::confirmDelete);
        rv.setAdapter(adapter);

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<History> list = HistoryHelper.getAll();
        emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submit(list);
    }

    private void openDetail(History h) {
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra(DetailActivity.KEY_SOURCE, h.sourceKey);
        i.putExtra(DetailActivity.KEY_VOD_ID, h.vodId);
        startActivity(i);
    }

    private void confirmDelete(History h) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage("删除「" + h.vodName + "」的观看记录？")
                .setPositiveButton("删除", (d, w) -> {
                    HistoryHelper.delete(h.key);
                    refresh();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmClear() {
        if (HistoryHelper.getAll().isEmpty()) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage("确定清空全部观看记录？")
                .setPositiveButton("清空", (d, w) -> {
                    HistoryHelper.clear();
                    refresh();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
