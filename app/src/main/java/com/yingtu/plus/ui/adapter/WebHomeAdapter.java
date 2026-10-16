package com.yingtu.plus.com.yingtu.plus.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.bean.WebHomeEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * 网页首页（WebHome）列表适配器。
 */
public class WebHomeAdapter extends RecyclerView.Adapter<WebHomeAdapter.VH> {

    public interface OnItemClick {
        void onClick(WebHomeEntry entry);
    }

    public interface OnItemLongClick {
        void onLongClick(WebHomeEntry entry);
    }

    private final List<WebHomeEntry> data = new ArrayList<>();
    private OnItemClick onItemClick;
    private OnItemLongClick onItemLongClick;

    public void setOnItemClick(OnItemClick l) {
        this.onItemClick = l;
    }

    public void setOnItemLongClick(OnItemLongClick l) {
        this.onItemLongClick = l;
    }

    public void submit(List<WebHomeEntry> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_webhome, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        WebHomeEntry entry = data.get(position);
        h.tvName.setText(entry.name);
        h.tvSub.setText(entry.subtitle());
        h.tvBadge.setVisibility(entry.builtin ? View.VISIBLE : View.GONE);
        int icon;
        switch (entry.type) {
            case WebHomeEntry.TYPE_LOCAL: icon = R.drawable.ic_file; break;
            case WebHomeEntry.TYPE_ASSET: icon = R.drawable.ic_cast; break;
            default: icon = R.drawable.ic_web; break;
        }
        h.ivIcon.setImageResource(icon);

        h.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(entry);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (onItemLongClick != null) onItemLongClick.onLongClick(entry);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvSub, tvBadge;

        VH(@NonNull View v) {
            super(v);
            ivIcon = v.findViewById(R.id.ivIcon);
            tvName = v.findViewById(R.id.tvName);
            tvSub = v.findViewById(R.id.tvSub);
            tvBadge = v.findViewById(R.id.tvBadge);
        }
    }
}
