package com.atmko.onmywatch.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SeriesNotifier;

@Dao
public interface MovieNotifierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addMovieNotifier(MovieNotifier movieNotifier);

    @Query("SELECT * FROM movie_notifiers WHERE id = :movieId")
    LiveData<MovieNotifier> getNotifierById(String movieId);

    @Query("SELECT * FROM movie_notifiers WHERE id = :movieId")
    MovieNotifier getNotifierByIdAlt(String movieId);

    //alternate method without live data
    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateMovieNotifier(MovieNotifier movieNotifier);

    @Delete
    void deleteMovieNotifier(MovieNotifier movieNotifier);
}
