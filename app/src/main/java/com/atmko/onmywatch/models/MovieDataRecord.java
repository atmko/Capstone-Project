/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "movie_data_records", primaryKeys = {"movie_id", "list_id"},
        foreignKeys =
                {@ForeignKey(entity = MovieData.class, parentColumns = "id", childColumns = "movie_id"),
                @ForeignKey(entity = UserListModel.class, parentColumns = "id",  childColumns = "list_id", onDelete = ForeignKey.CASCADE)}
                )

public class MovieDataRecord {
    @NonNull
    @ColumnInfo(name = "movie_id") String mId;
    @NonNull
    @ColumnInfo(name = "list_id") String mListName;

    public MovieDataRecord(@NonNull String mId, @NonNull String mListName) {
        this.mId = mId;
        this.mListName = mListName;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    public void setId(@NonNull String id) {
        this.mId = id;
    }

    @NonNull
    public String getListName() {
        return mListName;
    }

    public void setListName(@NonNull String listName) {
        this.mListName = listName;
    }
}
