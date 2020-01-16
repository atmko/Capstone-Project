/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SeriesNotifier;

import java.util.List;

public class DetailsViewModel extends ViewModel {
    private static final String TAG = DetailsViewModel.class.getSimpleName();

    private LiveData mediaData;
    private LiveData<List<String>> containingUserLists;
    private LiveData notifiers;

    DetailsViewModel(@NonNull AppDatabase database, int mediaType, String mediaId) {
        Log.d(TAG, "fetching media from the database");
        Log.d(TAG, "fetching user containing lists from the database");

        if (mediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            mediaData = database.movieDataDao().getMovieById(mediaId);
            containingUserLists =
                    database.movieDataRecordsDao().getAllListNamesContainingMedia(mediaId);
            notifiers =
                    database.movieNotifierDao().getNotifiersWithMediaId(mediaId);

        } else if (mediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            mediaData = database.seriesDataDao().getSeriesById(mediaId);
            containingUserLists =
                    database.seriesDataRecordsDao().getAllListNamesContainingMedia(mediaId);
            notifiers =
                    database.seriesNotifierDao().getNotifiersWithMediaId(mediaId);
        }
    }

    public LiveData getMediaData() {
        return mediaData;
    }

    public LiveData<List<String>> getContainingLists() {
        return containingUserLists;
    }

    public LiveData<List<MediaNotifier>> getNotifiers() {
        return notifiers;
    }
}
