/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Ignore;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;

import org.parceler.Parcel;

import java.util.List;

//TODO: access is weaker to accommodate parceler library
@Parcel
public class PersonData {
    String mId;
    String mGender;
    String mBiography;
    String mProfession;
    String mDateOfBirth;
    String mProfilePath;
    List<MovieData> mKnownForMovies;
    List<SeriesData> mKnownForSeries;
    String mName;
    double mPopularity;
    boolean mAdult;

    //constructor for parceler
    @Ignore
    public PersonData() {

    }

    @SuppressWarnings("unchecked")
    @Ignore
    public PersonData(@NonNull String id, String profilePath, List<List> knownFor, String name,
                      double popularity, boolean adult) {

        this.mId = id;
        if (profilePath == null) {
            this.mProfilePath = null;
        } else {
            this.mProfilePath = ApiConstants.IMAGE_BASE_URL +
                    ApiConstants.POSTER_IMAGE_SIZE +
                    profilePath;
        }

        this.mKnownForMovies = knownFor.get(0);
        this.mKnownForSeries = knownFor.get(1);
        this.mName = name;
        this.mPopularity = popularity;

        this.mAdult = adult;
    }

    @Ignore
    public PersonData(@NonNull String id, String profilePath, String name, double popularity,
                      boolean adult) {
        this.mId = id;
        if (profilePath == null) {
            this.mProfilePath = null;
        } else {
            this.mProfilePath = ApiConstants.IMAGE_BASE_URL +
                    ApiConstants.POSTER_IMAGE_SIZE +
                    profilePath;
        }
        this.mName = name;
        this.mPopularity = popularity;

        this.mAdult = adult;
    }

    public void setOverview(String overview) {
        this.mBiography = overview;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.mDateOfBirth = dateOfBirth;
    }

    public void setProfession(String profession) {
        this.mProfession = profession;
    }

    public void setKnownForMovies(List<MovieData> knownForMovies) {
        this.mKnownForMovies = knownForMovies;
    }

    public void setKnownForSeries(List<SeriesData> knownForSeries) {
        this.mKnownForSeries = knownForSeries;
    }

    public String getId() {
        return mId;
    }

    public String getOverview(Context context) {
        if (mBiography == null) {
            return context.getResources().getString(R.string.professions_placeholder);
        }

        if (mBiography.equals("")) {
            return context.getResources().getString(R.string.professions_placeholder);
        }

        return mBiography;
    }

    public String getProfession(Context context) {
        if (mProfession == null) {
            return context.getResources().getString(R.string.professions_placeholder);
        }

        if (mProfession.equals("")) {
            return context.getResources().getString(R.string.professions_placeholder);
        }

        return mProfession;
    }

    public String getDateOfBirth(Context context) {
        if (mDateOfBirth == null) {
            return context.getResources().getString(R.string.date_of_birth_placeholder);
        }

        if (mDateOfBirth.equals("")) {
            return context.getResources().getString(R.string.date_of_birth_placeholder);
        }

        return mDateOfBirth;
    }

    public String getProfilePath() {
        return mProfilePath;
    }

    public List<SeriesData> getKnownForSeries() {
        return mKnownForSeries;
    }

    public List<MovieData> getKnownForMovies() {
        return mKnownForMovies;
    }

    public String getName() {
        return mName;
    }
}
