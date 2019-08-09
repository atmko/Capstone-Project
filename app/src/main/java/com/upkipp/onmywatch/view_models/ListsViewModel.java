package com.upkipp.onmywatch.view_models;//package com.upkipp.onmywatch.view_models;

import android.app.Application;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.database.AppDatabase;
import com.upkipp.onmywatch.models.ListCounts;
import com.upkipp.onmywatch.models.UserListModel;
import com.upkipp.onmywatch.models.WatchListModel;

import java.util.List;

public class ListsViewModel extends AndroidViewModel {
    private static final String TAG = ListsViewModel.class.getSimpleName();

    private LiveData<List<UserListModel>> userLists;
    private LiveData<List<WatchListModel>> watchLists;

    private SparseArray<LiveData<ListCounts>> watchStatusCountsList;

    public ListsViewModel(@NonNull Application application) {
        super(application);

        AppDatabase database = AppDatabase.getInstance(this.getApplication());
        Log.d(TAG, "fetching user lists from the database");
        userLists = database.userListsDao().getAllLists();

        Log.d(TAG, "fetching watch list counts from the database");
        watchLists = database.watchListsDao().getAllLists();

        watchStatusCountsList = loadWatchListCounts(application, database);
    }

    private SparseArray<LiveData<ListCounts>> loadWatchListCounts(Application application, AppDatabase database) {
        int[] watchStatusValues =
                application.getResources().getIntArray(R.array.watch_status_series_values);

        SparseArray<LiveData<ListCounts>> tempSparseArray = new SparseArray<>();

        for (int watchStatus: watchStatusValues) {
            tempSparseArray.put(watchStatus, database.mediaDataDao()
                    .getAgnosticWatchStatusCount(watchStatus));

        }

        return tempSparseArray;
    }

    public LiveData<List<WatchListModel>> getWatchLists() {
        return watchLists;
    }

    public LiveData<List<UserListModel>> getUserLists() {
        return userLists;
    }

    public SparseArray<LiveData<ListCounts>> getWatchStatusCountList() {
        return watchStatusCountsList;
    }
}
