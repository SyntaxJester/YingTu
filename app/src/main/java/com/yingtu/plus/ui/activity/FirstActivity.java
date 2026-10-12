package com.yingtu.plus.ui.activity;

import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import com.yingtu.plus.R;
import com.yingtu.plus.base.BaseActivity;

/**
 * 启动页：仅显示 Logo 图标 + 应用名，1.2 秒后跳转到首页。
 */
public class FirstActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_first;
    }

    @Override
    protected void init() {
        // 应用自定义壁纸
        android.net.Uri wp = com.yingtu.plus.util.WallpaperHelper.getWallpaperUri(this);
        if (wp != null) {
            try {
                android.graphics.Bitmap bmp =
                        android.graphics.BitmapFactory.decodeStream(
                                getContentResolver().openInputStream(wp));
                if (bmp != null) {
                    getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.BitmapDrawable(bmp));
                }
            } catch (Exception ignored) {}
        }
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        ImageView ivLogo = find(R.id.ivLogo);

        // Logo 入场动画
        ScaleAnimation anim = new ScaleAnimation(
                0.7f, 1.0f, 0.7f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(600);
        anim.setFillAfter(true);
        ivLogo.startAnimation(anim);

        // 1.2 秒后跳转
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Intent i = new Intent(this, HomeActivity.class);
            startActivity(i);
            finish();
        }, 1200);
    }
}
