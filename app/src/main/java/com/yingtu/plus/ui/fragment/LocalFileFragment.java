package com.yingtu.plus.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yingtu.plus.R;
import com.yingtu.plus.base.BaseLazyFragment;
import com.yingtu.plus.bean.LocalVideo;
import com.yingtu.plus.ui.activity.LivePlayActivity;
import com.yingtu.plus.ui.adapter.LocalVideoAdapter;
import com.yingtu.plus.util.MediaScanner;
import com.yingtu.plus.util.OkGo;

import java.util.List;

/**
 * 「本地文件」Tab：扫描设备视频并列表展示，点击用内置播放器播放。
 */
public class LocalFileFragment extends BaseLazyFragment {

    private static final int REQ_PERM = 1001;

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private View progress;
    private LocalVideoAdapter adapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_list;
    }

    @Override
    protected void init(View root) {
        recyclerView = root.findViewById(R.id.recyclerView);
        tvEmpty = root.findViewById(R.id.tvEmpty);
        progress = root.findViewById(R.id.progress);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LocalVideoAdapter();
        adapter.setOnItemClick(this::playLocal);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void lazyLoad() {
        ensurePermissionThenScan();
    }

    private void ensurePermissionThenScan() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_VIDEO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), perm)
                == PackageManager.PERMISSION_GRANTED) {
            scan();
        } else {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{perm}, REQ_PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scan();
            } else {
                showEmpty("未授予存储权限，无法扫描本地视频");
            }
        }
    }

    private void scan() {
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        OkGo.io(() -> {
            List<LocalVideo> videos = MediaScanner.scanVideos(requireContext());
            OkGo.main(() -> {
                progress.setVisibility(View.GONE);
                if (videos.isEmpty()) {
                    showEmpty("没有扫描到本地视频文件");
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    adapter.submit(videos);
                }
            });
        });
    }

    private void playLocal(LocalVideo v) {
        Intent i = new Intent(getActivity(), LivePlayActivity.class);
        i.putExtra(LivePlayActivity.KEY_NAME, v.name);
        i.putExtra(LivePlayActivity.KEY_URL, v.uri != null ? v.uri : v.path);
        startActivity(i);
    }

    private void showEmpty(String msg) {
        progress.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(msg);
    }
}
