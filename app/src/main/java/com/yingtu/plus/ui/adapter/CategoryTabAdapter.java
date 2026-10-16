package com.yingtu.plus.com.yingtu.plus.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.bean.MovieSort;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页顶部分类标签（主页 + 当前源的分类）。
 *
 * 选中态高亮；若该分类带筛选条件（hasFilter），标题右侧显示 ▽ 三角（像原版）。
 */
public class CategoryTabAdapter extends RecyclerView.Adapter<CategoryTabAdapter.VH> {

    public interface OnTabClick {
        void onClick(int position, MovieSort sort);
    }

    private final List<MovieSort> data = new ArrayList<>();
    private int selected = 0;
    private OnTabClick listener;

    public void setOnTabClick(OnTabClick l) {
        this.listener = l;
    }

    public void submit(List<MovieSort> list) {
        data.clear();
        if (list != null) data.addAll(list);
        selected = 0;
        notifyDataSetChanged();
    }

    public void setSelected(int pos) {
        if (pos == selected) return;
        int old = selected;
        selected = pos;
        notifyItemChanged(old);
        notifyItemChanged(pos);
    }

    public MovieSort getItem(int pos) {
        return pos >= 0 && pos < data.size() ? data.get(pos) : null;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_tab, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MovieSort s = data.get(position);
        boolean sel = position == selected;
        // 选中且有筛选 → 标题后加 ▽
        h.tvName.setText(sel && s.hasFilter() ? s.typeName + " ▽" : s.typeName);
        h.tvName.setSelected(sel);
        h.itemView.setSelected(sel);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(h.getBindingAdapterPosition(), s);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
        }
    }
}
