package com.yingtu.plus.bean;

import java.io.Serializable;

/**
 * 本地视频文件条目（来自 MediaStore）。
 */
public class LocalVideo implements Serializable {
    public String name;
    public String path;      // 文件绝对路径
    public String uri;       // content:// uri 字符串
    public long size;        // 字节
    public long duration;    // 毫秒
    public String bucket;    // 所属文件夹名

    public LocalVideo() {}
}
