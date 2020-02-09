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

import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.UserListModel;

import java.util.List;

@Dao
public interface SeriesDataRecordsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addRecord(SeriesDataRecord seriesDataRecord);

    //alternate method without live data
    @Query("SELECT * FROM series_data_records WHERE media_id = :mediaId AND list_id = :listName")
    SeriesDataRecord getRecordByIdAlt(String mediaId, String listName);

    //alternate method without live data
    @Query("SELECT * FROM series_data_records")
    List<SeriesDataRecord> getAllRecordsAlt();

    @Query("SELECT * FROM series INNER JOIN series_data_records ON series.id = media_id "
            +"WHERE series_data_records.list_id = :listId")
    LiveData<List<SeriesData>> getAllSeriesInList(String listId);

    //alternate method without live data
    @Query("SELECT * FROM series INNER JOIN series_data_records ON series.id = media_id "
            +"WHERE series_data_records.list_id = :listId")
    List<SeriesData> getAllSeriesInListAlt(String listId);

    @Query("SELECT * FROM series INNER JOIN series_data_records ON series.id = media_id "
            +"WHERE series_data_records.list_id = :listId "
            + "AND series.tags LIKE :tag1 " + "AND series.tags LIKE :tag2 "
            + "AND series.tags LIKE :tag3 " + "AND series.tags LIKE :tag4 "
            + "AND series.tags LIKE :tag5 " + "AND series.tags LIKE :tag6 "
            + "AND series.tags LIKE :tag7")
    LiveData<List<SeriesData>> getMediaInListLike(String listId, String tag1, String tag2,
                                                  String tag3, String tag4, String tag5,
                                                  String tag6, String tag7);

    //alternate method without live data
    @Query("SELECT * FROM series_data_records WHERE list_id = :listId")
    List<SeriesDataRecord> getAllRecordsOfListAlt(String listId);

    @Query("SELECT list_id FROM series_data_records WHERE media_id = :seriesId")
    LiveData<List<String>> getAllListNamesContainingMedia(String seriesId);

    @Query("SELECT * FROM user_lists INNER JOIN series_data_records ON user_lists.id = series_data_records.list_id "
            +"WHERE media_id = :seriesId")
    LiveData<List<UserListModel>> getAllListsContainingMedia(String seriesId);

    //alternate method without live data
    @Query("SELECT * FROM user_lists INNER JOIN series_data_records ON user_lists.id = series_data_records.list_id "
            +"WHERE media_id = :seriesId")
    List<UserListModel> getAllListsContainingMediaAlt(String seriesId);

    @Delete
    void deleteRecord(SeriesDataRecord seriesDataRecord);
}