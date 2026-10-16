package com.yingtu.plus.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yingtu.plus.R;
import com.yingtu.plus.bean.Movie;

import java.util.ArrayList;
import java.util.List;

/**
 * 影片网格适配器（海报墙）。首页推荐、分类列表、搜索结果共用。
 */
public class GridMovieAdapter extends RecyclerView.Adapter<GridMovieAdapter.VH> {

    public interface OnItemClick {
        void onClick(Movie movie);
    }

    private final List<Movie> data = new ArrayList<>();
    private OnItemClick listener;

    public void setOnItemClick(OnItemClick l) {
        this.listener = l;
    }

    public void submit(List<Movie> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public void append(List<Movie> list) {
        if (list == null || list.isEmpty()) return;
        int start = data.size();
        data.addAll(list);
        notifyItemRangeInserted(start, list.size());
    }

    public int getDataCount() {
        return data.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grid_movie, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Movie m = data.get(position);
        h.tvName.setText(m.name);

        if (m.note == null || m.note.isEmpty()) {
            h.tvNote.setVisibility(View.GONE);
        } else {
            h.tvNote.setVisibility(View.VISIBLE);
            h.tvNote.setText(m.note);
        }

        if (m.rate == null || m.rate.isEmpty()) {
            h.tvRating.setVisibility(View.GONE);
        } else {
            h.tvRating.setVisibility(View.VISIBLE);
            h.tvRating.setText(m.rate);
        }

        h.tvBadge.setVisibility(m.douban ? View.VISIBLE : View.GONE);

        Context ctx = h.itemView.getContext();
        Glide.with(ctx)
                .load(m.pic)
                .placeholder(R.drawable.bg_poster_placeholder)
                .error(R.drawable.bg_poster_placeholder)
                .centerCrop()
                .into(h.ivPoster);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(m);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvName;
        TextView tvNote;
        TextView tvRating;
        TextView tvBadge;

        VH(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            tvName = itemView.findViewById(R.id.tvName);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvBadge = itemView.findViewById(R.id.tvBadge);
        }
    }
}
