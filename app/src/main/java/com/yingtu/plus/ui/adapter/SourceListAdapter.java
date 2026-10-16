package com.yingtu.plus.com.yingtu.plus.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.bean.SourceBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频源列表适配器，当前选中源右侧显示勾选。
 */
public class SourceListAdapter extends RecyclerView.Adapter<SourceListAdapter.VH> {

    public interface OnSourceClick {
        void onClick(SourceBean source);
    }

    private final List<SourceBean> data = new ArrayList<>();
    private String currentKey = "";
    private OnSourceClick listener;

    public void setOnSourceClick(OnSourceClick l) {
        this.listener = l;
    }

    public void submit(List<SourceBean> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public void setCurrentKey(String key) {
        this.currentKey = key == null ? "" : key;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_source, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SourceBean s = data.get(position);
        h.tvName.setText(s.getName());
        h.tvApi.setText(s.getApi());
        boolean selected = currentKey.equals(s.getKey());
        h.ivCheck.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(s);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvApi;
        ImageView ivCheck;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvApi = itemView.findViewById(R.id.tvApi);
            ivCheck = itemView.findViewById(R.id.ivCheck);
        }
    }
}
