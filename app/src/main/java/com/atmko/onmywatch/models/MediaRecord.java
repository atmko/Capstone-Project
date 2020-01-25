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

    @Ignore private String mUniqueExternalId;

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

    public String getUniqueExternalId() {
        return mUniqueExternalId;
    }

    public void setUniqueExternalId(String mDocumentId) {
        this.mUniqueExternalId = mDocumentId;
    }

    public static List<String> extractMediaNames(List<MediaRecord> mediaRecords) {
        List<String> extractedNames = new ArrayList<>();

        for (MediaRecord mediaRecord: mediaRecords) {
            extractedNames.add(mediaRecord.getId());
        }

        return extractedNames;
    }

    public static List<String> extractListNames(List<MediaRecord> mediaRecords) {
        List<String> extractedNames = new ArrayList<>();

        for (MediaRecord mediaRecord: mediaRecords) {
            extractedNames.add(mediaRecord.getListName());
        }

        return extractedNames;
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
