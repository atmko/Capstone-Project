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

    @Query("SELECT * FROM series WHERE watch_status = 2 AND next_episode > 0 ORDER BY next_episode ASC LIMIT 10")
    LiveData<List<SeriesData>> getUserUpcomingEpisodes();

    @Query("SELECT * FROM series WHERE watch_status IN (1, 2) AND (next_episode = 0 AND release_status NOT IN ('Canceled', 'Ended', 'Running')) LIMIT 10")
    LiveData<List<SeriesData>> getUndatedSeries();

    @Query("SELECT * FROM series WHERE watch_status IN (1, 2) AND release_status IN ('Canceled', 'Ended') ORDER BY next_episode ASC LIMIT 10")
    LiveData<List<SeriesData>> getEndedSeries();

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateSeriesData(SeriesData seriesData);

    @Delete
    void deleteSeriesData(SeriesData seriesData);
}
