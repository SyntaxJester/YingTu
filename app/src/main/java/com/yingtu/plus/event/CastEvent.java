package com.yingtu.plus.com.yingtu.plus.event;

import com.yingtu.plus.bean.Config;
import com.yingtu.plus.bean.Device;
import com.yingtu.plus.bean.History;

import org.greenrobot.eventbus.EventBus;

public record CastEvent(Config config, Device device, History history) {

    public static void post(Config config, Device device, History history) {
        EventBus.getDefault().post(new CastEvent(config, device, history));
    }
}
