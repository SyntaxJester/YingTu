package com.yingtu.plus.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.bean.SourceBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 站源选择弹窗里的 2 列网格适配器。
 * 支持搜索过滤和分类 chip 筛选。
 */
public class SourceGridAdapter extends RecyclerView.Adapter<SourceGridAdapter.VH> {

    public interface OnSourceClick {
        void onClick(SourceBean source);
    }

    private final List<SourceBean> allData = new ArrayList<>();
    private final List<SourceBean> displayData = new ArrayList<>();
    private String currentKey = "";
    private String searchKeyword = "";
    private String filterChip = "全部";
    private final List<String> chipList = Arrays.asList("全部", "短剧", "综艺", "动漫", "影视", "其他");
    private OnSourceClick listener;

    public void setOnSourceClick(OnSourceClick l) {
        this.listener = l;
    }

    public List<String> getChipList() {
        return chipList;
    }

    public void submit(List<SourceBean> list) {
        allData.clear();
        if (list != null) allData.addAll(list);
        applyFilter();
        notifyDataSetChanged();
    }

    public void setSearch(String keyword) {
        this.searchKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        applyFilter();
        notifyDataSetChanged();
    }

    public void setFilterChip(String chip) {
        this.filterChip = chip == null ? "全部" : chip;
        applyFilter();
        notifyDataSetChanged();
    }

    private void applyFilter() {
        displayData.clear();
        for (SourceBean s : allData) {
            String name = (s.getName() == null ? "" : s.getName()).toLowerCase();
            boolean matchSearch = searchKeyword.isEmpty() || name.contains(searchKeyword);
            boolean matchChip = "全部".equals(filterChip) || name.contains(filterChip.toLowerCase());
            if (matchSearch && matchChip) {
                displayData.add(s);
            }
        }
    }

    public void setCurrentKey(String key) {
        this.currentKey = key == null ? "" : key;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_source_grid, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SourceBean s = displayData.get(position);
        h.tvName.setText(s.getName());
        boolean selected = currentKey.equals(s.getKey());
        h.tvName.setSelected(selected);
        h.itemView.setSelected(selected);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(s);
        });
    }

    @Override
    public int getItemCount() {
        return displayData.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
        }
    }
}
