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

import com.atmko.onmywatch.models.MovieLog;

import java.util.List;

@Dao
public interface MovieLogsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addMediaLog(MovieLog mediaLog);

    @Query("SELECT * FROM movie_logs")
    List<MovieLog> getAllLogsAlt();

    @Query("SELECT * FROM Movie_logs "
            + "WHERE parent_id = :mediaId "
            + "AND condition = :condition")
    MovieLog getLog(String mediaId, int condition);

    @Query("SELECT * FROM movie_logs "
            + "WHERE parent_id = :parentId")
    List<MovieLog> getAllLogsWithMediaIdAlt(String parentId);

    //TODO: REMOVE HARDCODED CONSTANTS FROMM QUERIES

    @Query("SELECT * FROM movie_logs "
            + "WHERE condition = 1 "
            + "ORDER BY timestamp ASC "
            + "LIMIT 10")
    LiveData<List<MovieLog>> getUpcoming();

    @Query("SELECT * FROM movie_logs "
            + "WHERE condition = 1 "
            + "ORDER BY timestamp ASC "
            + "LIMIT 10")
    List<MovieLog> getUpcomingAlt();

    @Query("SELECT * FROM movie_logs "
            + "WHERE condition = 2 "
            + "ORDER BY timestamp DESC "
            + "LIMIT 10")
    LiveData<List<MovieLog>> getAired();

    @Query("SELECT * FROM movie_logs "
            + "WHERE condition = 3 "
            + "LIMIT 10")
    LiveData<List<MovieLog>> getUndated();

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateLog(MovieLog mediaLog);

    @Delete
    void deleteMediaLog(MovieLog mediaLog);
}
