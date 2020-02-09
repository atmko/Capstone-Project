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
    @Insert()
    void addList(UserListModel userListModel);

    @Query("SELECT * FROM user_lists")
    LiveData<List<UserListModel>> getAllLists();

    //alternate method without live data
    @Query("SELECT * FROM user_lists WHERE id = :name")
    UserListModel getListByNameAlt(String name);

    //alternate method without live data
    @Query("SELECT * FROM user_lists")
    List<UserListModel> getAllListsAlt();

    @Query("SELECT * FROM user_lists "
            + "WHERE id LIKE '%'||:tag1||'%' "
            + "AND id LIKE '%'||:tag2||'%' " + "AND id LIKE '%'||:tag3||'%' "
            + "AND id LIKE '%'||:tag4||'%' " + "AND id LIKE '%'||:tag5||'%' "
            + "AND id LIKE '%'||:tag6||'%' " + "AND id LIKE '%'||:tag7||'%'")
    LiveData<List<UserListModel>> getListsWithNameLike(String tag1, String tag2,
                                                       String tag3, String tag4, String tag5,
                                                       String tag6, String tag7);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateListConfiguration(UserListModel userListModel);

    @Delete
    void deleteList(UserListModel userListModel);
}
