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

import com.atmko.onmywatch.models.WatchListModel;

import java.util.List;

@Dao
public interface WatchListsDao {
    @Insert()
    void addList(WatchListModel watchListModel);

    @Query("SELECT * FROM watch_lists")
    LiveData<List<WatchListModel>> getAllLists();

    //alternate method without live data
    @Query("SELECT * FROM watch_lists")
    List<WatchListModel> getAllListsAlt();

    //alternate method without live data
    @Query("SELECT * FROM watch_lists WHERE id = :name")
    WatchListModel getListByNameAlt(String name);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateListConfiguration(WatchListModel watchListModel);

    @Delete
    void deleteList(WatchListModel watchListModel);
}
