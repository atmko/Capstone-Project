package com.upkipp.onmywatch.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.upkipp.onmywatch.models.MovieData;
import com.upkipp.onmywatch.models.SeriesData;

import java.util.List;

@Dao
public interface MovieDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addMovieData(MovieData movieData);

    @Query("SELECT * FROM movies WHERE id = :movieId")
    LiveData<MovieData> getMovieById(String movieId);

    @Query("SELECT watch_status FROM movies WHERE id = :movieId")
    LiveData<Integer> getMoviesWatchStatus(String movieId);

    @Query("SELECT COUNT(*) FROM movies WHERE watch_status = :watchStatus")
    LiveData<Integer> getWatchStatusCount(int watchStatus);

    @Query("SELECT * FROM movies WHERE watch_status = :watchStatus")
    LiveData<List<MovieData>> getMoviesByWatchStatus(int watchStatus);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateMovieData(MovieData movieData);

    @Delete
    void deleteMovieData(MovieData movieData);
}
