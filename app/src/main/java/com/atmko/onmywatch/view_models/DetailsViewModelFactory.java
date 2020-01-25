/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;


import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.atmko.onmywatch.database.AppDatabase;

public class DetailsViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    private final AppDatabase mDatabase;
    private final int mMediaType;
    private final String mId;

    public DetailsViewModelFactory(AppDatabase database, int mediaType, String movieId) {
        mDatabase = database;
        mMediaType = mediaType;
        mId = movieId;
    }

    @Override
    public @NonNull
    <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        //noinspection unchecked
        return (T) new DetailsViewModel(mDatabase, mMediaType, mId);
    }
}
