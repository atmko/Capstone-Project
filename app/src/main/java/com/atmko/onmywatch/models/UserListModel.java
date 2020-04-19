/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Parcel
@Entity(tableName = "user_lists")
public class UserListModel extends ListModel {
    //constructor for parceler
    @Ignore
    public UserListModel() {

    }

    @Ignore
    public UserListModel(@NonNull String name, int itemCount) {
        this.mName = name;
        this.mItemCount = itemCount;
    }

    public UserListModel(@NonNull String name) {
        this.mName = name;
    }

    public Map<String, Object> parseListModelToDataMap() {
        return getFirebaseListModelMap(this);
    }

}
