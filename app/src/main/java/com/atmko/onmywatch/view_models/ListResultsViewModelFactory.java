/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;


import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.database.AppDatabase;

import java.util.List;

public class ListResultsViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    private final AppDatabase mDatabase;
    private final int mListType;
    private final int mMediaType;
    private final List<String> mWatchStatusTitleList;
    private final String mListName;

    public ListResultsViewModelFactory(AppDatabase database, int listType, int mediaType,
                                       List<String> watchStatusTitleList, String listName) {

        this.mDatabase = database;
        this.mListType = listType;
        this.mMediaType = mediaType;
        this.mWatchStatusTitleList = watchStatusTitleList;
        this.mListName = listName;
    }

    @Override
    public @NonNull
    <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (MasterActivity.isProMode()) {
            //noinspection unchecked
            return (T) new FirebaseListsResultsViewModel(
                    mListType, mWatchStatusTitleList, mListName);
        } else {
            //noinspection unchecked
            return (T) new ListsResultsViewModel(
                    mDatabase, mListType, mMediaType, mWatchStatusTitleList, mListName);
        }
    }
}
