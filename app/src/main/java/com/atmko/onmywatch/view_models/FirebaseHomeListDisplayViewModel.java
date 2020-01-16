/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.atmko.onmywatch.Fragments.HomeListDisplayFragment;
import com.atmko.onmywatch.database.daos.FirebaseMovieDataDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class FirebaseHomeListDisplayViewModel extends ViewModel {
    private static final String TAG = FirebaseHomeListDisplayViewModel.class.getSimpleName();

    private MutableLiveData listLiveData = new MutableLiveData();

    FirebaseHomeListDisplayViewModel(String listName) {
        //define list query and media type
        Query listQuery;
        int mediaType = 0;

        switch (listName) {
            case HomeListDisplayFragment.UPCOMING_MOVIES:
                mediaType = MEDIA_TYPE_MOVIE;
                listQuery = FirebaseMovieDataDao.getUserUpcomingMovies();
                break;

            case HomeListDisplayFragment.UNDATED_MOVIES:
                mediaType = MEDIA_TYPE_MOVIE;
                listQuery = FirebaseMovieDataDao.getUndatedMovies();
                break;

            case HomeListDisplayFragment.ALREADY_RELEASED_MOVIES:
                mediaType = MEDIA_TYPE_MOVIE;
                listQuery = FirebaseMovieDataDao.getReleasedMovies();
                break;

            case HomeListDisplayFragment.UPCOMING_EPISODES:
                mediaType = MEDIA_TYPE_SERIES;
                listQuery = FirebaseSeriesDataDao.getUserUpcomingEpisodes();
                break;

            case HomeListDisplayFragment.UNDATED_SERIES:
                mediaType = MEDIA_TYPE_SERIES;
                listQuery = FirebaseSeriesDataDao.getUndatedSeries();
                break;

            case HomeListDisplayFragment.ENDED_SERIES:
                mediaType = MEDIA_TYPE_SERIES;
                listQuery = FirebaseSeriesDataDao.getEndedSeries();
                break;

            default: listQuery = null;
        }

        if (listQuery == null) return;

        //retrieve media data list from firebase database
        final List<MediaData> mediaDataList = new ArrayList<>();

        final int finalMediaType = mediaType;
        listQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.d(TAG, e.getMessage());
                    return;
                }

                if (snapshots == null) return;
                for (DocumentSnapshot document: snapshots.getDocuments()) {
                    if (document.getData() == null) continue;

                    MediaData mediaData;
                    if (finalMediaType == MEDIA_TYPE_MOVIE) {
                        mediaData = MovieData.parseDataMapToMediaData(document.getData());

                    } else {
                        mediaData = SeriesData.parseDataMapToMediaData(document.getData());
                    }

                    mediaDataList.add(mediaData);
                }

                //TODO media data list always contains media data type only
                //noinspection unchecked
                listLiveData.setValue(mediaDataList);
            }
        });
    }

    public LiveData getHomeDisplayList() {
        return listLiveData;
    }
}
