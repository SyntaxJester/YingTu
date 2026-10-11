package com.yingtu.plus.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.base.BaseLazyFragment;
import com.yingtu.plus.bean.WebHomeEntry;
import com.yingtu.plus.ui.activity.WebHomeActivity;
import com.yingtu.plus.ui.adapter.WebHomeAdapter;
import com.yingtu.plus.util.WebHomeHelper;

import java.util.List;

/**
 * 「网页首页」Tab（WebHome）。
 *
 * 列出所有网页首页条目：远程 URL / 本地 HTML 文件 / 内置示例。
 * 点击 → 用 {@link WebHomeActivity} 的内置 WebView 打开并注入 Native SDK。
 * 右下角 + 可添加远程网址，或选择设备上的本地 .html 文件（SAF 持久授权）。
 */
public class WebHomeFragment extends BaseLazyFragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private WebHomeAdapter adapter;

    private ActivityResultLauncher<String[]> openDocLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 必须在 STARTED 之前注册，放到 onCreate 最稳妥
        openDocLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::onLocalHtmlPicked);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_webhome;
    }

    @Override
    protected void init(View root) {
        recyclerView = root.findViewById(R.id.recyclerView);
        tvEmpty = root.findViewById(R.id.tvEmpty);

        root.findViewById(R.id.btnAdd).setOnClickListener(v -> showAddDialog());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WebHomeAdapter();
        adapter.setOnItemClick(this::open);
        adapter.setOnItemLongClick(this::confirmDelete);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onVisible() {
        refresh();
    }

    private void refresh() {
        List<WebHomeEntry> list = WebHomeHelper.getAll();
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submit(list);
    }

    private void open(WebHomeEntry entry) {
        String url;
        switch (entry.type) {
            case WebHomeEntry.TYPE_ASSET:
                url = "file:///android_asset/" + entry.target;
                break;
            case WebHomeEntry.TYPE_LOCAL:
                url = entry.target; // content:// 或 file://
                break;
            default:
                url = entry.target;
                break;
        }
        Intent i = new Intent(getActivity(), WebHomeActivity.class);
        i.putExtra(WebHomeActivity.KEY_URL, url);
        i.putExtra(WebHomeActivity.KEY_TITLE, entry.name);
        startActivity(i);
    }

    private void confirmDelete(WebHomeEntry entry) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setMessage("删除首页「" + entry.name + "」？\n（内置示例删除后不会自动恢复）")
                .setPositiveButton("删除", (d, w) -> {
                    WebHomeHelper.delete(entry.target);
                    refresh();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAddDialog() {
        View form = View.inflate(requireContext(), R.layout.dialog_add_webhome, null);
        EditText etName = form.findViewById(R.id.etName);
        EditText etUrl = form.findViewById(R.id.etUrl);
        RadioGroup rg = form.findViewById(R.id.rgType);
        View rowUrl = form.findViewById(R.id.rowUrl);
        TextView btnPick = form.findViewById(R.id.btnPickFile);

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            boolean local = checkedId == R.id.rbLocal;
            rowUrl.setVisibility(local ? View.GONE : View.VISIBLE);
            btnPick.setVisibility(local ? View.VISIBLE : View.GONE);
        });

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("添加网页首页")
                        .setView(form)
                        .setPositiveButton("保存", null)
                        .setNegativeButton("取消", null)
                        .create();

        btnPick.setOnClickListener(v -> {
            pendingName = etName.getText().toString().trim();
            openDocLauncher.launch(new String[]{"text/html", "application/xhtml+xml", "*/*"});
            dialog.dismiss();
        });

        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = etName.getText().toString().trim();
                    String url = etUrl.getText().toString().trim();
                    if (TextUtils.isEmpty(url)) {
                        toast("请输入网页地址 http(s)://");
                        return;
                    }
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        toast("地址需以 http:// 或 https:// 开头");
                        return;
                    }
                    if (TextUtils.isEmpty(name)) name = url;
                    WebHomeHelper.add(new WebHomeEntry(name, url, WebHomeEntry.TYPE_URL));
                    refresh();
                    dialog.dismiss();
                }));

        dialog.show();
    }

    private String pendingName = "";

    private void onLocalHtmlPicked(Uri uri) {
        if (uri == null) return;
        // 把选中的 .html 复制进应用内部存储，用 file:// 加载。
        // 这比直接让 WebView 加载 SAF content:// 更可靠（文档型 content URI 常被 WebView 拒绝）。
        String name = TextUtils.isEmpty(pendingName) ? guessName(uri) : pendingName;
        String fileUrl = copyToInternal(uri, name);
        pendingName = "";
        if (fileUrl == null) {
            toast("读取该文件失败，请换一个 .html 文件");
            return;
        }
        WebHomeHelper.add(new WebHomeEntry(name, fileUrl, WebHomeEntry.TYPE_LOCAL));
        refresh();
        toast("已添加本地 HTML 首页");
    }

    /** 复制 content:// 到 filesDir/webhome/<时间戳>.html，返回 file:// 地址，失败返回 null。 */
    private String copyToInternal(Uri uri, String name) {
        java.io.InputStream in = null;
        java.io.OutputStream out = null;
        try {
            java.io.File dir = new java.io.File(requireContext().getFilesDir(), "webhome");
            if (!dir.exists()) dir.mkdirs();
            String safe = name == null ? "page" : name.replaceAll("[^a-zA-Z0-9_.-]", "_");
            if (!safe.toLowerCase().endsWith(".html") && !safe.toLowerCase().endsWith(".htm")) safe += ".html";
            java.io.File dst = new java.io.File(dir, System.currentTimeMillis() + "_" + safe);
            in = requireContext().getContentResolver().openInputStream(uri);
            if (in == null) return null;
            out = new java.io.FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            out.flush();
            return Uri.fromFile(dst).toString();
        } catch (Throwable e) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
        }
    }

    private String guessName(Uri uri) {
        String last = uri.getLastPathSegment();
        if (TextUtils.isEmpty(last)) return "本地网页";
        int slash = last.lastIndexOf('/');
        if (slash >= 0) last = last.substring(slash + 1);
        return last;
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
