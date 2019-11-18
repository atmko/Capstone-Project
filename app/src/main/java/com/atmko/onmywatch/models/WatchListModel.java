/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.google.firebase.firestore.DocumentSnapshot;

import org.parceler.Parcel;

import java.util.Map;

@Parcel
@Entity(tableName = "watch_lists")
public class WatchListModel extends ListModel {
    //constructor for parceler
    @Ignore
    public WatchListModel() {

    }

    @Ignore
    public WatchListModel(@NonNull String name, int itemCount) {
        this.mName = name;
        this.mItemCount = itemCount;
    }

    public WatchListModel(@NonNull String name) {
        this.mName = name;
    }

    public Map<String, Object> parseListModelToDataMap() {
        return getFirebaseListModelMap(this);
    }

    //TODO: list name and list count are never null when retrieved from the database
    @SuppressWarnings("ConstantConditions")
    public static WatchListModel parseWatchListModel(DocumentSnapshot document) {
        String listName = document.getString(LIST_NAME_KEY);
        int listCount = ((Long) document.get(ITEM_COUNT_KEY)).intValue();

        WatchListModel watchListModel = new WatchListModel(listName, listCount);
        watchListModel.setDocumentId(document.getId());

        return watchListModel;
    }
}
