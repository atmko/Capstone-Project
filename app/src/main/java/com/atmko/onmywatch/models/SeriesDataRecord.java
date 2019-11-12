/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;
import static com.atmko.onmywatch.utils.network_utils.ApiConstants.ID_KEY;

@Entity(tableName = "series_data_records",
        foreignKeys =
                {@ForeignKey(entity = SeriesData.class, parentColumns = "id", childColumns = "media_id"),
                        @ForeignKey(entity = UserListModel.class, parentColumns = "id",
                                childColumns = "list_id", onDelete = ForeignKey.CASCADE)}
)

public class SeriesDataRecord extends MediaRecord {

    public SeriesDataRecord(@NonNull String mId, @NonNull String mListName) {
        this.mId = mId;
        this.mListName = mListName;
    }
}
