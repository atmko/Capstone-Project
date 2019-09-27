/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

import org.parceler.Parcel;

@Parcel
@Entity(tableName = "watch_lists")
public class WatchListModel extends ListModel {
    //constructor for parceler
    @Ignore
    public WatchListModel() {

    }

    @Ignore
    public WatchListModel(@NonNull String name, int itemCount) {
        this.name = name;
        this.mItemCount = itemCount;
    }

    public WatchListModel(@NonNull String name) {
        this.name = name;
    }
}
