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

import com.atmko.onmywatch.models.SeriesData;

import java.util.List;

@Dao
public interface SeriesDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addSeriesData(SeriesData seriesData);

    //alternate method without live data
    @Query("SELECT * FROM series")
    List<SeriesData> getAllSeriesAlt();

    @Query("SELECT * FROM series WHERE id = :seriesId")
    LiveData<SeriesData> getSeriesById(String seriesId);

    //alternate method without live data
    @Query("SELECT * FROM series WHERE id = :seriesId")
    SeriesData getSeriesByIdAlt(String seriesId);

    @Query("SELECT watch_status FROM series WHERE id = :seriesId")
    LiveData<Integer> getSeriesWatchStatus(String seriesId);

    @Query("SELECT * FROM series WHERE watch_status = :watchStatus")
    LiveData<List<SeriesData>> getSeriesByWatchStatus(int watchStatus);

    //alternate method without live data
    @Query("SELECT * FROM series WHERE watch_status = :watchStatus")
    List<SeriesData> getSeriesByWatchStatusAlt(int watchStatus);

    @Query("SELECT * FROM series WHERE watch_status = :watchStatus AND title LIKE :mediaTitle")
    LiveData<List<SeriesData>> getSeriesByWatchStatusLike(int watchStatus, String mediaTitle);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateSeriesData(SeriesData seriesData);

    @Delete
    void deleteSeriesData(SeriesData seriesData);
}
