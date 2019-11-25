package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "series_notifiers",
        primaryKeys = {"media_id", "condition"},
        foreignKeys =
                {@ForeignKey(entity = SeriesData.class, parentColumns = "id", childColumns = "media_id",
                        onDelete = ForeignKey.CASCADE)}
)

public class SeriesNotifier extends MediaNotifier {
    public SeriesNotifier(@NonNull String id, @NonNull int condition) {
        this.mId = id;
        this.mCondition = condition;
    }
}