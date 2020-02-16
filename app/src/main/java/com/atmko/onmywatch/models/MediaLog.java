package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(tableName = "media_logs", primaryKeys = {"parent_id", "type", "condition"})
public class MediaLog {
    public static final String TYPE_SEASON = "Season";
    public static final String TYPE_EPISODE = "Episode";

    public static final int CONDITION_UPCOMING = 1;
    public static final int CONDITION_AIRED = 2;
    public static final int CONDITION_UNDATED = 3;
    
   @NonNull public String type;
    public int seasonNumber;
    public int episodeNumber;
    public int condition;
    public long timestamp;
    public String title;
    public String posterPath;
    @NonNull
    @ColumnInfo(name = "parent_id") public String parentId;
    public boolean isBundled;

    @Ignore
    public MediaLog(String type, int seasonNumber, int condition, long timestamp, String title,
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

    public MediaLog(String type, int seasonNumber, int episodeNumber, int condition, long timestamp,
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
