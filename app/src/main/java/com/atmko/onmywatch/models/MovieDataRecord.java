/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.ID_KEY;

@Entity(tableName = "movie_data_records",
        foreignKeys =
                {@ForeignKey(entity = MovieData.class, parentColumns = "id", childColumns = "media_id"),
                        @ForeignKey(entity = UserListModel.class, parentColumns = "id",
                                childColumns = "list_id", onDelete = ForeignKey.CASCADE)}
)

public class MovieDataRecord extends MediaRecord {

    public MovieDataRecord(@NonNull String mId, @NonNull String mListName) {
        this.mId = mId;
        this.mListName = mListName;
    }

    @SuppressWarnings("ConstantConditions")
    public static MovieDataRecord parseMediaRecord(DocumentSnapshot document) {
        MovieDataRecord mediaRecord = new MovieDataRecord(
                (String) document.get(ID_KEY),
                (String) document.get(LIST_NAME_KEY)
        );

        mediaRecord.setDocumentId(document.getId());
        return mediaRecord;
    }

    public Map<String, Object> parseListModelToDataMap() {
        Map<String, Object> movieDataRecordMap = new HashMap<>();
        movieDataRecordMap.put(LIST_NAME_KEY, getListName());
        movieDataRecordMap.put(ID_KEY, getId());

        return movieDataRecordMap;
    }
}
