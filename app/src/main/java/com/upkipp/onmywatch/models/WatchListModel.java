package com.upkipp.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.parceler.Parcel;

import java.util.List;

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
//        this.mMediaDataList = new ArrayList<>();
    }

    public WatchListModel(@NonNull String name) {
        this.name = name;
    }
}
