/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;


import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.database.AppDatabase;

public class RateViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    private final AppDatabase mDatabase;
    private final int mMediaType;
    private final String mMediaId;

    public RateViewModelFactory(AppDatabase database, int mediaType, String mediaId) {

        this.mDatabase = database;
        this.mMediaType = mediaType;
        this.mMediaId = mediaId;
    }

    @Override
    public @NonNull
    <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        //noinspection unchecked
        return (T) new RateViewModel(
                mDatabase, mMediaType, mMediaId);
    }
}
