/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.upkipp.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.upkipp.onmywatch.utils.network_utils.ApiConstants;

import org.parceler.Parcel;

import java.util.ArrayList;

@Entity(tableName = "series")
@Parcel
public class SeriesData extends MediaData{

    //primary attributes
    ArrayList<String> mCountryOfOrigin;
    @Ignore ArrayList<String> mReviews;

    //detail attributes
    @Ignore ArrayList<Season> mSeasons;

    //constructor for parceler
    @Ignore
    public SeriesData() {

    }

    @Ignore
    public SeriesData(@NonNull String id, String voteCount, String voteAverage, String title,
                      double popularity, String posterPath, String originalLanguage,
                      String originalTitle, ArrayList<String> countryOfOrigin, ArrayList<Integer> genreIds,
                      String backdropPath, String overview, String releaseDate) {

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
        }

        this.mOriginalLanguage = originalLanguage;
        this.mOriginalTitle = originalTitle;
        this.mCountryOfOrigin = countryOfOrigin;
        this.mGenreIds = genreIds;
        this.mBackdropPath = ApiConstants.IMAGE_BASE_URL +
                ApiConstants.BACKDROP_IMAGE_SIZE +
                backdropPath;
        this.mOverview = overview;
        this.mReleaseDate = releaseDate;
    }

    public SeriesData(@NonNull String id, String voteAverage, String title,
                      String posterPath, String backdropPath,
                      String overview, String releaseDate) {

        //check numbers(int / double) and JSONArray objects and replace error values;
        //Boolean and String error already replaced by default
        this.mId = id;
        this.mVoteAverage = voteAverage;
        this.mTitle = title;
        this.mPosterPath = posterPath;
        this.mBackdropPath = backdropPath;
        this.mOverview = overview;
        this.mReleaseDate = releaseDate;
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

    public void setReleaseDate(String releaseDate) {
        this.mReleaseDate = releaseDate;
    }

    public ArrayList<Season> getSeasons() {
        return mSeasons;
    }

    public void setSeasons(ArrayList<Season> seasons) {
        mSeasons = seasons;
    }
}
