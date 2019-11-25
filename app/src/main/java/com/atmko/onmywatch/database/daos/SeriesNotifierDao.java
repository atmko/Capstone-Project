package com.atmko.onmywatch.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.atmko.onmywatch.models.SeriesNotifier;

import java.util.List;

@Dao
public interface SeriesNotifierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addMediaNotifier(SeriesNotifier seriesNotifier);

    //alternate method without live data
    @Query("SELECT * FROM series_notifiers")
    List<SeriesNotifier> getAllNotifiersAlt();

    //alternate method without live data
    @Query("SELECT * FROM series_notifiers WHERE media_id = :mediaId AND condition = :condition")
    SeriesNotifier getNotifierByIdAlt(String mediaId, int condition);

    @Query("SELECT * FROM series_notifiers WHERE media_id = :mediaId")
    LiveData<List<SeriesNotifier>> getNotifiersWithMediaId(String mediaId);

    //alternate method without live data
    @Query("SELECT * FROM series_notifiers WHERE media_id = :mediaId")
    List<SeriesNotifier> getNotifiersWithMediaIdAlt(String mediaId);

    @Delete
    void deleteNotifier(SeriesNotifier seriesNotifier);
}
