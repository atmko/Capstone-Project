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
    @Ignore private String documentId;

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

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
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
