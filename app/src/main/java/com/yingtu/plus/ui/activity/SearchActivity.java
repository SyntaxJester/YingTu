package com.yingtu.plus.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.ui.adapter.GridMovieAdapter;
import com.yingtu.plus.util.HawkConfig;
import com.yingtu.plus.viewmodel.SourceViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 搜索页：输入关键词 → 当前源搜索 → 网格展示结果；保留本地搜索历史。
 */
public class SearchActivity extends BaseActivity {

    private EditText etKeyword;
    private ChipGroup chipHistory;
    private View emptyView;
    private GridMovieAdapter adapter;
    private SourceViewModel viewModel;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_search;
    }

    @Override
    protected void init() {
        etKeyword = find(R.id.etKeyword);
        chipHistory = find(R.id.chipHistory);
        emptyView = find(R.id.tvEmpty);

        find(R.id.ivBack).setOnClickListener(v -> finish());
        find(R.id.btnSearch).setOnClickListener(v -> doSearch(etKeyword.getText().toString()));

        etKeyword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch(etKeyword.getText().toString());
                return true;
            }
            return false;
        });

        RecyclerView rv = find(R.id.recyclerView);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new GridMovieAdapter();
        adapter.setOnItemClick(movie -> {
            Intent i = new Intent(this, DetailActivity.class);
            i.putExtra(DetailActivity.KEY_SOURCE, movie.sourceKey);
            i.putExtra(DetailActivity.KEY_VOD_ID, movie.vodId);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        viewModel.searchResult.observe(this, movies -> {
            boolean empty = movies == null || movies.isEmpty();
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            adapter.submit(movies);
        });

        renderHistory();

        // 支持从 WebHome / 快捷入口带入关键词直接搜索
        String preset = getIntent().getStringExtra("keyword");
        if (preset != null && !preset.trim().isEmpty()) {
            etKeyword.setText(preset);
            etKeyword.setSelection(preset.length());
            doSearch(preset);
        }
    }

    private void doSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;
        keyword = keyword.trim();
        saveHistory(keyword);
        renderHistory();
        emptyView.setVisibility(View.GONE);
        viewModel.search(keyword);
    }

    private void renderHistory() {
        chipHistory.removeAllViews();
        for (String h : getHistory()) {
            Chip chip = new Chip(this);
            chip.setText(h);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> {
                etKeyword.setText(h);
                etKeyword.setSelection(h.length());
                doSearch(h);
            });
            chipHistory.addView(chip);
        }
    }

    private List<String> getHistory() {
        return Hawk.get(HawkConfig.SEARCH_HISTORY, new ArrayList<>());
    }

    private void saveHistory(String keyword) {
        Set<String> set = new LinkedHashSet<>();
        set.add(keyword);
        set.addAll(getHistory());
        List<String> list = new ArrayList<>(set);
        if (list.size() > 12) list = list.subList(0, 12);
        Hawk.put(HawkConfig.SEARCH_HISTORY, list);
    }
}
