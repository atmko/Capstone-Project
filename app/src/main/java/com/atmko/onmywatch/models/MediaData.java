/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: access is weaker to accommodate parceler library
@SuppressWarnings("WeakerAccess")
abstract public class MediaData {
    public static final String MEDIA_TYPE_KEY = "media_type";
    public static final int WATCH_STATUS_NONE = 0;
    public static final int WATCH_STATUS_TO_WATCH = 1;
    public static final int WATCH_STATUS_WATCHING = 2;
    public static final int WATCH_STATUS_WATCHED = 3;
    public static final int WATCH_STATUS_DROPPED = 4;

    public static final String WATCH_STATUS_KEY = "watch_status";
    public static final String USER_RATING_KEY = "user_rating";

    public static final String TAGS_KEY = "tags";

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
    public @ColumnInfo(name = "tags") List<SearchTag> searchTags;

    //secondary attributes(details)
    @Ignore ArrayList<CastData> mCast;
    @Ignore ArrayList<Map<String, String>> mVideos;
    @Ignore ArrayList<Map<String, String>> mReviews;
    @ColumnInfo(name = "release_status") String mReleaseStatus;

    //trakt attributes
    @ColumnInfo(name = "trakt_id") String mTraktId;

    @ColumnInfo(name = "watch_status") int mWatchStatus;
    @ColumnInfo(name = "user_rating") int mUserRating;

    @Ignore private String mUniqueExternalId;

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

    public void createTags() {
        searchTags = new ArrayList<>();
        List objectToParse = Arrays.asList(mTitle, mOriginalTitle);
        parseTags(objectToParse);
    }

    public void parseTags(Object objectToParse) {
        //if list iterate through list and repeat method for each item
        if (objectToParse instanceof List) {
            for (Object listItem: ((List) objectToParse)) {
                parseTags(listItem);
            }

            //if string, trim string, if there are spaces, split and repeat method otherwise, create tag
        } else if (objectToParse instanceof String) {
            String trimmedString = ((String) objectToParse).trim();

            if (trimmedString.contains(" ")) {
                String[] strings = ((String) objectToParse).split(" ");
                for (String string: strings) {
                    parseTags(string);
                }

            } else {
                SearchTag searchTag = new SearchTag(trimmedString);
                if (!searchTags.contains(searchTag)) {
                    searchTags.add(searchTag);
                }
            }
        }
    }

    public String getReleaseStatus() {
        return mReleaseStatus;
    }

    public void setReleaseStatus(String releaseStatus) {
        this.mReleaseStatus = releaseStatus;
    }

    public String getTraktId() {
        return mTraktId;
    }

    public void setTraktId(String traktId) {
        this.mTraktId = traktId;
    }

    public ArrayList<CastData> getCast() {
        return mCast;
    }

    public void setCast(ArrayList<CastData> cast) {
        this.mCast = cast;
    }

    public ArrayList<Map<String, String>> getVideos() {
        return mVideos;
    }

    public void setVideos(ArrayList<Map<String, String>> videoList) {
        this.mVideos = videoList;
    }

    public ArrayList<Map<String, String>> getReviews() {
        return mReviews;
    }

    public void setReviews(ArrayList<Map<String, String>> reviews) {
        this.mReviews = reviews;
    }

    public int getWatchStatus() {
        return mWatchStatus;
    }

    public void setWatchStatus(int watchStatus) {
        this.mWatchStatus = watchStatus;
    }

    public int getUserRating() {
        return mUserRating;
    }

    public void setUserRating(int userRating) {
        this.mUserRating = userRating;
    }

    public String getUniqueExternalId() {
        return mUniqueExternalId;
    }

    public void setUniqueExternalId(String mDocumentId) {
        this.mUniqueExternalId = mDocumentId;
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

    public abstract String getMediaUrl(Context context, String mediaId);

    Map<String, Object> getFirebaseMediaDataMap(MediaData mediaData) {
        Map<String, Object> mediaDataMap = new HashMap<>();

        mediaDataMap.put(ApiConstants.ID_KEY, mediaData.getId());
        mediaDataMap.put(TraktApiConstants.TRAKT_ID_KEY, mediaData.getTraktId());
        mediaDataMap.put(ApiConstants.VOTE_AVERAGE_KEY, mediaData.getVoteAverage());
        mediaDataMap.put(ApiConstants.POSTER_PATH_KEY, mediaData.getPosterPath());
        mediaDataMap.put(ApiConstants.ORIG_LANG_KEY, mediaData.getOriginalLanguage());
        mediaDataMap.put(ApiConstants.GENRES_KEY, mediaData.getGenres());
        mediaDataMap.put(ApiConstants.BACKDROP_PATH_KEY, mediaData.getBackdropPath());
        mediaDataMap.put(ApiConstants.OVERVIEW_KEY, mediaData.getOverview());
        mediaDataMap.put(ApiConstants.RELEASE_STATUS_KEY, mediaData.getReleaseStatus());
        mediaDataMap.put(WATCH_STATUS_KEY, mediaData.getWatchStatus());
        mediaDataMap.put(USER_RATING_KEY, mediaData.getUserRating());

        return mediaDataMap;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof MediaData) {
            return ((MediaData) obj).getId().equals(this.mId);

        } else {
            return super.equals(obj);

        }
    }
}
