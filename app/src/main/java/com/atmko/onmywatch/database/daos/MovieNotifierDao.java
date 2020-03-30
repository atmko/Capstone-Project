package com.atmko.onmywatch.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.atmko.onmywatch.models.MovieNotifier;

import java.util.List;

@Dao
public interface MovieNotifierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addMediaNotifier(MovieNotifier movieNotifier);

    //alternate method without live data
    @Query("SELECT * FROM movie_notifiers")
    List<MovieNotifier> getAllNotifiersAlt();

    //alternate method without live data
    @Query("SELECT * FROM movie_notifiers WHERE is_active == 1")
    List<MovieNotifier> getActiveNotifiersAlt();

    //alternate method without live data
    @Query("SELECT media_id FROM movie_notifiers")
    List<String> getAllMediaIdsAlt();

    //alternate method without live data
    @Query("SELECT * FROM movie_notifiers WHERE media_id = :mediaId AND condition = :condition")
    MovieNotifier getNotifierByIdAlt(String mediaId, int condition);

    @Query("SELECT * FROM movie_notifiers WHERE media_id = :mediaId")
    LiveData<List<MovieNotifier>> getNotifiersWithMediaId(String mediaId);

    //alternate method without live data
    @Query("SELECT * FROM movie_notifiers WHERE media_id = :mediaId")
    List<MovieNotifier> getNotifiersWithMediaIdAlt(String mediaId);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateNotifier(MovieNotifier movieNotifier);

    @Delete
    void deleteNotifier(MovieNotifier movieNotifier);
}
