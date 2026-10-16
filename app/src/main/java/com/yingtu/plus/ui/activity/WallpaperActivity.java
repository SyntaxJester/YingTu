package com.yingtu.plus.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.yingtu.plus.R;
import com.yingtu.plus.base.BaseActivity;
import com.yingtu.plus.util.HawkConfig;
import com.yingtu.plus.util.WallpaperHelper;
import com.orhanobut.hawk.Hawk;

/**
 * 壁纸设置页：预览 + 从相册选择 + 恢复默认。
 * 选中的壁纸应用到首页/启动页背景。
 */
public class WallpaperActivity extends BaseActivity {

    private ImageView ivWallpaper;
    private ActivityResultLauncher<Intent> wallpaperPicker;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_wallpaper;
    }

    @Override
    protected void init() {
        ivWallpaper = find(R.id.ivWallpaper);

        // 注册 SAF / photo picker
        wallpaperPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri uri = data.getData();
                            WallpaperHelper.setWallpaper(this, uri);
                            refreshPreview();
                            Toast.makeText(this, "壁纸已更新", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        find(R.id.btnPickWallpaper).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            wallpaperPicker.launch(i);
        });

        find(R.id.btnResetWallpaper).setOnClickListener(v -> {
            WallpaperHelper.resetWallpaper(this);
            refreshPreview();
            Toast.makeText(this, "已恢复默认背景", Toast.LENGTH_SHORT).show();
        });

        refreshPreview();
    }

    private void refreshPreview() {
        Uri wallpaper = WallpaperHelper.getWallpaperUri(this);
        if (wallpaper != null) {
            ivWallpaper.setImageURI(wallpaper);
            ivWallpaper.setBackgroundResource(0);
        } else {
            ivWallpaper.setImageURI(null);
            ivWallpaper.setBackgroundResource(R.drawable.bg_main_gradient);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPreview();
    }
}
