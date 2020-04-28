package com.atmko.onmywatch.models;

import androidx.room.Entity;
import androidx.room.Ignore;

import org.parceler.Parcel;

@Parcel
@Entity(tableName = "movie_logs")
public class MovieLog extends MediaLog {
    //constructor for parceler
    @Ignore
    public MovieLog() {
    }

    public MovieLog(int condition, long timestamp, String title, String posterPath,
                     String backdropPath, String parentId) {
        this.condition = condition;
        this.timestamp = timestamp;
        this.title = title;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.parentId = parentId;
    }
}
