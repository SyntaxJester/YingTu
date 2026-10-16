package com.yingtu.plus.com.yingtu.plus.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.yingtu.plus.bean.LocalVideo;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地视频扫描器：通过 MediaStore 查询设备上的视频文件。
 */
public class MediaScanner {

    private MediaScanner() {}

    public static List<LocalVideo> scanVideos(Context context) {
        List<LocalVideo> list = new ArrayList<>();
        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        };
        String sort = MediaStore.Video.Media.DATE_MODIFIED + " DESC";

        try (Cursor c = context.getContentResolver()
                .query(collection, projection, null, null, sort)) {
            if (c == null) return list;
            int idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int dataCol = c.getColumnIndex(MediaStore.Video.Media.DATA);
            int sizeCol = c.getColumnIndex(MediaStore.Video.Media.SIZE);
            int durCol = c.getColumnIndex(MediaStore.Video.Media.DURATION);
            int bucketCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);

            while (c.moveToNext()) {
                LocalVideo v = new LocalVideo();
                long id = c.getLong(idCol);
                v.uri = ContentUris.withAppendedId(collection, id).toString();
                v.name = c.getString(nameCol);
                if (dataCol >= 0) v.path = c.getString(dataCol);
                if (sizeCol >= 0) v.size = c.getLong(sizeCol);
                if (durCol >= 0) v.duration = c.getLong(durCol);
                if (bucketCol >= 0) v.bucket = c.getString(bucketCol);
                list.add(v);
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    public static String formatSize(long bytes) {
        if (bytes <= 0) return "";
        if (bytes >= 1024 * 1024 * 1024) return String.format("%.1f GB", bytes / 1024.0 / 1024 / 1024);
        if (bytes >= 1024 * 1024) return String.format("%.0f MB", bytes / 1024.0 / 1024);
        return String.format("%.0f KB", bytes / 1024.0);
    }

    public static String formatDuration(long ms) {
        if (ms <= 0) return "";
        long s = ms / 1000;
        long m = s / 60;
        long h = m / 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m % 60, s % 60);
        return String.format("%d:%02d", m, s % 60);
    }
}
