/*
 * Copyright (C) 2019 Aayat Mimiko
 */

/*view model class to persist firebase data*/
package com.atmko.onmywatch.view_models;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.atmko.onmywatch.database.daos.FirebaseMovieDataDao;
import com.atmko.onmywatch.database.daos.FirebaseMovieDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseUserListDao;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.UserListModel;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.models.ListModel.ITEM_COUNT_KEY;
import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;

public class FirebaseAddToListViewModel extends ViewModel {
    private static final String TAG = FirebaseAddToListViewModel.class.getSimpleName();

    private MutableLiveData<Integer> watchStatusLiveData;
    private MutableLiveData<List<UserListModel>> allUserListsLiveData;
    private MutableLiveData<List<UserListModel>> containingUserListsLiveData;

    FirebaseAddToListViewModel(int mediaType, String mediaId) {
        watchStatusLiveData = new MutableLiveData<>();
        allUserListsLiveData = new MutableLiveData<>();
        containingUserListsLiveData = new MutableLiveData<>();

        //fetch firestore data for watch status
        fetchWatchStatus(mediaType, mediaId);

        //fetch firestore data for media containing lists
        fetchContainingLists(mediaType, mediaId);

        //fetch firestore data for all user lists
        fetchAllUserLists();
    }

    private void fetchWatchStatus(int mediaType, String mediaId) {
        //get media data from the database (to get watch status)
        Query watchStatusListener;

        //TODO: consider moving type determination into dao
        if (mediaType == MEDIA_TYPE_MOVIE) {
            //check if movie exists in db
            watchStatusListener = FirebaseMovieDataDao.getMovieById(mediaId);

        } else {
            //check if series exists in db
            watchStatusListener = FirebaseSeriesDataDao.getSeriesById(mediaId);

        }

        watchStatusListener.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                //TODO: adding a check for value greater than one can help clean up duplicate database records
                if (snapshots.getDocuments().size() != 0) {
                    DocumentSnapshot mediaDataDocument =
                            snapshots.getDocuments().get(0);

                    Map<String, Object> mediaDataMap = mediaDataDocument.getData();

                    //set watch status
                    //TODO: mediaDataMap null check already done by getting by id and checking getDocuments != 0
                    //noinspection ConstantConditions
                    watchStatusLiveData.setValue(
                            ((Long) mediaDataMap.get(MediaData.WATCH_STATUS_KEY)).intValue());

                } else {
                    watchStatusLiveData.setValue(null);
                }
            }
        });
    }

    private void fetchContainingLists(int mediaType, String mediaId) {
        //get record of lists containing media
        Query containingListsListener;

        if (mediaType == MEDIA_TYPE_MOVIE) {
            //check if movie exists in db
            containingListsListener = FirebaseMovieDataRecordsDao.getAllListsContainingMedia(mediaId);

        } else {
            //check if series exists in db
            containingListsListener = FirebaseSeriesDataRecordsDao.getAllListsContainingMedia(mediaId);

        }

        containingListsListener.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                if (snapshots.getDocuments().size() != 0) {
                    final List<UserListModel> containingLists = new ArrayList<>();

                    List<DocumentSnapshot> containingListsDocuments = snapshots.getDocuments();

                    for (DocumentSnapshot documentSnapshot: containingListsDocuments) {
                        String listName = ((String) documentSnapshot.get(LIST_NAME_KEY));
                        containingLists.add(UserListModel.parseUserListModel(listName));

                    }

                    //set containing lists
                    containingUserListsLiveData.setValue(containingLists);

                } else {
                    containingUserListsLiveData.setValue(null);

                }
            }
        });
    }

    private void fetchAllUserLists() {
        Log.d(TAG, "fetching all user lists from the database");

        CollectionReference userListsCollectionReference = FirebaseUserListDao.getAllUserLists();

        //get all user lists from the database
        userListsCollectionReference.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                final List<UserListModel> userLists = new ArrayList<>();

                for (DocumentSnapshot document : snapshots.getDocuments()) {
                    UserListModel userList = ((UserListModel) UserListModel.parseUserListModel(document));
                    userLists.add(userList);
                }

                allUserListsLiveData.setValue(userLists);
            }
        });
    }

    public LiveData<Integer> getWatchStatus() {
        return watchStatusLiveData;
    }

    public LiveData<List<UserListModel>> getContainingLists() {
        return containingUserListsLiveData;
    }

    public LiveData<List<UserListModel>> getAllUserLists() {
        return allUserListsLiveData;
    }
}
