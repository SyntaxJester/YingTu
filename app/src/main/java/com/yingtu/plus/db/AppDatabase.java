package com.yingtu.plus.com.yingtu.plus.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.yingtu.plus.App;
import com.yingtu.plus.bean.Config;
import com.yingtu.plus.bean.Device;
import com.yingtu.plus.bean.History;
import com.yingtu.plus.bean.Keep;
import com.yingtu.plus.bean.Live;
import com.yingtu.plus.bean.PlaybackDeleteTombstone;
import com.yingtu.plus.bean.Site;
import com.yingtu.plus.bean.Track;
import com.yingtu.plus.db.dao.ConfigDao;
import com.yingtu.plus.db.dao.DeviceDao;
import com.yingtu.plus.db.dao.HistoryDao;
import com.yingtu.plus.db.dao.KeepDao;
import com.yingtu.plus.db.dao.LiveDao;
import com.yingtu.plus.db.dao.PlaybackDeleteTombstoneDao;
import com.yingtu.plus.db.dao.SiteDao;
import com.yingtu.plus.db.dao.TrackDao;
import com.yingtu.plus.utils.AppBackup;
import com.yingtu.plus.utils.Notify;
import com.yingtu.plus.utils.Task;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Path;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Database(entities = {Keep.class, Site.class, Live.class, Track.class, Config.class, Device.class, History.class, PlaybackDeleteTombstone.class}, version = AppDatabase.VERSION)
public abstract class AppDatabase extends RoomDatabase {

    public static final int VERSION = 37;
    public static final String NAME = "tv";
    public static final String SYMBOL = "@@@";

    private static volatile AppDatabase instance;

    public static synchronized AppDatabase get() {
        if (instance == null) instance = create(App.get());
        return instance;
    }

    public static void backup() {
        backup(new com.fongmi.android.tv.impl.Callback());
    }

    public static void backup(com.fongmi.android.tv.impl.Callback callback) {
        backup(callback, null);
    }

    public static void backup(com.fongmi.android.tv.impl.Callback callback, AppBackup.Progress progress) {
        Task.execute(() -> {
            File file = new File(Path.tv(), AppBackup.fileName());
            try {
                AppBackup.CreateResult result = AppBackup.create(file, progress);
                App.post(() -> {
                    callback.success();
                    if (result.hasWarning()) Notify.show(result.warning);
                });
                cleanOld();
            } catch (Exception e) {
                SpiderDebug.log("backup", "local create failed error=%s", e.getMessage());
                App.post(callback::error);
            }
        });
    }

    public static void restore(File file, com.fongmi.android.tv.impl.Callback callback) {
        restore(file, callback, null);
    }

    public static void restore(File file, com.fongmi.android.tv.impl.Callback callback, AppBackup.Progress progress) {
        Task.execute(() -> {
            try {
                AppBackup.RestoreResult result = AppBackup.restore(file, progress);
                App.post(() -> {
                    callback.success();
                    if (result.hasWarning()) Notify.show(result.warning);
                });
            } catch (Exception e) {
                SpiderDebug.log("backup", "local restore failed file=%s error=%s", file == null ? "" : file.getAbsolutePath(), e.getMessage());
                App.post(callback::error);
            }
        });
    }

    private static void cleanOld() {
        List<File> items = new ArrayList<>();
        File[] files = Path.tv().listFiles();
        if (files == null) files = new File[0];
        for (File file : files) if (AppBackup.isBackup(file)) items.add(file);
        if (!items.isEmpty()) items.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        if (items.size() > 7) for (int i = 7; i < items.size(); i++) Path.clear(items.get(i));
    }

    private static AppDatabase create(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, NAME)
                .addMigrations(Migrations.MIGRATION_30_31)
                .addMigrations(Migrations.MIGRATION_31_32)
                .addMigrations(Migrations.MIGRATION_32_33)
                .addMigrations(Migrations.MIGRATION_33_34)
                .addMigrations(Migrations.MIGRATION_34_35)
                .addMigrations(Migrations.MIGRATION_35_36)
                .addMigrations(Migrations.MIGRATION_36_37)
                .fallbackToDestructiveMigration(true)
                .allowMainThreadQueries().build();
    }

    public abstract KeepDao getKeepDao();

    public abstract SiteDao getSiteDao();

    public abstract LiveDao getLiveDao();

    public abstract TrackDao getTrackDao();

    public abstract ConfigDao getConfigDao();

    public abstract DeviceDao getDeviceDao();

    public abstract HistoryDao getHistoryDao();

    public abstract PlaybackDeleteTombstoneDao getPlaybackDeleteTombstoneDao();
}
