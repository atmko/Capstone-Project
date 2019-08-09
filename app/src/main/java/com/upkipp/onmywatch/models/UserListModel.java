package com.upkipp.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

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
        this.name = name;
        this.mItemCount = itemCount;
//        this.mMediaDataList = new ArrayList<>();
    }

    public UserListModel(@NonNull String name) {
        this.name = name;
    }
}
