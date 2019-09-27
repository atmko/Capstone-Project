/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;

import org.parceler.Parcel;

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

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof UserListModel) {
            return ((UserListModel) obj).getName().equals(this.mName);

        } else {
            return super.equals(obj);

        }
    }
}
