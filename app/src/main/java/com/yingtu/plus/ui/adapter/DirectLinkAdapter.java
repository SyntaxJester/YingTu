package com.yingtu.plus.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.bean.DirectLink;

import java.util.ArrayList;
import java.util.List;

/**
 * 直链收藏列表适配器。右侧提供分享按钮，整行点击播放，长按删除。
 */
public class DirectLinkAdapter extends RecyclerView.Adapter<DirectLinkAdapter.VH> {

    public interface OnItemClick { void onClick(DirectLink link); }
    public interface OnItemLongClick { void onLongClick(DirectLink link); }
    public interface OnShareClick { void onShare(DirectLink link); }

    private final List<DirectLink> data = new ArrayList<>();
    private OnItemClick clickListener;
    private OnItemLongClick longClickListener;
    private OnShareClick shareListener;

    public void setOnItemClick(OnItemClick l) { this.clickListener = l; }
    public void setOnItemLongClick(OnItemLongClick l) { this.longClickListener = l; }
    public void setOnShareClick(OnShareClick l) { this.shareListener = l; }

    public void submit(List<DirectLink> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_direct_link, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DirectLink link = data.get(position);
        h.tvName.setText(link.name);
        h.tvUrl.setText(link.url);
        h.itemView.setOnClickListener(view -> {
            if (clickListener != null) clickListener.onClick(link);
        });
        h.itemView.setOnLongClickListener(view -> {
            if (longClickListener != null) longClickListener.onLongClick(link);
            return true;
        });
        h.ivShare.setOnClickListener(view -> {
            if (shareListener != null) shareListener.onShare(link);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvUrl;
        ImageView ivShare;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvUrl = itemView.findViewById(R.id.tvUrl);
            ivShare = itemView.findViewById(R.id.ivShare);
        }
    }
}
