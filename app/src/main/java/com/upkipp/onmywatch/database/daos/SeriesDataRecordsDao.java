package com.upkipp.onmywatch.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.upkipp.onmywatch.models.SeriesData;
import com.upkipp.onmywatch.models.SeriesDataRecord;

import java.util.List;

@Dao
public interface SeriesDataRecordsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addRecord(SeriesDataRecord seriesDataRecord);

    @Query("SELECT * FROM series_data_records")
    LiveData<List<SeriesDataRecord>> getAllRecords();
//
    @Query("SELECT * FROM series INNER JOIN series_data_records ON series.id = series_data_records.series_id WHERE series_data_records.list_id = :listId")
    LiveData<List<SeriesData>> getAllSeriesInList(String listId);

    @Query("SELECT list_id FROM series_data_records WHERE series_id = :seriesId")
    LiveData<List<String>> getAllListNamesContainingMedia(String seriesId);

    @Query("SELECT COUNT(*) FROM series_data_records WHERE list_id = :listId")
    LiveData<Integer> getListMemberCount(String listId);
//
//    @Query("SELECT * FROM lists WHERE id LIKE :name")
//    LiveData<List<ListModel>> getListsWithNameLike(String name);
//
//    @Update(onConflict = OnConflictStrategy.REPLACE)
//    void updateListConfiguration(ListModel listModel);
//
    @Delete
    void deleteRecord(SeriesDataRecord seriesDataRecord);
}
