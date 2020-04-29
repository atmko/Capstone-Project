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

    @Query("SELECT * FROM series WHERE last_updated <= :updateThreshold")
    List<SeriesData> getMediaAtOrBeforeThreshold(long updateThreshold);

    //alternate method without live data
    @Query("SELECT * FROM series WHERE tags LIKE :tag")
    List<SeriesData> getAllMediaWithTagAlt(String tag);

    @Query("SELECT * FROM series WHERE watch_status = :watchStatus INTERSECT "
            + "SELECT * FROM series " + "WHERE tags LIKE '%'||:tag1||'%' "
            + "AND tags LIKE '%'||:tag2||'%' " + "AND tags LIKE '%'||:tag3||'%' "
            + "AND tags LIKE '%'||:tag4||'%' " + "AND tags LIKE '%'||:tag5||'%' "
            + "AND tags LIKE '%'||:tag6||'%' " + "AND tags LIKE '%'||:tag7||'%'")
    LiveData<List<SeriesData>> getAllMediaWithWatchStatusAndTags(int watchStatus, String tag1, String tag2,
                                                                 String tag3, String tag4, String tag5,
                                                                 String tag6, String tag7);

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

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateSeriesData(SeriesData seriesData);

    @Delete
    void deleteSeriesData(SeriesData seriesData);
}
