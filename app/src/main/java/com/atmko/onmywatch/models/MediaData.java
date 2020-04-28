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
import com.atmko.onmywatch.utils.api_utils.MovieApiConstants;
import com.atmko.onmywatch.utils.api_utils.SeriesApiConstants;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

    private static final String MATURITY_RATING_PLACEHOLDER = "____";
    private static final String RELEASE_DATE_PLACEHOLDER = "____";
    private static final String NO_STATUS_PLACEHOLDER = "No Status";

    //primary attributes
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id") String mId = "";
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
    public @ColumnInfo(name = "tags") List<SearchMediaTag> searchTags;

    //secondary attributes(details)
    @Ignore ArrayList<Map<String, String>> mVideos;
    @Ignore ArrayList<Review> mReviews;
    public @ColumnInfo(name = "maturityRating") String mMaturityRating;
    @ColumnInfo(name = "release_status") String mReleaseStatus;

    //trakt attributes
    @ColumnInfo(name = "trakt_id") String mTraktId;

    @ColumnInfo(name = "watch_status") int mWatchStatus;
    @ColumnInfo(name = "user_rating") int mUserRating;

    @Ignore String mUniqueExternalId;

    public String getId() {
        return mId;
    }

    public String getVoteAverage() {
        if (mVoteAverage == null || mVoteAverage.equals("")) {
            return "0.0";

        } else {
            return mVoteAverage;
        }
    }

    public String getTitle() {
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
        if (mOverview != null && !mOverview.equals("")) {
            return mOverview;

        } else {
            return "";
        }
    }

    public String getReleaseDate() {
        return mReleaseDate;
    }

    public String getDisplayReleaseDate() {
        if (mReleaseDate != null && !mReleaseDate.equals("")) {
            return mReleaseDate;

        } else {
            return RELEASE_DATE_PLACEHOLDER;
        }
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
                SearchMediaTag searchTag = new SearchMediaTag(trimmedString);
                if (!searchTags.contains(searchTag)) {
                    searchTags.add(searchTag);
                }
            }
        }
    }

    public String getMaturityRating() {
        if (mMaturityRating != null && !mMaturityRating.equals("")) {
            return mMaturityRating;

        } else {
            return MATURITY_RATING_PLACEHOLDER;
        }
    }

    public void setMaturityRating(String maturityRating) {
        this.mMaturityRating = maturityRating;
    }

    public String getReleaseStatus() {
        return mReleaseStatus;
    }

    public String getDisplayReleaseStatus() {
        return mReleaseStatus != null && !mReleaseStatus.equals("")
                ? mReleaseStatus : NO_STATUS_PLACEHOLDER;
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

    public ArrayList<Map<String, String>> getVideos() {
        if (mVideos == null) {
            return new ArrayList<>();

        } else {
            return mVideos;
        }
    }

    public void setVideos(ArrayList<Map<String, String>> videoList) {
        this.mVideos = videoList;
    }

    public ArrayList<Review> getReviews() {
        return mReviews;
    }

    public void setReviews(ArrayList<Review> reviews) {
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
                context.getResources().getStringArray(R.array.watch_status_titles);

        return watchStatusTitles[watchStatus];
    }

    public static String getMediaTypeTitle(int mediaType, Context context) {
        String[] mediaTypeTitles =
                context.getResources().getStringArray(R.array.media_types);

        return mediaTypeTitles[mediaType];
    }

    public abstract String getMediaUrl(Context context, String mediaId);

    public boolean isPendingRelease() {
        boolean isPendingReleasedStatus =
                !mReleaseStatus.equals(MovieApiConstants.RELEASE_STATUS_RELEASED)
                        && !mReleaseStatus.equals(ApiConstants.TextReplacement.REPLACEMENT_RETURNING_SERIES)
                        && !mReleaseStatus.equals(SeriesApiConstants.RELEASE_STATUS_PILOT)
                        && !mReleaseStatus.equals(SeriesApiConstants.RELEASE_STATUS_ENDED)
                        && !mReleaseStatus.equals(ApiConstants.RELEASE_STATUS_CANCELED);

        //check if release date in future because release status may be wrong(like when a movie has release status but local country release date is in the future)
        ScheduledMedia scheduledMedia = new ScheduledMedia();
        try {
            scheduledMedia.setAirDate(getReleaseDate());
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        boolean releaseDateInFuture = true;
        if (scheduledMedia.getBestLocalAirDate() != null) {
            releaseDateInFuture = !scheduledMedia.getBestLocalAirDate().before(new Date());
        }
        return releaseDateInFuture || isPendingReleasedStatus;
    }

    public abstract boolean supportsNotifiers();

    Map<String, Object> getFirebaseMediaDataMap(MediaData mediaData) {
        Map<String, Object> mediaDataMap = new HashMap<>();

        List<String> tagStrings = new ArrayList<>();
        for (SearchMediaTag searchTag: searchTags) {
            tagStrings.add(searchTag.mTag);
        }

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
        mediaDataMap.put(TAGS_KEY, tagStrings);

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
