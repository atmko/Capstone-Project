package com.upkipp.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "series_data_records", primaryKeys = {"series_id", "list_id"},
        foreignKeys =
                {@ForeignKey(entity = SeriesData.class, parentColumns = "id", childColumns = "series_id"),
                @ForeignKey(entity = UserListModel.class, parentColumns = "id",  childColumns = "list_id", onDelete = ForeignKey.CASCADE)}
                )

public class SeriesDataRecord {
    @NonNull
    @ColumnInfo(name = "series_id") String mId;
    @NonNull
    @ColumnInfo(name = "list_id") String mListName;

    public SeriesDataRecord(@NonNull String mId, @NonNull String mListName) {
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
