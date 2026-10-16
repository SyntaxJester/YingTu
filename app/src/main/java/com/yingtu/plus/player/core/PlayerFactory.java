package com.yingtu.plus.player.core;

import android.content.Context;

import com.yingtu.plus.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

/**
 * 播放内核工厂：按用户在设置里选择的内核（Hawk PLAYER_CORE）创建实例。
 * 默认 ExoPlayer；IJK / MPV 失败时安全回退到 Exo。
 */
public class PlayerFactory {

    private PlayerFactory() {}

    public static int currentCore() {
        return Hawk.get(HawkConfig.PLAYER_CORE, AbstractPlayer.CORE_EXO);
    }

    public static void setCore(int core) {
        Hawk.put(HawkConfig.PLAYER_CORE, core);
    }

    public static String coreName(int core) {
        switch (core) {
            case AbstractPlayer.CORE_IJK: return "IJK";
            case AbstractPlayer.CORE_MPV: return "MPV";
            default: return "ExoPlayer";
        }
    }

    public static AbstractPlayer create(Context context, int core) {
        try {
            if (core == AbstractPlayer.CORE_IJK) return new IjkPlayerCore(context);
            if (core == AbstractPlayer.CORE_MPV) return new MpvPlayerCore(context);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return new ExoPlayerCore(context);
    }

    public static AbstractPlayer create(Context context) {
        return create(context, currentCore());
    }
}
