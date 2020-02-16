/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.atmko.onmywatch.models.MediaLog;

import java.util.List;

@Dao
public interface MediaLogsDao {
    @Insert()
    void addMediaLog(MediaLog mediaLog);

    @Query("SELECT * FROM media_logs "
            + "WHERE parent_id = :parentId")
    List<MediaLog> getAllLogsWithMediaIdAlt(String parentId);

    @Query("SELECT * FROM media_logs "
            + "WHERE condition = 1 "
            + "ORDER BY timestamp ASC "
            + "LIMIT 10")
    LiveData<List<MediaLog>> getUpcoming();

    @Query("SELECT * FROM media_logs "
            + "WHERE condition = 2 "
            + "ORDER BY timestamp DESC "
            + "LIMIT 10")
    LiveData<List<MediaLog>> getAired();

    @Query("SELECT * FROM media_logs "
            + "WHERE condition = 3 "
            + "LIMIT 10")
    LiveData<List<MediaLog>> getUndated();

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateMediaLog(MediaLog mediaLog);

    @Delete
    void deleteMediaLog(MediaLog mediaLog);
}
