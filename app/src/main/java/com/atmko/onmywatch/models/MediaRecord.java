/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.List;

@Entity(primaryKeys = {"media_id", "list_id"},
        foreignKeys =
                {@ForeignKey(entity = MovieData.class, parentColumns = "id", childColumns = "media_id"),
                        @ForeignKey(entity = UserListModel.class, parentColumns = "id",
                                childColumns = "list_id", onDelete = ForeignKey.CASCADE)}
)

abstract public class MediaRecord {
    @NonNull
    @ColumnInfo(name = "media_id") String mId;
    @NonNull
    @ColumnInfo(name = "list_id") String mListName;

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    public String getListName() {
        return mListName;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof MediaRecord) {
            boolean listNameMatches = ((MediaRecord) obj).getListName().equals(this.mListName);
            boolean mediaIdMatches = ((MediaRecord) obj).getId().equals(this.mId);
            return listNameMatches && mediaIdMatches;

        } else {
            return super.equals(obj);

        }
    }
}
