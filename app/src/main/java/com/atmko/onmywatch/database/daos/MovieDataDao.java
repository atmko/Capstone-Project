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

    //alternate method without live data
    @Query("SELECT * FROM movies WHERE tags LIKE :tag")
    List<MovieData> getAllMediaWithTagAlt(String tag);

    @Query("SELECT * FROM movies WHERE watch_status = :watchStatus INTERSECT "
            + "SELECT * FROM movies " + "WHERE tags LIKE '%'||:tag1||'%' "
            + "AND tags LIKE '%'||:tag2||'%' " + "AND tags LIKE '%'||:tag3||'%' "
            + "AND tags LIKE '%'||:tag4||'%' " + "AND tags LIKE '%'||:tag5||'%' "
            + "AND tags LIKE '%'||:tag6||'%' " + "AND tags LIKE '%'||:tag7||'%'")
    LiveData<List<MovieData>> getAllMediaWithWatchStatusAndTags(int watchStatus, String tag1, String tag2,
                                                                String tag3, String tag4, String tag5,
                                                                String tag6, String tag7);

    @Query("SELECT * FROM movies WHERE id = :movieId")
    LiveData<MovieData> getMovieById(String movieId);

    //alternate method without live data
    @Query("SELECT * FROM movies WHERE id = :movieId")
    MovieData getMovieByIdAlt(String movieId);

    @Query("SELECT watch_status FROM movies WHERE id = :movieId")
    LiveData<Integer> getMoviesWatchStatus(String movieId);

    @Query("SELECT * FROM movies WHERE watch_status = :watchStatus")
    LiveData<List<MovieData>> getMoviesByWatchStatus(int watchStatus);

    //alternate method without live data
    @Query("SELECT * FROM movies WHERE watch_status = :watchStatus")
    List<MovieData> getMoviesByWatchStatusAlt(int watchStatus);

    @Query("SELECT * FROM movies WHERE (watch_status = 1 OR watch_status = 2) AND (scheduled_media > 0 AND release_status != 'Released') ORDER BY scheduled_media ASC LIMIT 10")
    LiveData<List<MovieData>> getUserUpcomingMovies();

    //alternate method without live data
    @Query("SELECT * FROM movies WHERE (watch_status = 1 OR watch_status = 2) AND (scheduled_media > 0 AND release_status != 'Released') ORDER BY scheduled_media ASC LIMIT 10")
    List<MovieData> getUserUpcomingMoviesAlt();

    @Query("SELECT * FROM movies WHERE watch_status IN (1, 2) AND release_status = 'Released' ORDER BY scheduled_media ASC LIMIT 10")
    LiveData<List<MovieData>> getReleasedMovies();

    @Query("SELECT * FROM movies WHERE watch_status IN (1, 2) AND (scheduled_media = 0 AND release_status != 'Released') LIMIT 10")
    LiveData<List<MovieData>> getUndatedMovies();

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateMovieData(MovieData movieData);

    @Delete
    void deleteMovieData(MovieData movieData);
}