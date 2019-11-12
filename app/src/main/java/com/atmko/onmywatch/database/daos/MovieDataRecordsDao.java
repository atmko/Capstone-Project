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

import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.UserListModel;

import java.util.List;

@Dao
public interface MovieDataRecordsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addRecord(MovieDataRecord movieDataRecord);

    @Query("SELECT * FROM movie_data_records")
    LiveData<List<MovieDataRecord>> getAllRecords();

    @Query("SELECT * FROM movies INNER JOIN movie_data_records ON movies.id = movie_data_records.media_id "
            +"WHERE movie_data_records.list_id = :listId")
    LiveData<List<MovieData>> getAllMoviesInList(String listId);

    //alternate method without live data
    @Query("SELECT * FROM movies INNER JOIN movie_data_records ON movies.id = movie_data_records.media_id "
            +"WHERE movie_data_records.list_id = :listId")
    List<MovieData> getAllMoviesInListAlt(String listId);

    @Query("SELECT * FROM movies INNER JOIN movie_data_records ON movies.id = movie_data_records.media_id "
            +"WHERE movie_data_records.list_id = :listId AND movies.title LIKE :mediaTitle")
    LiveData<List<MovieData>> getMoviesWithNameLike(String listId, String mediaTitle);

    //alternate method without live data
    @Query("SELECT * FROM movie_data_records WHERE list_id = :listId")
    List<MovieDataRecord> getAllRecordsOfListAlt(String listId);

    @Query("SELECT list_id FROM movie_data_records WHERE media_id = :movieId")
    LiveData<List<String>> getAllListNamesContainingMedia(String movieId);

    @Query("SELECT * FROM user_lists INNER JOIN movie_data_records ON user_lists.id = movie_data_records.list_id "
            +"WHERE media_id = :movieId")
    LiveData<List<UserListModel>> getAllListsContainingMedia(String movieId);

    //alternate method without live data
    @Query("SELECT * FROM user_lists INNER JOIN movie_data_records ON user_lists.id = movie_data_records.list_id "
            +"WHERE media_id = :movieId")
    List<UserListModel> getAllListsContainingMediaAlt(String movieId);

    @Query("SELECT COUNT(*) FROM movie_data_records WHERE list_id = :listId")
    LiveData<Integer> getListMemberCount(String listId);

    @Delete
    void deleteRecord(MovieDataRecord movieDataRecord);
}