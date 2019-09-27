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

public class RateViewModel extends ViewModel {
    private static final String TAG = RateViewModel.class.getSimpleName();

    private LiveData mMediaData;

    RateViewModel(@NonNull AppDatabase database, int mediaType, String mediaId) {
        Log.d(TAG, "fetching media from the database");
        if (mediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            mMediaData = database.movieDataDao()
                    .getMovieById(mediaId);

        } else if (mediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            mMediaData = database.seriesDataDao()
                    .getSeriesById(mediaId);

        }
    }

    public LiveData getMediaData () {
        return mMediaData;
    }
}
