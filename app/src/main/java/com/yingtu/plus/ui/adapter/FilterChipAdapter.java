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
 * 分类下的「筛选条件」横向 chip 行（如：全部/都市/逆袭/家庭/古装...）。
 * 对应原版选中分类后展开的一行筛选标签。
 */
public class FilterChipAdapter extends RecyclerView.Adapter<FilterChipAdapter.VH> {

    public interface OnValueClick {
        void onClick(MovieSort.Value value);
    }

    private final List<MovieSort.Value> data = new ArrayList<>();
    private int selected = 0;
    private OnValueClick listener;

    public void setOnValueClick(OnValueClick l) {
        this.listener = l;
    }

    public void submit(List<MovieSort.Value> list) {
        data.clear();
        if (list != null) data.addAll(list);
        selected = 0;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_filter_chip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MovieSort.Value val = data.get(position);
        h.tvName.setText(val.n);
        boolean sel = position == selected;
        h.tvName.setSelected(sel);
        h.itemView.setSelected(sel);
        h.itemView.setOnClickListener(v -> {
            int old = selected;
            selected = h.getBindingAdapterPosition();
            notifyItemChanged(old);
            notifyItemChanged(selected);
            if (listener != null) listener.onClick(val);
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
