package com.github.tvbox.osc.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.LiveChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * 直播频道列表（右侧）。显示频道名 + 多线路角标。
 */
public class LiveChannelAdapter extends RecyclerView.Adapter<LiveChannelAdapter.VH> {

    public interface OnChannelClick {
        void onClick(LiveChannel channel);
    }

    private final List<LiveChannel> data = new ArrayList<>();
    private OnChannelClick listener;

    public void setOnChannelClick(OnChannelClick l) {
        this.listener = l;
    }

    public void submit(List<LiveChannel> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_live_channel, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LiveChannel c = data.get(position);
        h.tvName.setText(c.name);
        if (c.hasMultiLine()) {
            h.tvLine.setVisibility(View.VISIBLE);
            h.tvLine.setText(c.urls.size() + "线路");
        } else {
            h.tvLine.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvLine;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvLine = itemView.findViewById(R.id.tvLine);
        }
    }
}
