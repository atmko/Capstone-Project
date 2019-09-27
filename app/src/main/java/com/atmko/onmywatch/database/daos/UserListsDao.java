/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update;

import com.atmko.onmywatch.models.UserListModel;

import java.util.List;

@Dao
public interface UserListsDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void addList(UserListModel userListModel);

    @Query("SELECT * FROM user_lists")
    LiveData<List<UserListModel>> getAllLists();

    @Query("SELECT * FROM user_lists WHERE id = :name")
    LiveData<UserListModel> getListByName(String name);

    @Query("SELECT * FROM user_lists WHERE id LIKE :name")
    LiveData<List<UserListModel>> getListsWithNameLike(String name);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateListConfiguration(UserListModel userListModel);

    @Delete
    void deleteList(UserListModel userListModel);
}
