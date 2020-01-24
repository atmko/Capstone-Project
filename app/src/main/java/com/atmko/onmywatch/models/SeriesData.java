/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.Converters;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.SeriesApiConstants;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.Map;

@Entity(tableName = "series")
@Parcel
public class SeriesData extends MediaData{
    public static final String NEXT_EPISODE_KEY = "next_episode";

    //primary attributes
    @ColumnInfo(name = "country_of_origin") ArrayList<String> mCountryOfOrigin;
    @Ignore ArrayList<String> mReviews;
    @ColumnInfo(name = "next_episode") Episode mNextEpisodeToAir;

    //constructor for parceler
    @Ignore
    public SeriesData() {

    }

    @Ignore
    public SeriesData(@NonNull String id, String voteCount, String voteAverage, String title,
                      double popularity, String posterPath, String originalLanguage,
                      String originalTitle, ArrayList<String> countryOfOrigin,
                      ArrayList<String> genres, String backdropPath, String overview,
                      String releaseDate) {

        this.mId = id;
        this.mVoteCount = voteCount;
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
        this.mCountryOfOrigin = countryOfOrigin;
        this.mGenres = genres;
        this.mBackdropPath = ApiConstants.IMAGE_BASE_URL +
                ApiConstants.BACKDROP_IMAGE_SIZE +
                backdropPath;
        this.mOverview = overview;
        this.mReleaseDate = releaseDate;
    }

    public SeriesData(@NonNull String id, String traktId, String voteAverage, String title,
                      String posterPath, String originalLanguage, String originalTitle,
                      ArrayList<String> countryOfOrigin, ArrayList<String> genres, String backdropPath,
                      String overview, String releaseDate, String releaseStatus, Episode nextEpisodeToAir) {

        this.mId = id;
        this.mTraktId = traktId;
        this.mVoteAverage = voteAverage;
        this.mTitle = title;
        this.mPosterPath = posterPath;
        this.mOriginalLanguage = originalLanguage;
        this.mOriginalTitle = originalTitle;
        this.mCountryOfOrigin = countryOfOrigin;
        this.mGenres = genres;
        this.mBackdropPath = backdropPath;
        this.mOverview = overview;
        this.mReleaseDate = releaseDate;
        this.mReleaseStatus = releaseStatus;
        this.mNextEpisodeToAir = nextEpisodeToAir;
    }

    public String getVoteAverage() {
        return mVoteAverage;
    }

    public ArrayList<String> getCountryOfOrigin() {
        return mCountryOfOrigin;
    }

    public void setCountryOfOrigin(ArrayList<String> countryOfOrigin) {
        this.mCountryOfOrigin = countryOfOrigin;
    }

    public String getReleaseDate() {
        return mReleaseDate;
    }

    public Episode getNextEpisodeToAir() {
        return mNextEpisodeToAir;
    }

    public void setNextEpisodeToAir(Episode nextEpisodeToAir) {
        this.mNextEpisodeToAir = nextEpisodeToAir;
    }

    @Override
    public String getMediaUrl(Context context, String mediaId) {
        return context.getString(R.string.series_base_url) + "/" + mediaId;
    }

    public Map<String, Object> parseMediaDataToDataMap() {
        Map<String, Object> firebaseMediaDataMap = getFirebaseMediaDataMap(this);

        firebaseMediaDataMap.put(SeriesApiConstants.ORIGIN_COUNTRY_KEY, getCountryOfOrigin());
        firebaseMediaDataMap.put(SeriesApiConstants.NAME_KEY, getTitle());
        firebaseMediaDataMap.put(SeriesApiConstants.ORIG_NAME_KEY, getOriginalTitle());
        firebaseMediaDataMap.put(SeriesApiConstants.FIRST_AIR_DATE_KEY, getReleaseDate());
        firebaseMediaDataMap.put(NEXT_EPISODE_KEY,
                Converters.scheduledMediaToLong(getNextEpisodeToAir()));

        return firebaseMediaDataMap;
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public static SeriesData parseDataMapToMediaData(Map<String, Object> firebaseDataMap) {
        Episode episode = firebaseDataMap.get(NEXT_EPISODE_KEY) == null ? null
                : Converters.longToEpisode((long) firebaseDataMap.get(NEXT_EPISODE_KEY));

        SeriesData seriesData = new SeriesData(
                (String) firebaseDataMap.get(ApiConstants.ID_KEY),
                ((String) firebaseDataMap.get(TraktApiConstants.TRAKT_ID_KEY)),
                (String) firebaseDataMap.get(ApiConstants.VOTE_AVERAGE_KEY),
                (String) firebaseDataMap.get(SeriesApiConstants.NAME_KEY),
                (String) firebaseDataMap.get(ApiConstants.POSTER_PATH_KEY),
                (String) firebaseDataMap.get(ApiConstants.ORIG_LANG_KEY),
                (String) firebaseDataMap.get(SeriesApiConstants.ORIG_NAME_KEY),
                (ArrayList<String>) firebaseDataMap.get(SeriesApiConstants.ORIGIN_COUNTRY_KEY),
                (ArrayList<String>) firebaseDataMap.get(ApiConstants.GENRES_KEY),
                (String) firebaseDataMap.get(ApiConstants.BACKDROP_PATH_KEY),
                (String) firebaseDataMap.get(ApiConstants.OVERVIEW_KEY),
                (String) firebaseDataMap.get(SeriesApiConstants.FIRST_AIR_DATE_KEY),
                (String) firebaseDataMap.get(ApiConstants.RELEASE_STATUS_KEY),
                episode
        );

        seriesData.setWatchStatus(
                ((Long) firebaseDataMap.get(MediaData.WATCH_STATUS_KEY)).intValue());
        seriesData.setUserRating(
                ((Long) firebaseDataMap.get(MediaData.USER_RATING_KEY)).intValue());

        return seriesData;
    }
}
