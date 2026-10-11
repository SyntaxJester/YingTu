package com.github.tvbox.osc.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.MovieSort;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类页左侧的分类标签列表适配器（单选高亮）。
 */
public class SortTabAdapter extends RecyclerView.Adapter<SortTabAdapter.VH> {

    public interface OnTabSelected {
        void onSelected(MovieSort sort);
    }

    private final List<MovieSort> data = new ArrayList<>();
    private int selected = 0;
    private OnTabSelected listener;

    public void setOnTabSelected(OnTabSelected l) {
        this.listener = l;
    }

    public void submit(List<MovieSort> list) {
        data.clear();
        if (list != null) data.addAll(list);
        selected = 0;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sort_tab, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MovieSort s = data.get(position);
        h.tv.setText(s.typeName);
        h.tv.setSelected(position == selected);
        h.itemView.setOnClickListener(v -> {
            int old = selected;
            selected = h.getAdapterPosition();
            notifyItemChanged(old);
            notifyItemChanged(selected);
            if (listener != null) listener.onSelected(s);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvTab);
        }
    }
}
