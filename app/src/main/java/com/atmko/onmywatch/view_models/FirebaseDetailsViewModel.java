/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.atmko.onmywatch.database.daos.FirebaseMovieDataDao;
import com.atmko.onmywatch.database.daos.FirebaseMovieDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseMovieNotifiersDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesNotifiersDao;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.UserListModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;

//TODO: generics used for fluidity
@SuppressWarnings("unchecked")
public class FirebaseDetailsViewModel extends ViewModel {
    private static final String TAG = FirebaseDetailsViewModel.class.getSimpleName();

    private MutableLiveData mediaDataLiveData = new MutableLiveData();
    private MutableLiveData<List<String>> containingUserListsLiveData = new MutableLiveData<>();
    private MutableLiveData notifiers = new MutableLiveData<>();

    FirebaseDetailsViewModel(int mediaType, String mediaId) {
        Log.d(TAG, "fetching media from the database");
        Log.d(TAG, "fetching user containing lists from the database");
        Log.d(TAG, "fetching notifiers from the database");

        //get media data from the database (to get watch status)
        Query mediaDataQuery;
        //get record of lists containing media
        Query containingListsListener;
        //get record of lists containing media
        Query mediaNotifierQuery;

        if (mediaType == MEDIA_TYPE_MOVIE) {
            mediaDataQuery = FirebaseMovieDataDao.getMovieById(mediaId);
            containingListsListener = FirebaseMovieDataRecordsDao.getAllListsContainingMedia(mediaId);
            mediaNotifierQuery = FirebaseMovieNotifiersDao.getNotifiersWithMediaId(mediaId);

        } else {
            mediaDataQuery = FirebaseSeriesDataDao.getSeriesById(mediaId);
            containingListsListener = FirebaseSeriesDataRecordsDao.getAllListsContainingMedia(mediaId);
            mediaNotifierQuery = FirebaseSeriesNotifiersDao.getNotifiersWithMediaId(mediaId);
        }

        //fetch media data from firestore
        fetchMediaData(mediaType, mediaDataQuery);

        //fetch firestore data for media containing lists
        fetchContainingLists(containingListsListener);

        //fetch notifiers from firestore
        fetchNotifiers(mediaType, mediaNotifierQuery);
    }

    private void fetchMediaData(final int mediaType, Query mediaDataQuery) {
        mediaDataQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                MediaData mediaData = null;

                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                //TODO: adding a check for value greater than one can help clean up duplicate database records
                if (snapshots.getDocuments().size() != 0) {
                    DocumentSnapshot mediaDataDocument =
                            snapshots.getDocuments().get(0);

                    Map<String, Object> mediaDataMap = mediaDataDocument.getData();

                    if (mediaDataMap == null) return;

                    //set watch status
                    //TODO: mediaDataMap null check already done by getting by id and checking getDocuments != 0
                    if (mediaType == MEDIA_TYPE_MOVIE) {
                        mediaData = MovieData.parseDataMapToMediaData(mediaDataMap);
                        mediaDataLiveData.setValue(mediaData);

                    } else {
                        mediaData = SeriesData.parseDataMapToMediaData(mediaDataMap);
                        mediaDataLiveData.setValue(mediaData);
                    }
                }

                mediaDataLiveData.setValue(mediaData);
            }
        });
    }

    private void fetchContainingLists(Query containingListsListener) {
        containingListsListener.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<UserListModel> containingLists = new ArrayList<>();

                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                if (snapshots.getDocuments().size() != 0) {

                    List<DocumentSnapshot> containingListsDocuments = snapshots.getDocuments();

                    for (DocumentSnapshot documentSnapshot: containingListsDocuments) {
                        String listName = ((String) documentSnapshot.get(LIST_NAME_KEY));
                        containingLists.add(parseUserListModel(listName));
                    }
                }

                containingUserListsLiveData.setValue(UserListModel.getContainingListsNames(containingLists));
            }
        });
    }

    private void fetchNotifiers(final int mediaType, Query mediaDataNotifierQuery) {
        mediaDataNotifierQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MediaNotifier> mediaNotifiers = new ArrayList<>();

                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                if (snapshots.getDocuments().size() != 0) {
                    List<DocumentSnapshot> notifierDocuments = snapshots.getDocuments();

                    for (DocumentSnapshot documentSnapshot: notifierDocuments) {
                        if (mediaType == MEDIA_TYPE_MOVIE) {
                            mediaNotifiers.add(MovieNotifier.parseMediaNotifier(documentSnapshot));

                        } else {
                            mediaNotifiers.add(SeriesNotifier.parseMediaNotifier(documentSnapshot));
                        }
                    }
                }

                //set notifiers
                notifiers.setValue(mediaNotifiers);
            }
        });
    }

    private UserListModel parseUserListModel(String listName) {
        return new UserListModel(listName);
    }

    public LiveData getMediaData() {
        return mediaDataLiveData;
    }

    public LiveData<List<String>> getContainingLists() {
        return containingUserListsLiveData;
    }

    public LiveData<List<MediaNotifier>> getNotifiers() {
        return notifiers;
    }
}
