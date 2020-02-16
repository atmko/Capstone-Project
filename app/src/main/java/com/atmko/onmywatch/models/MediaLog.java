package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "media_logs", primaryKeys = {"parent_id", "type", "condition"})
public abstract class MediaLog {
    public static final int CONDITION_UPCOMING = 1;
    public static final int CONDITION_AIRED = 2;
    public static final int CONDITION_UNDATED = 3;

   @NonNull public String type;
    public int condition;
    public long timestamp;
    public String title;
    public String posterPath;
    @NonNull
    @ColumnInfo(name = "parent_id") public String parentId;
}
