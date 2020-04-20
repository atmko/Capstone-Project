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

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity(tableName = "series")
@Parcel
public class SeriesData extends MediaData{
    public static final String NEXT_EPISODE_KEY = "next_episode";

    //primary attributes
    @ColumnInfo(name = "country_of_origin") ArrayList<String> mCountryOfOrigin;
    @ColumnInfo(name = "network") String mNetwork;
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
                      String overview, String releaseDate, String maturityRating, String releaseStatus,
                      Episode nextEpisodeToAir, List<SearchMediaTag> searchTags, String network) {

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
        this.mMaturityRating = maturityRating;
        this.mReleaseStatus = releaseStatus;
        this.mNextEpisodeToAir = nextEpisodeToAir;
        this.searchTags = searchTags;
        this.mNetwork = network;
    }

    public ArrayList<String> getCountryOfOrigin() {
        return mCountryOfOrigin;
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

    public String getNetwork() {
        return mNetwork != null && !mNetwork.equals("") ? mNetwork : SeriesApiConstants.NO_NETWORKS;
    }

    public void setNetwork(String network) {
        this.mNetwork = network;
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
        firebaseMediaDataMap.put(SeriesApiConstants.RATING_KEY, getMaturityRating());
        firebaseMediaDataMap.put(NEXT_EPISODE_KEY,
                Converters.episodeToString(getNextEpisodeToAir()));
        firebaseMediaDataMap.put(SeriesApiConstants.NETWORK_KEY, getNetwork());

        return firebaseMediaDataMap;
    }
}
