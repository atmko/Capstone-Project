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
import com.atmko.onmywatch.models.UserListModel;

import java.util.List;

public class AddToListViewModel extends ViewModel {
    private static final String TAG = AddToListViewModel.class.getSimpleName();

    private LiveData<Integer> watchStatus;
    private final LiveData<List<UserListModel>> allUserLists;
    private LiveData<List<UserListModel>> containingUserLists;

    AddToListViewModel(@NonNull AppDatabase database, int mediaType, String mediaId) {
        Log.d(TAG, "fetching watch status from the database");
        if (mediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            watchStatus = database.movieDataDao().getMoviesWatchStatus(mediaId);


        } else if (mediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            watchStatus = database.seriesDataDao().getSeriesWatchStatus(mediaId);

        }

        Log.d(TAG, "fetching all user lists from the database");
        allUserLists = database.userListsDao().getAllLists();

        Log.d(TAG, "fetching all user lists from the database");
        if (mediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            containingUserLists = database.movieDataRecordsDao().getAllListsContainingMedia(mediaId);


        } else if (mediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            containingUserLists = database.seriesDataRecordsDao().getAllListsContainingMedia(mediaId);

        }
    }

    public LiveData<Integer> getWatchStatus() {
        return watchStatus;
    }

    public LiveData<List<UserListModel>> getContainingLists() {
        return containingUserLists;
    }

    public LiveData<List<UserListModel>> getAllUserLists() {
        return allUserLists;
    }
}
