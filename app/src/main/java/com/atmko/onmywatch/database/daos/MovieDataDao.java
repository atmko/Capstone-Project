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

import com.atmko.onmywatch.models.MovieData;

import java.util.List;

@Dao
public interface MovieDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addMovieData(MovieData movieData);

    //alternate method without live data
    @Query("SELECT * FROM movies")
    List<MovieData> getAllMoviesAlt();

    @Query("SELECT * FROM movies WHERE id = :movieId")
    LiveData<MovieData> getMovieById(String movieId);

    //alternate method without live data
    @Query("SELECT * FROM movies WHERE id = :movieId")
    MovieData getMovieByIdAlt(String movieId);

    @Query("SELECT watch_status FROM movies WHERE id = :movieId")
    LiveData<Integer> getMoviesWatchStatus(String movieId);

    @Query("SELECT COUNT(*) FROM movies WHERE watch_status = :watchStatus")
    LiveData<Integer> getWatchStatusCount(int watchStatus);

    @Query("SELECT * FROM movies WHERE watch_status = :watchStatus")
    LiveData<List<MovieData>> getMoviesByWatchStatus(int watchStatus);

    //alternate method without live data
    @Query("SELECT * FROM movies WHERE watch_status = :watchStatus")
    List<MovieData> getMoviesByWatchStatusAlt(int watchStatus);

    @Query("SELECT * FROM movies WHERE watch_status = :watchStatus AND title LIKE :mediaTitle")
    LiveData<List<MovieData>> getMoviesByWatchStatusLike(int watchStatus, String mediaTitle);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateMovieData(MovieData movieData);

    @Delete
    void deleteMovieData(MovieData movieData);
}
