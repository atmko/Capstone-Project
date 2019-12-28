/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.atmko.onmywatch.Fragments.HomeListDisplayFragment;
import com.atmko.onmywatch.database.AppDatabase;

public class HomeListDisplayViewModel extends ViewModel {
    private LiveData listLiveData;

    HomeListDisplayViewModel(@NonNull AppDatabase database, String listName) {
        switch (listName) {
            case HomeListDisplayFragment.UPCOMING_MOVIES:
                listLiveData = database.movieDataDao().getUserUpcomingMovies();
                break;

            case HomeListDisplayFragment.UNDATED_MOVIES:
                listLiveData = database.movieDataDao().getUndatedMovies();
                break;

            case HomeListDisplayFragment.ALREADY_RELEASED_MOVIES:
                listLiveData = database.movieDataDao().getReleasedMovies();
                break;

            case HomeListDisplayFragment.UPCOMING_EPISODES:
                listLiveData = database.seriesDataDao().getUserUpcomingEpisodes();
                break;

            case HomeListDisplayFragment.UNDATED_SERIES:
                listLiveData = database.seriesDataDao().getUndatedSeries();
                break;

            case HomeListDisplayFragment.ENDED_SERIES:
                listLiveData = database.seriesDataDao().getEndedSeries();
                break;
        }
    }

    public LiveData getHomeDisplayList() {
        return listLiveData;
    }
}
