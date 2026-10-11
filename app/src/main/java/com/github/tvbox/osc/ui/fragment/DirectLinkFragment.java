package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.DirectLink;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;
import com.github.tvbox.osc.ui.adapter.DirectLinkAdapter;
import com.github.tvbox.osc.util.FavHelper;

import java.util.List;

/**
 * 「直链收藏」Tab：手动添加可直接播放的地址并保存；点击播放、长按删除、可分享。
 */
public class DirectLinkFragment extends BaseLazyFragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private DirectLinkAdapter adapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_fav_link;
    }

    @Override
    protected void init(View root) {
        recyclerView = root.findViewById(R.id.recyclerView);
        tvEmpty = root.findViewById(R.id.tvEmpty);

        root.findViewById(R.id.btnAdd).setOnClickListener(v -> showAddDialog());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DirectLinkAdapter();
        adapter.setOnItemClick(this::play);
        adapter.setOnItemLongClick(this::confirmDelete);
        adapter.setOnShareClick(this::share);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onVisible() {
        refresh();
    }

    private void refresh() {
        List<DirectLink> list = FavHelper.getLinks();
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submit(list);
    }

    private void showAddDialog() {
        View form = View.inflate(requireContext(), R.layout.dialog_add_link, null);
        EditText etName = form.findViewById(R.id.etName);
        EditText etUrl = form.findViewById(R.id.etUrl);
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("添加直链")
                .setView(form)
                .setPositiveButton("保存", (d, w) -> {
                    String url = etUrl.getText().toString().trim();
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(url)) {
                        toast("请输入播放地址");
                        return;
                    }
                    if (TextUtils.isEmpty(name)) name = "直链 " + (FavHelper.getLinks().size() + 1);
                    FavHelper.addLink(new DirectLink(name, url));
                    refresh();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void play(DirectLink link) {
        Intent i = new Intent(getActivity(), LivePlayActivity.class);
        i.putExtra(LivePlayActivity.KEY_NAME, link.name);
        i.putExtra(LivePlayActivity.KEY_URL, link.url);
        startActivity(i);
    }

    private void confirmDelete(DirectLink link) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setMessage("删除「" + link.name + "」？")
                .setPositiveButton("删除", (d, w) -> {
                    FavHelper.deleteLink(link.url);
                    refresh();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void share(DirectLink link) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, link.name);
        send.putExtra(Intent.EXTRA_TEXT, link.name + "\n" + link.url);
        startActivity(Intent.createChooser(send, "分享直链"));
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
