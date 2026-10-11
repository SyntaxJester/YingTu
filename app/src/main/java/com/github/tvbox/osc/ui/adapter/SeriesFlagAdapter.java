package com.github.tvbox.osc.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VodInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 线路切换标签（横向），如「线路1 / 线路2」。
 */
public class SeriesFlagAdapter extends RecyclerView.Adapter<SeriesFlagAdapter.VH> {

    public interface OnFlagSelected {
        void onSelected(int index);
    }

    private final List<VodInfo.VodSeriesFlag> data = new ArrayList<>();
    private int selected = 0;
    private OnFlagSelected listener;

    public void setOnFlagSelected(OnFlagSelected l) {
        this.listener = l;
    }

    public void submit(List<VodInfo.VodSeriesFlag> list) {
        data.clear();
        if (list != null) data.addAll(list);
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
                .inflate(R.layout.item_series_flag, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VodInfo.VodSeriesFlag flag = data.get(position);
        h.tv.setText(flag.name);
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
            tv = itemView.findViewById(R.id.tvFlag);
        }
    }
}
