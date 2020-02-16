package com.atmko.onmywatch.models;

import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(tableName = "series_logs")
public class SeriesLog extends MediaLog {
    public static final String TYPE_SEASON = "Season";
    public static final String TYPE_EPISODE = "Episode";

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
}
