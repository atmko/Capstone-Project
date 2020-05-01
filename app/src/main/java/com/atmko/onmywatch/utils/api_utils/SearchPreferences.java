/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.api_utils;

import android.content.Context;

import com.atmko.onmywatch.R;

import org.parceler.Parcel;

//TODO: access is weaker to accommodate parceler library
@SuppressWarnings("WeakerAccess")
@Parcel
public final class SearchPreferences {

    //language parameters
    String mLanguageParamVal;
    private static final String ENG_US = "en-US";

    //query parameter
    String mQuery;

    //paging parameters
    int mTargetPage = 0;//the target/desired page and not necessarily the current page
    int mCurrentPage;
    int mTotalPages = 1;

    //include adult parameter
    boolean mIncludeAdult = false;

    //region parameter
    String mRegion;

    //year parameter
    String mYear;

    //primary release year parameter
    String mPrimaryReleaseYear;

    //first air date year parameter
    String mFirstAirDateYear;

    String mGenres;

    String mNetworks;

    String mSortBy;

    //---------------------------------------------------------

    //constructor for parceler
    public SearchPreferences() {
        mLanguageParamVal = ENG_US;
        //TODO implement region support
        mRegion = ApiConstants.USER_LOCALE;
        mQuery = "";
    }

    public String[] getPreferenceValueList(){
        String[] preferenceList = new String[12];
        preferenceList[0] = ApiConstants.API_KEY;
        preferenceList[1] = getLanguageValue();
        preferenceList[2] = getQuery();
        preferenceList[3] = String.valueOf(getTargetPage());
        preferenceList[4] = Boolean.toString(getIncludeAdult());
        preferenceList[5] = getRegion();
        preferenceList[6] = getYear();
        preferenceList[7] = getPrimaryReleaseYear();
        preferenceList[8] = getFirstAirDateYear();
        preferenceList[9] = getGenres();
        preferenceList[10] = getNetworks();
        preferenceList[11] = getSortBy();

        return preferenceList;
    }

    public int getTotalPages() {
        return mTotalPages;
    }

    void setTotalPages(int mTotalPages) {
        this.mTotalPages = mTotalPages;
    }

    public int getTargetPage() {
        return mTargetPage;
    }

    public void setTargetPage(int page) {
        this.mTargetPage = page;
    }

    public String getQuery() {
        return mQuery;
    }

    public void setQuery(String query) {
        this.mQuery = query;
    }

    public String getLanguageValue() {
        return mLanguageParamVal;
    }

    public String getRegion() {
        return mRegion;
    }

    public String getYear() {
        return mYear;
    }

    public String getPrimaryReleaseYear() {
        return mPrimaryReleaseYear;
    }

    public String getFirstAirDateYear() {
        return mFirstAirDateYear;
    }

    public boolean getIncludeAdult() {
        return mIncludeAdult;
    }

    public void setGenres(Context context, int[] genresIndices) {
        String[] genreKeys = context.getResources().getStringArray(R.array.genre_id_key);

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < genresIndices.length; i++) {
            String genreKey = genreKeys[genresIndices[i]];
            if (genreKey.equals(genreKeys[0])) continue;

            stringBuilder.append(genreKey);
            if (i != genresIndices.length - 1) stringBuilder.append(",");
        }

        mGenres = stringBuilder.toString();
    }

    public void setNetworks(Context context, int[] networkIndices) {
        String[] networkKeys = context.getResources().getStringArray(R.array.network_keys);

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < networkIndices.length; i++) {
            String genreKey = networkKeys[networkIndices[i]];
            if (genreKey.equals(networkKeys[0])) continue;

            stringBuilder.append(genreKey);
            if (i != networkIndices.length - 1) stringBuilder.append(",");
        }

        mNetworks = stringBuilder.toString();
    }

    public void setSortBy(Context context, int sortByIndex) {
        String[] sortByKeys = context.getResources().getStringArray(R.array.sort_keys);

        mSortBy = !sortByKeys[sortByIndex].equals(sortByKeys[0]) ? sortByKeys[sortByIndex] : "";
    }

    public String getGenres() {
        return mGenres != null && !mGenres.equals("") ? mGenres : "";
    }

    public String getNetworks() {
        return mNetworks != null && !mNetworks.equals("") ? mNetworks : "";
    }

    public String getSortBy() {
        return mSortBy != null && !mSortBy.equals("") ? mSortBy : "";
    }
}
