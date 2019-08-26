package com.atmko.onmywatch.view_models;//package com.upkipp.onmywatch.view_models;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.database.AppDatabase;

import java.util.List;

public class DetailsViewModel extends ViewModel {
    private static final String TAG = DetailsViewModel.class.getSimpleName();

    private LiveData<Integer> watchStatus;
    private LiveData<List<String>> containingUserLists;

    public DetailsViewModel(@NonNull AppDatabase database, int mediaType, String mediaId) {

        if (mediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            watchStatus = database.movieDataDao().getMoviesWatchStatus(mediaId);
            containingUserLists =
                    database.movieDataRecordsDao().getAllListNamesContainingMedia(mediaId);

        } else if (mediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            watchStatus = database.seriesDataDao().getSeriesWatchStatus(mediaId);
            containingUserLists =
                    database.seriesDataRecordsDao().getAllListNamesContainingMedia(mediaId);

        }

        Log.d(TAG, "fetching watch status from the database");

        Log.d(TAG, "fetching user containing lists from the database");
    }

    public LiveData<Integer> getWatchStatus() {
        return watchStatus;
    }

    public LiveData<List<String>> getContainingLists() {
        return containingUserLists;
    }
}
