package com.atmko.onmywatch.view_models;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.atmko.onmywatch.Fragments.ListsWatchAndUserParentFragment;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;

import java.util.List;

public class ListsResultsViewModel extends ViewModel {
    private static final String TAG = ListsResultsViewModel.class.getSimpleName();

    private LiveData<List<MovieData>> allMoviesInWatchList, allMoviesInUserList;
    private LiveData<List<SeriesData>> allSeriesInWatchList, allSeriesInUserList;
    
    public ListsResultsViewModel(@NonNull AppDatabase database, int listType, int mediaType,
                                 List<String> watchStatusTitleList, String listName) {

        if (listType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            if (mediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                allMoviesInWatchList = database.movieDataDao()
                        .getMoviesByWatchStatus(watchStatusTitleList.indexOf(listName));

            } else if (mediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                allSeriesInWatchList = database.seriesDataDao()
                        .getSeriesByWatchStatus(watchStatusTitleList.indexOf(listName));

            }

        } else if (listType == ListsWatchAndUserParentFragment.LIST_TYPE_USER){
            if (mediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                allMoviesInUserList = database.movieDataRecordsDao().getAllMoviesInList(listName);

            } else if (mediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                allSeriesInUserList = database.seriesDataRecordsDao().getAllSeriesInList(listName);

            }
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
