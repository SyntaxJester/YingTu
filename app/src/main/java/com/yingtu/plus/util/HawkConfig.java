package com.yingtu.plus.com.yingtu.plus.util;

/**
 * Hawk 持久化存储用到的所有 key 常量。
 */
public class HawkConfig {
    public static final String API_CONFIG_CACHE = "api_config_cache";
    public static final String HOME_API = "home_api";
    public static final String API_URL = "api_url";
    public static final String PLAY_HISTORY = "play_history";
    public static final String SEARCH_HISTORY = "search_history";
    public static final String DARK_MODE = "dark_mode";
    public static final String HOME_REC = "home_rec";
    public static final String DEFAULT_PARSE = "default_parse";
    public static final String GRID_COLUMNS = "grid_columns";
    public static final String FAV_LINKS = "fav_links";
    public static final String FAV_VODS = "fav_vods";

    /** 播放内核：0=ExoPlayer 1=IJK 2=MPV */
    public static final String PLAYER_CORE = "player_core";

    /** WebHome 网页主页条目列表（List&lt;WebHomeEntry&gt;） */
    public static final String WEBHOME_ENTRIES = "webhome_entries";
    /** WebHome 内置示例主页是否已初始化 */
    public static final String WEBHOME_SEEDED = "webhome_seeded";
}
