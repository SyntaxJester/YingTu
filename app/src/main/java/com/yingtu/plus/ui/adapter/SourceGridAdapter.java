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
import java.util.List;

/**
 * 站源选择抽屉里的 2 列网格适配器（参考原版「站源选择」右侧抽屉）。
 * 当前选中源高亮。
 */
public class SourceGridAdapter extends RecyclerView.Adapter<SourceGridAdapter.VH> {

    public interface OnSourceClick {
        void onClick(SourceBean source);
    }

    private final List<SourceBean> data = new ArrayList<>();
    private String currentKey = "";
    private OnSourceClick listener;

    public void setOnSourceClick(OnSourceClick l) {
        this.listener = l;
    }

    public void submit(List<SourceBean> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
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
        SourceBean s = data.get(position);
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
