package com.atmko.onmywatch.models;

import java.util.ArrayList;
import java.util.List;

public class MovieLog extends MediaLog {
    private MovieLog(int condition, long timestamp, String title, String posterPath, String parentId) {
        this.condition = condition;
        this.timestamp = timestamp;
        this.title = title;
        this.posterPath = posterPath;
        this.parentId = parentId;
    }

    public static ArrayList<MovieLog> convertMediaToLogs(List<MovieData> movieDataList) {
        ArrayList<MovieLog> movieLogs = new ArrayList<>();
        for (MovieData movieData: movieDataList) {
            int condition;
            long releaseTimestamp;
            ScheduledMedia scheduledMedia = movieData.getScheduledMedia();
            if (scheduledMedia == null) {
                condition = CONDITION_UNDATED;
                releaseTimestamp = Long.MAX_VALUE;

            } else {
                long timeDifference = scheduledMedia.getBestTimeDifference();
                condition = timeDifference >= 0 ? CONDITION_UPCOMING : CONDITION_AIRED;
                releaseTimestamp = scheduledMedia.getBestLocalAirDate().getTime();
            }

            MovieLog movieLog = new MovieLog(condition, releaseTimestamp, movieData.getTitle(),
                    movieData.mPosterPath, movieData.getId());

            movieLogs.add(movieLog);
        }

        return movieLogs;
    }
}
