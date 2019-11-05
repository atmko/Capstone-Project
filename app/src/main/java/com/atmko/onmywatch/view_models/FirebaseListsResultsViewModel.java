/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.atmko.onmywatch.Fragments.ListsWatchAndUserParentFragment;
import com.atmko.onmywatch.database.daos.FirebaseMovieDataDao;
import com.atmko.onmywatch.database.daos.FirebaseMovieDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataRecordsDao;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/*
 * View model for ListResultsFragment and HomeListDisplayFragment(watch lists) firebase Dao
 */

public class FirebaseListsResultsViewModel extends ViewModel {
    private static final String TAG = FirebaseListsResultsViewModel.class.getSimpleName();

    private MutableLiveData<List<MovieData>> allMoviesInWatchList, allMoviesInUserList;
    private MutableLiveData<List<SeriesData>> allSeriesInWatchList, allSeriesInUserList;

    FirebaseListsResultsViewModel(int listType, List<String> watchStatusTitleList, String listName) {
        allMoviesInWatchList = new MutableLiveData<>();
        allSeriesInWatchList = new MutableLiveData<>();
        allMoviesInUserList = new MutableLiveData<>();
        allSeriesInUserList = new MutableLiveData<>();

        if (listType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            Log.d(TAG, "fetching media in watch list");
            fetchMoviesInWatchList(watchStatusTitleList.indexOf(listName));
            fetchSeriesInWatchList(watchStatusTitleList.indexOf(listName));

        } else if (listType == ListsWatchAndUserParentFragment.LIST_TYPE_USER){
            Log.d(TAG, "fetching media in user list");
            fetchMoviesInUserList(listName);
            fetchSeriesInUserList(listName);

        }
    }

    private void fetchMoviesInUserList(String listName) {
        FirebaseMovieDataRecordsDao.getAllMoviesInList(listName)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                if (snapshots.getDocuments().size() != 0) {
                    final List<MovieData> containingMediaData = new ArrayList<>();

                    List<DocumentSnapshot> containingMediaDataDocuments = snapshots.getDocuments();

                    for (DocumentSnapshot documentSnapshot : containingMediaDataDocuments) {
                        String mediaId = documentSnapshot.getString(ApiConstants.ID_KEY);

                        FirebaseMovieDataDao.getMovieById(mediaId).get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (!task.isSuccessful()) {
                                    //TODO: make message when error occurs
                                }

                                if (task.getResult() != null) {
                                    List<DocumentSnapshot> documentSnapshots =
                                            task.getResult().getDocuments();

                                    for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                                        //TODO: mediaDataMap null check already done by getting by id and checking getDocuments != 0
                                        //noinspection ConstantConditions
                                        MovieData movieData =
                                                MovieData.parseDataMapToMediaData(documentSnapshot.getData());
                                        containingMediaData.add(movieData);

                                    }

                                    //set containing lists
                                    allMoviesInUserList.setValue(containingMediaData);

                                } else {
                                    allMoviesInUserList.setValue(null);

                                }
                            }
                        });
                    }
                }
            }
        });
    }

    private void fetchSeriesInUserList(String listName) {
        FirebaseSeriesDataRecordsDao.getAllSeriesInList(listName)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        //TODO: make message when error occurs
                        if (e != null) return;
                        if (snapshots == null) return;

                        final List<SeriesData> containingMediaData = new ArrayList<>();
                        if (snapshots.getDocuments().size() != 0) {

                            List<DocumentSnapshot> containingMediaDataDocuments = snapshots.getDocuments();

                            for (DocumentSnapshot documentSnapshot : containingMediaDataDocuments) {
                                String mediaId = documentSnapshot.getString(ApiConstants.ID_KEY);

                                FirebaseSeriesDataDao.getSeriesById(mediaId).get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (!task.isSuccessful()) {
                                            //TODO: make message when error occurs
                                        }

                                        if (task.getResult() != null) {
                                            List<DocumentSnapshot> documentSnapshots =
                                                    task.getResult().getDocuments();

                                            for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                                                //TODO: mediaDataMap null check already done by getting by id and checking getDocuments != 0
                                                //noinspection ConstantConditions
                                                SeriesData seriesData =
                                                        SeriesData.parseDataMapToMediaData(documentSnapshot.getData());
                                                containingMediaData.add(seriesData);

                                            }

                                            //set containing lists
                                            allSeriesInUserList.setValue(containingMediaData);

                                        } else {
                                            allSeriesInUserList.setValue(null);

                                        }
                                    }
                                });
                            }
                        } else {
                            allSeriesInUserList.setValue(containingMediaData);

                        }
                    }
                });
    }

    private void fetchMoviesInWatchList(int watchStatus) {
        FirebaseMovieDataDao.getMoviesByWatchStatus(watchStatus)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        //TODO: make message when error occurs
                        if (e != null) return;
                        if (snapshots == null) return;

                        final List<MovieData> containingMediaData = new ArrayList<>();

                        if (snapshots.getDocuments().size() != 0) {
                            List<DocumentSnapshot> containingMediaDataDocuments = snapshots.getDocuments();

                            for (DocumentSnapshot documentSnapshot: containingMediaDataDocuments) {
                                //TODO: mediaDataMap null check already done by getting by id and checking getDocuments != 0
                                //noinspection ConstantConditions
                                MovieData movieData =
                                        MovieData.parseDataMapToMediaData(documentSnapshot.getData());
                                containingMediaData.add(movieData);

                            }
                        }

                        //set containing lists
                        allMoviesInWatchList.setValue(containingMediaData);
                    }
                });
    }

    private void fetchSeriesInWatchList(int watchStatus) {
        FirebaseSeriesDataDao.getSeriesByWatchStatus(watchStatus)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        //TODO: make message when error occurs
                        if (e != null) return;
                        if (snapshots == null) return;

                        final List<SeriesData> containingMediaData = new ArrayList<>();

                        if (snapshots.getDocuments().size() != 0) {
                            List<DocumentSnapshot> containingMediaDataDocuments = snapshots.getDocuments();

                            for (DocumentSnapshot documentSnapshot: containingMediaDataDocuments) {
                                //TODO: mediaDataMap null check already done by getting by id and checking getDocuments != 0
                                //noinspection ConstantConditions
                                SeriesData seriesData =
                                        SeriesData.parseDataMapToMediaData(documentSnapshot.getData());
                                containingMediaData.add(seriesData);

                            }
                        }

                        //set containing lists
                        allSeriesInWatchList.setValue(containingMediaData);
                    }
                });
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
