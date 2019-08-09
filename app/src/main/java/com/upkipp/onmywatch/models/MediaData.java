/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.upkipp.onmywatch.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.utils.MovieDataParser;

import java.util.ArrayList;
import java.util.List;

abstract public class MediaData {
    public static final int WATCH_STATUS_NONE = 0;
    public static final int WATCH_STATUS_TO_WATCH = 1;
    public static final int WATCH_STATUS_WATCHING = 2;
    public static final int WATCH_STATUS_WATCHED = 3;
    public static final int WATCH_STATUS_DROPPED = 4;
    public static final int WATCH_STATUS_OTHER = 5;

    //primary attributes
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id") String mId;
    @Ignore String mVoteCount;
    @ColumnInfo(name = "vote_average") String mVoteAverage;
    @ColumnInfo(name = "title") String mTitle;
    @Ignore double mPopularity;
    @ColumnInfo(name = "poster_path") String mPosterPath;
    public @ColumnInfo(name = "original_language") String mOriginalLanguage;
    public @ColumnInfo(name = "original_title") String mOriginalTitle;
    public @ColumnInfo(name = "genre_ids") ArrayList<Integer> mGenreIds;
    @ColumnInfo(name = "backdrop_path") String mBackdropPath;
    @ColumnInfo(name = "overview") String mOverview;
    @ColumnInfo(name = "release_date") String mReleaseDate;
    public @ColumnInfo(name = "list_id") int mListId;

    //detail attributes
    @Ignore ArrayList<CastData> mCast;

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

    public String getOriginalLanguage() {
        return mOriginalLanguage;
    }

    public String getOriginalTitle() {
        return mOriginalTitle;
    }

    public List<Integer> getGenreIds() {
        return mGenreIds;
    }

    public String getGenreByIndex(int index) {
        try {
            if (mGenreIds.get(index) == null || mGenreIds.get(index).equals("")) {
                return null;
            } else {
                return MovieDataParser.getGenreById(getGenreIds().get(index));
            }
        } catch (IndexOutOfBoundsException e) {
            return null;
        }

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
}
