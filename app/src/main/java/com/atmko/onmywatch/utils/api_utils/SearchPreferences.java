/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.api_utils;

import com.atmko.onmywatch.utils.api_utils.ApiConstants;

import org.parceler.Parcel;

//TODO: access is weaker to accommodate parceler library
@SuppressWarnings("WeakerAccess")
@Parcel
public final class SearchPreferences {
    //urls & paths
    private static String mQueryUrlString;//final url used by MovieLoader

    //sort parameters---------------------------------------------------------------
    public static final String SORT_BY_POPULAR = "popular";
    public static final String SORT_BY_TOP_RATED = "top_rated";
    public static final String SORT_BY_FAVORITES = "favorites";

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

    //---------------------------------------------------------

    //constructor for parceler
    public SearchPreferences() {
        mLanguageParamVal = ENG_US;
        //TODO implement region support
        mRegion = ApiConstants.USER_LOCALE;
        mQuery = "";
    }

    public String[] getPreferenceValueList(){
        String[] preferenceList = new String[9];
        preferenceList[0] = ApiConstants.API_KEY;
        preferenceList[1] = getLanguageValue();
        preferenceList[2] = getQuery();
        preferenceList[3] = String.valueOf(getTargetPage());
        preferenceList[4] = Boolean.toString(getIncludeAdult());
        preferenceList[5] = getRegion();
        preferenceList[6] = getYear();
        preferenceList[7] = getPrimaryReleaseYear();
        preferenceList[8] = getFirstAirDateYear();

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

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public void setCurrentPage(int pageNum) {
        mCurrentPage = pageNum;
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

    public void setLanguageParamVal(String languageParamVal) {
        this.mLanguageParamVal = languageParamVal;
    }

    public String getRegion() {
        return mRegion;
    }

    public void setRegion(String region) {
        this.mRegion = region;
    }

    public String getYear() {
        return mYear;
    }

    public void setYear(String year) {
        this.mYear = year;
    }

    public String getPrimaryReleaseYear() {
        return mPrimaryReleaseYear;
    }

    public void setPrimaryReleaseYear(String primaryReleaseYear) {
        this.mPrimaryReleaseYear = primaryReleaseYear;
    }

    public String getFirstAirDateYear() {
        return mFirstAirDateYear;
    }

    public void setFirstAirDateYear(String firstAirDateYear) {
        this.mFirstAirDateYear = firstAirDateYear;
    }

    public boolean getIncludeAdult() {
        return mIncludeAdult;
    }

    public void setIncludeAdult(boolean includeAdult) {
        this.mIncludeAdult = includeAdult;
    }

    public String getQueryUrlString() {
        return mQueryUrlString;
    }

    public void setQueryUrlString(String urlString) {
        mQueryUrlString = urlString;
    }
}
