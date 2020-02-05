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
import com.atmko.onmywatch.database.Converters;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.MovieApiConstants;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Parcel
@Entity(tableName = "movies")
public class MovieData extends MediaData{
    public static final String SCHEDULED_MEDIA_KEY = "scheduled_media";

    //primary attributes
    @Ignore boolean mVideo;
    @ColumnInfo(name = "adult") boolean mAdult;
    @ColumnInfo(name = "scheduled_media") ScheduledMedia mScheduledMedia;

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
            this.mScheduledMedia = scheduledMedia;
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }
    }

    public MovieData(@NonNull String id, String traktId, String voteAverage, String title,
                     String posterPath, String originalLanguage, String originalTitle,
                     ArrayList<String> genres, boolean adult, String backdropPath, String overview,
                     String releaseDate, String releaseStatus, List<SearchTag> searchTags,
                     ScheduledMedia scheduledMedia) {

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
        this.mScheduledMedia = scheduledMedia;
        this.searchTags = searchTags;
    }

    public boolean isVideo() {
        return mVideo;
    }

    public boolean isAdult() {
        return mAdult;
    }

    public ScheduledMedia getScheduledMedia() {
        return mScheduledMedia;
    }

    public void setScheduledMedia(ScheduledMedia scheduledMedia) {
        this.mScheduledMedia = scheduledMedia;
    }

    @Override
    public String getMediaUrl(Context context, String mediaId) {
        return context.getString(R.string.movie_base_url) + "/" + mediaId;
    }

    public Map<String, Object> parseMediaDataToDataMap() {
        Map<String, Object> firebaseMediaDataMap = getFirebaseMediaDataMap(this);

        List<String> tagStrings = new ArrayList<>();
        for (SearchTag searchTag: searchTags) {
            tagStrings.add(searchTag.mTag);
        }

        firebaseMediaDataMap.put(MovieApiConstants.ADULT_KEY, isAdult());
        firebaseMediaDataMap.put(MovieApiConstants.TITLE_KEY, getTitle());
        firebaseMediaDataMap.put(MovieApiConstants.ORIG_TITLE_KEY, getOriginalTitle());
        firebaseMediaDataMap.put(MovieApiConstants.RELEASE_DATE_KEY, getReleaseDate());
        firebaseMediaDataMap.put(TAGS_KEY, tagStrings);
        firebaseMediaDataMap.put(SCHEDULED_MEDIA_KEY,
                Converters.scheduledMediaToLong(getScheduledMedia()));

        return firebaseMediaDataMap;
    }
}
