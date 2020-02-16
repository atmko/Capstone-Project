package com.atmko.onmywatch.models;

import androidx.room.Entity;
import androidx.room.Ignore;

import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "series_logs")
public class SeriesLog extends MediaLog {
    public static final String TYPE_SEASON = "Season";
    public static final String TYPE_EPISODE = "Episode";

    public static final String TYPE_KEY = "type";
    public static final String SEASON_NUMBER_KEY = "season_number";
    public static final String EPISODE_NUMBER_KEY = "season";
    public static final String CONDITION_KEY = "condition";
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String TITLE_KEY = "title";
    public static final String POSTER_PATH_KEY = "poster_path";
    public static final String PARENT_ID_KEY = "parent_id";
    public static final String IS_BUNDLED_KEY = "is_bundled";

    public int seasonNumber;
    public int episodeNumber;
    public boolean isBundled;

    @Ignore
    public SeriesLog(String type, int seasonNumber, int condition, long timestamp, String title,
                     String posterPath, String parentId, boolean isBundled) {
        this.type = type;
        this.seasonNumber = seasonNumber;
        this.condition = condition;
        this.timestamp = timestamp;
        this.title = title;
        this.posterPath = posterPath;
        this.parentId = parentId;
        this.isBundled = isBundled;
    }

    public SeriesLog(String type, int seasonNumber, int episodeNumber, int condition, long timestamp,
                     String title, String posterPath, String parentId, boolean isBundled) {
        this.type = type;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.condition = condition;
        this.timestamp = timestamp;
        this.title = title;
        this.posterPath = posterPath;
        this.parentId = parentId;
        this.isBundled = isBundled;
    }

    public Map<String, Object> parseLogToDataMap() {
        Map<String, Object> seriesLogMap = new HashMap<>();
        seriesLogMap.put(TYPE_KEY, type);
        seriesLogMap.put(SEASON_NUMBER_KEY, seasonNumber);
        seriesLogMap.put(EPISODE_NUMBER_KEY, episodeNumber);
        seriesLogMap.put(CONDITION_KEY, condition);
        seriesLogMap.put(TIMESTAMP_KEY, timestamp);
        seriesLogMap.put(TITLE_KEY, title);
        seriesLogMap.put(POSTER_PATH_KEY, posterPath);
        seriesLogMap.put(PARENT_ID_KEY, parentId);
        seriesLogMap.put(IS_BUNDLED_KEY, isBundled);

        return seriesLogMap;
    }
}
