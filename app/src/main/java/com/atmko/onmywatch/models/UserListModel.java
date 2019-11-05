/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

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

    public static List<String> getContainingListsNames(List<UserListModel> newContainingLists) {
        List<String> newContainingListNames = new ArrayList<>();

        for (UserListModel userListModel: newContainingLists) {
            newContainingListNames.add(userListModel.getName());

        }

        return newContainingListNames;
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
