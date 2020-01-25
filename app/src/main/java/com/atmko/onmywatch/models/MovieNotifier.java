package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "movie_notifiers",
        primaryKeys = {"media_id", "condition"},
        foreignKeys =
                {@ForeignKey(entity = MovieData.class, parentColumns = "id", childColumns = "media_id",
                        onDelete = ForeignKey.CASCADE)}
)

public class MovieNotifier extends MediaNotifier {
    public MovieNotifier(@NonNull String id, @NonNull int condition) {
        this.mId = id;
        this.mCondition = condition;
    }

    public Map<String, Object> parseNotifierToDataMap() {
        Map<String, Object> movieDataRecordMap = new HashMap<>();
        movieDataRecordMap.put(NOTIFIER_ID_KEY, getMediaId());
        movieDataRecordMap.put(CONDITION_KEY, getCondition());

        return movieDataRecordMap;
    }
}