/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import java.util.HashMap;
import java.util.Map;

import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.ID_KEY;

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

    public Map<String, Object> parseListModelToDataMap() {
        Map<String, Object> seriesDataRecordMap = new HashMap<>();
        seriesDataRecordMap.put(LIST_NAME_KEY, getListName());
        seriesDataRecordMap.put(ID_KEY, getId());

        return seriesDataRecordMap;
    }
}