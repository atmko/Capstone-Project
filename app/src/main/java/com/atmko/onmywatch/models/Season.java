package com.atmko.onmywatch.models;

import org.parceler.Parcel;

@Parcel
public class Season {
    public String mId;
    public String mName;
    public String mAirDate;
    public String mSeasonNumber;
    public String mPosterPath;
    public String mOverview;
    public String mEpisodeCount;

    //constructor for parceler
    public Season() {
    }

    public Season(String id, String name, String airDate, String seasonNumber,
                  String posterPath, String overview, String episodeCount) {
        this.mId = id;
        this.mName = name;
        this.mAirDate = airDate;
        this.mSeasonNumber = seasonNumber;
        this.mPosterPath = posterPath;
        this.mOverview = overview;
        this.mEpisodeCount = episodeCount;
    }
}