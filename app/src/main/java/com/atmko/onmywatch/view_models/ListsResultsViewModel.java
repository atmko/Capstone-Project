/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.atmko.onmywatch.Fragments.ListsWatchAndUserParentFragment;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;

import java.util.List;

public class ListsResultsViewModel extends ViewModel {
    private static final String TAG = ListsResultsViewModel.class.getSimpleName();

    private LiveData<List<MovieData>> allMoviesInWatchList, allMoviesInUserList;
    private LiveData<List<SeriesData>> allSeriesInWatchList, allSeriesInUserList;

    ListsResultsViewModel(@NonNull AppDatabase database, int listType, int mediaType,
                          List<String> watchStatusTitleList, String listName) {

        if (listType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            Log.d(TAG, "fetching media in watch list");
            allMoviesInWatchList = database.movieDataDao()
                    .getMoviesByWatchStatus(watchStatusTitleList.indexOf(listName));

            allSeriesInWatchList = database.seriesDataDao()
                    .getSeriesByWatchStatus(watchStatusTitleList.indexOf(listName));

        } else if (listType == ListsWatchAndUserParentFragment.LIST_TYPE_USER){
            Log.d(TAG, "fetching media in user list");
            allMoviesInUserList = database.movieDataRecordsDao().getAllMoviesInList(listName);
            allSeriesInUserList = database.seriesDataRecordsDao().getAllSeriesInList(listName);
        }
    }

    public LiveData<List<MovieData>> getAllMoviesInWatchList() {
        return allMoviesInWatchList;
    }

    public LiveData<List<MovieData>> getAllMoviesInUserList() {
        return allMoviesInUserList;
    }

    public LiveData<List<SeriesData>> getAllSeriesInWatchList() {
        return allSeriesInWatchList;
    }

    public LiveData<List<SeriesData>> getAllSeriesInUserList() {
        return allSeriesInUserList;
    }
}
