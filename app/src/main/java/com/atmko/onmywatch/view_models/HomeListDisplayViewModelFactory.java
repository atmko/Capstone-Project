/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;


import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.atmko.onmywatch.database.AppDatabase;

public class HomeListDisplayViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    private final AppDatabase mDatabase;
    private final String mListName;

    public HomeListDisplayViewModelFactory(AppDatabase database, String listName) {
        this.mDatabase = database;
        this.mListName = listName;
    }

    @Override
    public @NonNull
    <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        //noinspection unchecked
        return (T) new HomeListDisplayViewModel(mDatabase, mListName);
    }
}
