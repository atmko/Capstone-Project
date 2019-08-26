package com.atmko.onmywatch.view_models;//package com.upkipp.onmywatch.view_models;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SeriesNotifier;

import java.util.List;

public class DetailsViewModel extends ViewModel {
    private static final String TAG = DetailsViewModel.class.getSimpleName();

    private LiveData<Integer> watchStatus;
    private LiveData<List<String>> containingUserLists;
    private LiveData mediaNotifier;

    public DetailsViewModel(@NonNull AppDatabase database, int mediaType, String mediaId) {

        if (mediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            watchStatus = database.movieDataDao().getMoviesWatchStatus(mediaId);
            containingUserLists =
                    database.movieDataRecordsDao().getAllListNamesContainingMedia(mediaId);
            mediaNotifier = database.movieNotifierDao().getNotifierById(mediaId);

        } else if (mediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            watchStatus = database.seriesDataDao().getSeriesWatchStatus(mediaId);
            containingUserLists =
                    database.seriesDataRecordsDao().getAllListNamesContainingMedia(mediaId);
            mediaNotifier = database.seriesNotifierDao().getNotifierById(mediaId);

        }

        Log.d(TAG, "fetching watch status from the database");

        Log.d(TAG, "fetching user containing lists from the database");

        Log.d(TAG, "fetching media notifier from the database");
    }

    public LiveData<Integer> getWatchStatus() {
        return watchStatus;
    }

    public LiveData<List<String>> getContainingLists() {
        return containingUserLists;
    }

    public LiveData getMediaNotifier() {
        return mediaNotifier;
    }
}
