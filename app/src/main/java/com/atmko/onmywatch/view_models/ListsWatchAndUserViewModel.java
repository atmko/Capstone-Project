/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;

import java.util.List;

public class ListsWatchAndUserViewModel extends AndroidViewModel {
    private static final String TAG = ListsWatchAndUserViewModel.class.getSimpleName();

    private LiveData<List<UserListModel>> userLists;
    private LiveData<List<WatchListModel>> watchLists;

    public ListsWatchAndUserViewModel(@NonNull Application application) {
        super(application);

        AppDatabase database = AppDatabase.getInstance(this.getApplication());
        Log.d(TAG, "fetching user lists from the database");
        userLists = database.userListsDao().getAllLists();

        Log.d(TAG, "fetching watch list counts from the database");
        watchLists = database.watchListsDao().getAllLists();
    }

    public LiveData<List<WatchListModel>> getWatchLists() {
        return watchLists;
    }

    public LiveData<List<UserListModel>> getUserLists() {
        return userLists;
    }
}
