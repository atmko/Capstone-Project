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

import com.atmko.onmywatch.models.SeriesLog;

import java.util.List;

@Dao
public interface SeriesLogsDao {
    @Insert()
    void addMediaLog(SeriesLog mediaLog);

    @Query("SELECT * FROM series_logs")
    List<SeriesLog> getAllLogsAlt();

    @Query("SELECT * FROM series_logs "
            + "WHERE parent_id = :parentId")
    List<SeriesLog> getAllLogsWithMediaIdAlt(String parentId);

    @Query("SELECT * FROM series_logs "
            + "WHERE condition = 1 "
            + "ORDER BY timestamp ASC "
            + "LIMIT 10")
    LiveData<List<SeriesLog>> getUpcoming();

    @Query("SELECT * FROM series_logs "
            + "WHERE condition = 2 "
            + "ORDER BY timestamp DESC "
            + "LIMIT 10")
    LiveData<List<SeriesLog>> getAired();

    @Query("SELECT * FROM series_logs "
            + "WHERE condition = 3 "
            + "LIMIT 10")
    LiveData<List<SeriesLog>> getUndated();

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateMediaLog(SeriesLog mediaLog);

    @Delete
    void deleteMediaLog(SeriesLog mediaLog);
}
