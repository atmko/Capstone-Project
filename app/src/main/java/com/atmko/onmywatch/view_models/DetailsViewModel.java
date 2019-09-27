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

import java.util.List;

public class DetailsViewModel extends ViewModel {
    private static final String TAG = DetailsViewModel.class.getSimpleName();

    private LiveData mediaData;
    private LiveData<List<String>> containingUserLists;

    DetailsViewModel(@NonNull AppDatabase database, int mediaType, String mediaId) {
        Log.d(TAG, "fetching media from the database");
        Log.d(TAG, "fetching user containing lists from the database");

        if (mediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            mediaData = database.movieDataDao().getMovieById(mediaId);
            containingUserLists =
                    database.movieDataRecordsDao().getAllListNamesContainingMedia(mediaId);

        } else if (mediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            mediaData = database.seriesDataDao().getSeriesById(mediaId);
            containingUserLists =
                    database.seriesDataRecordsDao().getAllListNamesContainingMedia(mediaId);

        }
    }

    public LiveData getMediaData() {
        return mediaData;
    }

    public LiveData<List<String>> getContainingLists() {
        return containingUserLists;
    }
}
