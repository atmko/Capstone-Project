package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(tableName = "media_logs", primaryKeys = {"parent_id", "type", "condition"})
public abstract class MediaLog {
    public static final int CONDITION_UPCOMING = 1;
    public static final int CONDITION_AIRED = 2;
    public static final int CONDITION_UNDATED = 3;

    public static final String TYPE_KEY = "type";
    public static final String CONDITION_KEY = "condition";
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String TITLE_KEY = "title";
    public static final String POSTER_PATH_KEY = "poster_path";
    public static final String PARENT_ID_KEY = "parent_id";

    @NonNull public String type;
    public int condition;
    public long timestamp;
    public String title;
    public String posterPath;
    @NonNull @ColumnInfo(name = "parent_id") public String parentId;

    @Ignore
    private String mUniqueExternalId;

    public String getUniqueExternalId() {
        return mUniqueExternalId;
    }

    public void setUniqueExternalId(String mDocumentId) {
        this.mUniqueExternalId = mDocumentId;
    }
}
