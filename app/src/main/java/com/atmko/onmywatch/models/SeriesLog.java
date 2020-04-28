package com.atmko.onmywatch.models;

import androidx.room.Entity;
import androidx.room.Ignore;

import org.parceler.Parcel;

import java.util.Map;

@Parcel
@Entity(tableName = "series_logs")
public class SeriesLog extends MediaLog {
    public static final String TYPE_SEASON = "Season";
    public static final String TYPE_EPISODE = "Episode";

    public static final String TYPE_KEY = "type";
    public static final String SEASON_NUMBER_KEY = "season_number";
    public static final String EPISODE_NUMBER_KEY = "episode";
    public static final String IS_BUNDLED_KEY = "is_bundled";

    private static final String SEASON_SHORTHAND = "S";
    private static final String EPISODE_SHORTHAND = "E";

    public int seasonNumber;
    public int episodeNumber;
    public boolean isBundled;

    //constructor for parceler
    @Ignore
    public SeriesLog() {

    }

    @Ignore
    public SeriesLog(String type, int seasonNumber, int condition, long timestamp, String title,
                     String posterPath, String backdropPath, String parentId, boolean isBundled) {
        this.type = type;
        this.seasonNumber = seasonNumber;
        this.condition = condition;
        this.timestamp = timestamp;
        this.title = title;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.parentId = parentId;
        this.isBundled = isBundled;
    }

    public SeriesLog(String type, int seasonNumber, int episodeNumber, int condition, long timestamp,
                     String title, String posterPath, String backdropPath, String parentId,
                     boolean isBundled) {
        this.type = type;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.condition = condition;
        this.timestamp = timestamp;
        this.title = title;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.parentId = parentId;
        this.isBundled = isBundled;
    }

    public String getTypeString() {
        if (type.equals(TYPE_SEASON)) {
            return TYPE_SEASON + " " + seasonNumber;

        } else {
            return SEASON_SHORTHAND +
                    seasonNumber +
                    EPISODE_SHORTHAND +
                    episodeNumber;
        }
    }

    @Override
    public Map<String, Object> parseLogToDataMap() {
        Map<String, Object> seriesLogMap = super.parseLogToDataMap();
        seriesLogMap.put(TYPE_KEY, type);
        seriesLogMap.put(SEASON_NUMBER_KEY, seasonNumber);
        seriesLogMap.put(EPISODE_NUMBER_KEY, episodeNumber);
        seriesLogMap.put(IS_BUNDLED_KEY, isBundled);

        return seriesLogMap;
    }
}
