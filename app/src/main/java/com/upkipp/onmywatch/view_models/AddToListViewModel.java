package com.upkipp.onmywatch.view_models;//package com.upkipp.onmywatch.view_models;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.upkipp.onmywatch.MasterActivity;
import com.upkipp.onmywatch.database.AppDatabase;
import com.upkipp.onmywatch.models.UserListModel;

import java.util.List;

public class AddToListViewModel extends ViewModel {
    private static final String TAG = AddToListViewModel.class.getSimpleName();

    private LiveData<Integer> watchStatus;
    private LiveData<List<UserListModel>> allUserLists;
    private LiveData<List<String>> containingUserLists;

    public AddToListViewModel(@NonNull AppDatabase database, int mediaType, String mediaId) {
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
            containingUserLists = database.movieDataRecordsDao().getAllListNamesContainingMedia(mediaId);


        } else if (mediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            containingUserLists = database.seriesDataRecordsDao().getAllListNamesContainingMedia(mediaId);

        }
    }

    public LiveData<Integer> getWatchStatus() {
        return watchStatus;
    }

    public LiveData<List<String>> getContainingLists() {
        return containingUserLists;
    }

    public LiveData<List<UserListModel>> getAllUserLists() {
        return allUserLists;
    }
}
