package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.FavVod;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.adapter.FavVodAdapter;
import com.github.tvbox.osc.util.FavHelper;

import java.util.List;

/**
 * 「分享收藏」Tab：收藏过的影片网格；点击进详情，长按删除/分享。
 */
public class FavVodFragment extends BaseLazyFragment {

    private RecyclerView recyclerView;
    private View tvEmpty;
    private FavVodAdapter adapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_fav_vod;
    }

    @Override
    protected void init(View root) {
        recyclerView = root.findViewById(R.id.recyclerView);
        tvEmpty = root.findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new FavVodAdapter();
        adapter.setOnItemClick(this::openDetail);
        adapter.setOnItemLongClick(this::showMenu);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onVisible() {
        refresh();
    }

    private void refresh() {
        List<FavVod> list = FavHelper.getVods();
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submit(list);
    }

    private void openDetail(FavVod f) {
        Intent i = new Intent(getActivity(), DetailActivity.class);
        i.putExtra(DetailActivity.KEY_SOURCE, f.sourceKey);
        i.putExtra(DetailActivity.KEY_VOD_ID, f.vodId);
        startActivity(i);
    }

    private void showMenu(FavVod f) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(f.name)
                .setItems(new String[]{"分享", "删除收藏"}, (d, which) -> {
                    if (which == 0) share(f);
                    else {
                        FavHelper.deleteVod(f.key());
                        refresh();
                    }
                })
                .show();
    }

    private void share(FavVod f) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, f.name);
        send.putExtra(Intent.EXTRA_TEXT, "【影图+ 推荐】" + f.name
                + "\n来源：" + f.sourceKey + "\n影片ID：" + f.vodId);
        startActivity(Intent.createChooser(send, "分享影片"));
    }
}
