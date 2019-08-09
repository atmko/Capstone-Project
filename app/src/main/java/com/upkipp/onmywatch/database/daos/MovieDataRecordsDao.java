package com.upkipp.onmywatch.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.upkipp.onmywatch.models.MovieData;
import com.upkipp.onmywatch.models.MovieDataRecord;

import java.util.List;

@Dao
public interface MovieDataRecordsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addRecord(MovieDataRecord movieDataRecord);

    @Query("SELECT * FROM movie_data_records")
    LiveData<List<MovieDataRecord>> getAllRecords();
//
    @Query("SELECT * FROM movies INNER JOIN movie_data_records ON movies.id = movie_data_records.movie_id WHERE movie_data_records.list_id = :listId")
    LiveData<List<MovieData>> getAllMoviesInList(String listId);

    @Query("SELECT list_id FROM movie_data_records WHERE movie_id = :movieId")
    LiveData<List<String>> getAllListNamesContainingMedia(String movieId);

    @Query("SELECT COUNT(*) FROM movie_data_records WHERE list_id = :listId")
    LiveData<Integer> getListMemberCount(String listId);
//
//    @Query("SELECT * FROM lists WHERE id LIKE :name")
//    LiveData<List<ListModel>> getListsWithNameLike(String name);
//
//    @Update(onConflict = OnConflictStrategy.REPLACE)
//    void updateListConfiguration(ListModel listModel);
//
    @Delete
    void deleteRecord(MovieDataRecord movieDataRecord);
}
