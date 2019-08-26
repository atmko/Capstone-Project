package com.atmko.onmywatch.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.atmko.onmywatch.models.SeriesNotifier;

@Dao
public interface SeriesNotifierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addSeriesNotifier(SeriesNotifier seriesNotifier);

    @Query("SELECT * FROM series_notifiers WHERE id = :seriesId")
    LiveData<SeriesNotifier> getNotifierById(String seriesId);

    //alternate method without live data
    @Query("SELECT * FROM series_notifiers WHERE id = :seriesId")
    SeriesNotifier getNotifierByIdAlt(String seriesId);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateSeriesNotifier(SeriesNotifier seriesNotifier);

    @Delete
    void deleteSeriesNotifier(SeriesNotifier seriesNotifier);
}
