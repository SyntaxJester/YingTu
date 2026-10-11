package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

/**
 * 启动页：品牌展示 1.2 秒后跳转到首页。
 */
public class FirstActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_first;
    }

    @Override
    protected void init() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        ImageView ivLogo = find(R.id.ivLogo);
        TextView tvVersion = find(R.id.tvVersion);

        // Logo 入场动画
        ScaleAnimation anim = new ScaleAnimation(
                0.7f, 1.0f, 0.7f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(600);
        anim.setFillAfter(true);
        ivLogo.startAnimation(anim);

        tvVersion.setText("v1.1.0");

        // 1.2 秒后跳转
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Intent i = new Intent(this, HomeActivity.class);
            startActivity(i);
            finish();
        }, 1200);
    }
}
