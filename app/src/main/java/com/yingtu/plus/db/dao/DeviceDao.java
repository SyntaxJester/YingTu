package com.yingtu.plus.com.yingtu.plus.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.yingtu.plus.bean.Device;

import java.util.List;

@Dao
public abstract class DeviceDao extends BaseDao<Device> {

    @Query("SELECT * FROM Device")
    public abstract List<Device> findAll();

    @Query("DELETE FROM Device")
    public abstract void delete();

    @Query("DELETE FROM Device WHERE uuid = :uuid")
    public abstract void delete(String uuid);
}
