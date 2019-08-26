/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.atmko.onmywatch.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

abstract public class MediaData {
    public static final int WATCH_STATUS_NONE = 0;
    public static final int WATCH_STATUS_TO_WATCH = 1;
    public static final int WATCH_STATUS_WATCHING = 2;
    public static final int WATCH_STATUS_WATCHED = 3;
    public static final int WATCH_STATUS_DROPPED = 4;

    //primary attributes
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id") String mId;
    @Ignore String mVoteCount;
    @ColumnInfo(name = "vote_average") String mVoteAverage;
    @ColumnInfo(name = "title") String mTitle;
    @Ignore double mPopularity;
    @ColumnInfo(name = "poster_path") String mPosterPath;
    @Ignore String mSpotlightPosterPath;
    public @ColumnInfo(name = "original_language") String mOriginalLanguage;
    public @ColumnInfo(name = "original_title") String mOriginalTitle;
    public @ColumnInfo(name = "genres") ArrayList<String> mGenres;
    @ColumnInfo(name = "backdrop_path") String mBackdropPath;
    @ColumnInfo(name = "overview") String mOverview;
    @ColumnInfo(name = "release_date") String mReleaseDate;

    //secondary attributes(details)
    @Ignore ArrayList<CastData> mCast;
    @ColumnInfo(name = "release_status") String mReleaseStatus;

    @ColumnInfo(name = "watch_status") int mWatchStatus;

    public String getId() {
        return mId;
    }

    public String getVoteCount() {
        return mVoteCount;
    }

    public String getVoteAverage() {
        return mVoteAverage;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getFormattedTitle() {
        if (mTitle.contains(": ")) {
            String[] titleStringArray = mTitle.split(": ", 2);

            return titleStringArray[0] + ":" + "\n" + titleStringArray[1];
        }

        return mTitle;
    }

    public double getPopularity() {
        return mPopularity;
    }

    public String getPosterPath() {
        return mPosterPath;
    }

    public String getSpotlightPosterPath() {
        return mSpotlightPosterPath;
    }

    public void setSpotlightPosterPath(String spotlightPosterPath) {
        this.mSpotlightPosterPath = spotlightPosterPath;
    }

    public String getOriginalLanguage() {
        return mOriginalLanguage;
    }

    public String getOriginalTitle() {
        return mOriginalTitle;
    }

    public ArrayList<String> getGenres() {
        return mGenres;
    }

    public String getBackdropPath() {
        return mBackdropPath;
    }

    public String getOverview() {
        return mOverview;
    }

    public String getReleaseDate() {
        return mReleaseDate;
    }

    public String getReleaseStatus() {
        return mReleaseStatus;
    }

    public void setReleaseStatus(String releaseStatus) {
        this.mReleaseStatus = releaseStatus;
    }

    public ArrayList<CastData> getCast() {
        return mCast;
    }

    public void setCast(ArrayList<CastData> cast) {
        this.mCast = cast;
    }

    public int getWatchStatus() {
        return mWatchStatus;
    }

    public void setWatchStatus(int watchStatus) {
        this.mWatchStatus = watchStatus;
    }

    public static String getWatchStatusTitle(int watchStatus, Context context) {
        String[] watchStatusTitles =
                context.getResources().getStringArray(R.array.watch_status_series_titles);

        return watchStatusTitles[watchStatus];
    }

    public static String getMediaTypeTitle(int mediaType, Context context) {
        String[] mediaTypeTitles =
                context.getResources().getStringArray(R.array.media_types);

        return mediaTypeTitles[mediaType];
    }
}
