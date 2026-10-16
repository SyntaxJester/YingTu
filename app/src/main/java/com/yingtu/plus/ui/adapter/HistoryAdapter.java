package com.yingtu.plus.com.yingtu.plus.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yingtu.plus.R;
import com.yingtu.plus.bean.History;

import java.util.ArrayList;
import java.util.List;

/**
 * 观看历史列表适配器。显示封面 + 片名 + 「观看至 X · 进度%」，
 * 支持点击续播与长按删除。
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public interface OnHistoryClick {
        void onClick(History history);
    }

    public interface OnHistoryLongClick {
        void onLongClick(History history);
    }

    private final List<History> data = new ArrayList<>();
    private OnHistoryClick clickListener;
    private OnHistoryLongClick longClickListener;

    public void setOnHistoryClick(OnHistoryClick l) {
        this.clickListener = l;
    }

    public void setOnHistoryLongClick(OnHistoryLongClick l) {
        this.longClickListener = l;
    }

    public void submit(List<History> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        History item = data.get(position);
        h.tvName.setText(item.vodName == null ? "" : item.vodName);

        StringBuilder sub = new StringBuilder();
        if (item.episodeName != null && !item.episodeName.isEmpty()) {
            sub.append("观看至 ").append(item.episodeName);
        }
        int percent = item.percent();
        if (percent > 0) {
            if (sub.length() > 0) sub.append(" · ");
            sub.append(percent).append("%");
        }
        h.tvSub.setText(sub.toString());

        Glide.with(h.itemView.getContext())
                .load(item.vodPic)
                .placeholder(R.drawable.bg_poster_placeholder)
                .error(R.drawable.bg_poster_placeholder)
                .centerCrop()
                .into(h.ivPoster);

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(item);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onLongClick(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvName;
        TextView tvSub;

        VH(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            tvName = itemView.findViewById(R.id.tvName);
            tvSub = itemView.findViewById(R.id.tvSub);
        }
    }
}
