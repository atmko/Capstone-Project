/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.google.firebase.firestore.DocumentSnapshot;

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

    public static List<String> getContainingListsNames(List<UserListModel> newContainingLists) {
        List<String> newContainingListNames = new ArrayList<>();

        for (UserListModel userListModel: newContainingLists) {
            newContainingListNames.add(userListModel.getName());

        }

        return newContainingListNames;
    }

    public Map<String, Object> parseListModelToDataMap() {
        return getFirebaseListModelMap(this);
    }

    //TODO: list name and list count are never null when retrieved from the database
    @SuppressWarnings("ConstantConditions")
    public static UserListModel parseUserListModel(DocumentSnapshot document) {
        String listName = document.getString(LIST_NAME_KEY);
        int listCount = ((Long) document.get(ITEM_COUNT_KEY)).intValue();

        UserListModel userListModel = new UserListModel(listName, listCount);
        userListModel.setDocumentId(document.getId());

        return userListModel;
    }

    public static UserListModel parseUserListModel(String listName) {
        return new UserListModel(listName);
    }
}
