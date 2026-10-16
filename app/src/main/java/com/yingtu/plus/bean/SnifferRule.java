package com.yingtu.plus.com.yingtu.plus.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 嗅探规则（参考 FongMi/webhtv 的 Rule）。
 *
 * 对应 TVBox 配置里 rules 数组的条目：按 host 命中后，
 * 使用 regex 判定命中直链、exclude 排除、script 注入 JS 辅助嗅探。
 */
public class SnifferRule implements Serializable {

    public String name;
    public List<String> hosts = new ArrayList<>();
    public List<String> regex = new ArrayList<>();
    public List<String> script = new ArrayList<>();
    public List<String> exclude = new ArrayList<>();

    public static SnifferRule empty() {
        return new SnifferRule();
    }
}
