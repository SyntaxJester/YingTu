package com.yingtu.plus.com.yingtu.plus.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.bean.LocalVideo;
import com.yingtu.plus.util.MediaScanner;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地视频列表适配器。
 */
public class LocalVideoAdapter extends RecyclerView.Adapter<LocalVideoAdapter.VH> {

    public interface OnItemClick {
        void onClick(LocalVideo video);
    }

    private final List<LocalVideo> data = new ArrayList<>();
    private OnItemClick listener;

    public void setOnItemClick(OnItemClick l) {
        this.listener = l;
    }

    public void submit(List<LocalVideo> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_local_video, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LocalVideo v = data.get(position);
        h.tvName.setText(v.name);
        StringBuilder meta = new StringBuilder();
        if (v.bucket != null && !v.bucket.isEmpty()) meta.append(v.bucket);
        String dur = MediaScanner.formatDuration(v.duration);
        String size = MediaScanner.formatSize(v.size);
        if (!dur.isEmpty()) { if (meta.length() > 0) meta.append(" · "); meta.append(dur); }
        if (!size.isEmpty()) { if (meta.length() > 0) meta.append(" · "); meta.append(size); }
        h.tvMeta.setText(meta.toString());
        h.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onClick(v);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvMeta;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvMeta = itemView.findViewById(R.id.tvMeta);
        }
    }
}
