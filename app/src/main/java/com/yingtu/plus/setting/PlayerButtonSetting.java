package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class PlayerButtonSetting {
    public static final String PLAYER = "player";
    public static final String DECODE = "decode";
    public static final String SPEED = "speed";
    public static final String SCALE = "scale";
    public static final String TEXT = "text";
    public static final String AUDIO = "audio";
    public static final String VIDEO = "video";
    public static final String PREV = "prev";
    public static final String NEXT = "next";
    public static final String EPISODES = "episodes";
    public static final String FULLSCREEN = "fullscreen";
    public static final String DANMAKU = "danmaku";
    public static final String RESET = "reset";
    public static final String REPEAT = "repeat";

    private static final String ORDER_KEY = "player_button_order";
    private static final String HIDDEN_KEY = "player_button_hidden";
    
    private static final List<String> DEFAULT_ORDER = Arrays.asList(
        PLAYER, DECODE, SPEED, SCALE, TEXT, AUDIO, VIDEO, 
        PREV, NEXT, EPISODES, FULLSCREEN, DANMAKU, RESET, REPEAT
    );

    public static List<String> getOrder() {
        String stored = Prefers.getString(ORDER_KEY, null);
        if (stored == null) return new ArrayList<>(DEFAULT_ORDER);
        return Arrays.asList(stored.split(","));
    }

    public static void putOrder(List<String> order) {
        Prefers.put(ORDER_KEY, String.join(",", order));
    }

    public static List<String> getHidden() {
        String stored = Prefers.getString(HIDDEN_KEY, null);
        if (stored == null) return new ArrayList<>();
        return Arrays.asList(stored.split(","));
    }

    public static boolean isVisible(String btn) {
        return !getHidden().contains(btn);
    }

    public static void applyOrder(List<String> buttons) {
        // Apply ordering logic
    }

    public static void applyVisibility(List<String> buttons) {
        // Apply visibility logic
    }
}
