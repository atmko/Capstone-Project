package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "movie_notifiers",
        primaryKeys = {"media_id", "condition"},
        foreignKeys =
                {@ForeignKey(entity = MovieData.class, parentColumns = "id", childColumns = "media_id",
                        onDelete = ForeignKey.CASCADE)}
)

public class MovieNotifier extends MediaNotifier {
    public MovieNotifier(@NonNull String id, @NonNull int condition, String alarmDate) {
        this.mId = id;
        this.mCondition = condition;
        this.alarmDate = alarmDate;
    }
}