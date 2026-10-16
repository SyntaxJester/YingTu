package com.yingtu.plus.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.bean.VodInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 选集网格适配器，当前播放集高亮。
 */
public class SeriesAdapter extends RecyclerView.Adapter<SeriesAdapter.VH> {

    public interface OnSeriesClick {
        void onClick(int index);
    }

    private final List<VodInfo.VodSeries> data = new ArrayList<>();
    private int playing = -1;
    private OnSeriesClick listener;

    public void setOnSeriesClick(OnSeriesClick l) {
        this.listener = l;
    }

    public void submit(List<VodInfo.VodSeries> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public void setPlaying(int index) {
        int old = playing;
        playing = index;
        notifyItemChanged(old);
        notifyItemChanged(index);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_series, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VodInfo.VodSeries s = data.get(position);
        h.tv.setText(s.name);
        h.tv.setSelected(position == playing);
        h.itemView.setOnClickListener(v -> {
            setPlaying(h.getAdapterPosition());
            if (listener != null) listener.onClick(h.getAdapterPosition());
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
            tv = itemView.findViewById(R.id.tvSeries);
        }
    }
}
