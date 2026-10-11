package com.github.tvbox.osc.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.LiveGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 直播分组列表（左侧栏，单选高亮）。
 */
public class LiveGroupAdapter extends RecyclerView.Adapter<LiveGroupAdapter.VH> {

    public interface OnGroupSelected {
        void onSelected(int index);
    }

    private final List<LiveGroup> data = new ArrayList<>();
    private int selected = 0;
    private OnGroupSelected listener;

    public void setOnGroupSelected(OnGroupSelected l) {
        this.listener = l;
    }

    public void submit(List<LiveGroup> list) {
        data.clear();
        if (list != null) data.addAll(list);
        selected = 0;
        notifyDataSetChanged();
    }

    public void setSelected(int index) {
        int old = selected;
        selected = index;
        notifyItemChanged(old);
        notifyItemChanged(index);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_live_group, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LiveGroup g = data.get(position);
        h.tv.setText(g.name);
        h.tv.setSelected(position == selected);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSelected(h.getAdapterPosition());
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
