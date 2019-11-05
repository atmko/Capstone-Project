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
import com.atmko.onmywatch.utils.network_utils.MovieApiConstants;

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
    }

    public MovieData(@NonNull String id, String voteAverage, String title, String posterPath,
                     String originalLanguage, String originalTitle, ArrayList<String> genres,
                     boolean adult, String backdropPath, String overview, String releaseDate,
                     String releaseStatus) {

        this.mId = id;
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

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public static MovieData parseDataMapToMediaData(Map<String, Object> firebaseDataMap) {
        MovieData movieData = new MovieData(
                (String) firebaseDataMap.get(ApiConstants.ID_KEY),
                (String) firebaseDataMap.get(ApiConstants.VOTE_AVERAGE_KEY),
                (String) firebaseDataMap.get(MovieApiConstants.TITLE_KEY),
                (String) firebaseDataMap.get(ApiConstants.POSTER_PATH_KEY),
                (String) firebaseDataMap.get(ApiConstants.ORIG_LANG_KEY),
                (String) firebaseDataMap.get(MovieApiConstants.ORIG_TITLE_KEY),
                (ArrayList<String>) firebaseDataMap.get(ApiConstants.GENRES_KEY),
                (boolean) firebaseDataMap.get(MovieApiConstants.ADULT_KEY),
                (String) firebaseDataMap.get(ApiConstants.BACKDROP_PATH_KEY),
                (String) firebaseDataMap.get(ApiConstants.OVERVIEW_KEY),
                (String) firebaseDataMap.get(MovieApiConstants.RELEASE_DATE_KEY),
                (String) firebaseDataMap.get(ApiConstants.RELEASE_STATUS_KEY)
        );

        movieData.setWatchStatus(
                ((Long) firebaseDataMap.get(MediaData.WATCH_STATUS_KEY)).intValue());
        movieData.setUserRating(
                ((Long) firebaseDataMap.get(MediaData.USER_RATING_KEY)).intValue());

        return movieData;
    }
}
