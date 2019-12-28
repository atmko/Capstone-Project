/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import androidx.annotation.NonNull;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Parcel
@Entity(tableName = "movies")
public class MovieData extends MediaData{

    //primary attributes
    @Ignore boolean mVideo;
    @ColumnInfo(name = "adult") boolean mAdult;

    //detail attributes
    @Ignore List<Map<String, String>> mVideos;
    @Ignore List<String> mReviews;

    //constructor for parceler
    @Ignore
    public MovieData() {

    }

    @Ignore
    public MovieData(@NonNull String id, String voteCount, boolean video, String voteAverage,
                     String title, double popularity, String posterPath, String originalLanguage,
                     String originalTitle, ArrayList<String> genres, String backdropPath,
                     boolean adult, String overview, String releaseDate) {

        this.mId = id;
        this.mVoteCount = voteCount;
        this.mVideo = video;
        this.mVoteAverage = voteAverage;
        this.mTitle = title;
        this.mPopularity = popularity;

        if (posterPath == null) {
            this.mPosterPath = null;
        } else {
            this.mPosterPath = ApiConstants.IMAGE_BASE_URL +
                    ApiConstants.POSTER_IMAGE_SIZE +
                    posterPath;

            this.mSpotlightPosterPath = ApiConstants.IMAGE_BASE_URL +
                    ApiConstants.SPOTLIGHT_POSTER_IMAGE_SIZE +
                    posterPath;
        }

        this.mOriginalLanguage = originalLanguage;
        this.mOriginalTitle = originalTitle;
        this.mGenres = genres;
        this.mBackdropPath = ApiConstants.IMAGE_BASE_URL +
                ApiConstants.BACKDROP_IMAGE_SIZE +
                backdropPath;
        this.mAdult = adult;
        this.mOverview = overview;
        this.mReleaseDate = releaseDate;

        try {
            ScheduledMedia scheduledMedia = new ScheduledMedia();
            scheduledMedia.setAirDate(releaseDate);

            if (scheduledMedia.getBestTimeDifference() > 0) {
                this.mCountdown = scheduledMedia.getBestTimeDifference();

            } else {
                this.mCountdown = 0;
            }

        } catch (ScheduledMedia.DateFormatException e) {
            this.mCountdown = 0;
        }
    }

    public MovieData(@NonNull String id, String traktId, String voteAverage, String title,
                     String posterPath, String originalLanguage, String originalTitle,
                     ArrayList<String> genres, boolean adult, String backdropPath, String overview,
                     String releaseDate, String releaseStatus) {

        this.mId = id;
        this.mTraktId = traktId;
        this.mVoteAverage = voteAverage;
        this.mTitle = title;
        this.mPosterPath = posterPath;
        this.mOriginalLanguage = originalLanguage;
        this.mOriginalTitle = originalTitle;
        this.mGenres = genres;
        this.mAdult = adult;
        this.mBackdropPath = backdropPath;
        this.mOverview = overview;
        this.mReleaseDate = releaseDate;
        this.mReleaseStatus = releaseStatus;
    }

    public boolean isVideo() {
        return mVideo;
    }

    public boolean isAdult() {
        return mAdult;
    }

    @Override
    public String getMediaUrl(Context context, String mediaId) {
        return context.getString(R.string.movie_base_url) + "/" + mediaId;
    }
}
