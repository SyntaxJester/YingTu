package com.github.tvbox.osc.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.FavVod;

import java.util.ArrayList;
import java.util.List;

/**
 * 分享收藏（影片）网格适配器。
 */
public class FavVodAdapter extends RecyclerView.Adapter<FavVodAdapter.VH> {

    public interface OnItemClick { void onClick(FavVod f); }
    public interface OnItemLongClick { void onLongClick(FavVod f); }

    private final List<FavVod> data = new ArrayList<>();
    private OnItemClick clickListener;
    private OnItemLongClick longClickListener;

    public void setOnItemClick(OnItemClick l) { this.clickListener = l; }
    public void setOnItemLongClick(OnItemLongClick l) { this.longClickListener = l; }

    public void submit(List<FavVod> list) {
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
        FavVod f = data.get(position);
        h.tvName.setText(f.name);
        h.tvSub.setText("");
        Glide.with(h.itemView.getContext())
                .load(f.pic)
                .placeholder(R.drawable.bg_poster_placeholder)
                .error(R.drawable.bg_poster_placeholder)
                .centerCrop()
                .into(h.ivPoster);
        h.itemView.setOnClickListener(view -> {
            if (clickListener != null) clickListener.onClick(f);
        });
        h.itemView.setOnLongClickListener(view -> {
            if (longClickListener != null) longClickListener.onLongClick(f);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvName, tvSub;
        VH(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            tvName = itemView.findViewById(R.id.tvName);
            tvSub = itemView.findViewById(R.id.tvSub);
        }
    }
}
