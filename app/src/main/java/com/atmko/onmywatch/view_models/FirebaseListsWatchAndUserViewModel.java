/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.database.daos.FirebaseUserListDao;
import com.atmko.onmywatch.database.daos.FirebaseWatchListDao;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import static com.atmko.onmywatch.models.ListModel.ITEM_COUNT_KEY;
import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;

public class FirebaseListsWatchAndUserViewModel extends AndroidViewModel {
    private static final String TAG = FirebaseListsWatchAndUserViewModel.class.getSimpleName();

    private final MutableLiveData<List<UserListModel>> allUserListsLiveData;
    private final MutableLiveData<List<WatchListModel>> watchListsLiveData;

    public FirebaseListsWatchAndUserViewModel(@NonNull Application application) {
        super(application);

        Log.d(TAG, "fetching lists from the database");

        allUserListsLiveData = new MutableLiveData<>();
        watchListsLiveData = new MutableLiveData<>();

        fetchAllUserLists();
        fetchAllWatchLists();
    }

    private void fetchAllUserLists() {
        Log.d(TAG, "fetching all user lists from the database");
        //get all user lists from the database
        FirebaseUserListDao.getAllUserLists().addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                final List<UserListModel> userLists = new ArrayList<>();

                for (DocumentSnapshot document : snapshots.getDocuments()) {
                    UserListModel userList = ((UserListModel) parseUserListModel(document));
                    userLists.add(userList);
                }

                allUserListsLiveData.setValue(userLists);
            }
        });
    }

    private void fetchAllWatchLists() {
        Log.d(TAG, "fetching all watch lists from the database");
        //get all watch lists from the database
        FirebaseWatchListDao.getAllWatchLists().addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                final List<WatchListModel> watchLists = new ArrayList<>();

                for (DocumentSnapshot document : snapshots.getDocuments()) {
                    WatchListModel watchList = ((WatchListModel) parseWatchListModel(document));
                    watchLists.add(watchList);
                }

                watchListsLiveData.setValue(watchLists);
            }
        });
    }

    private ListModel parseUserListModel(DocumentSnapshot document) {
        String listName = document.getString(LIST_NAME_KEY);
        int listCount = ((Long) document.get(ITEM_COUNT_KEY)).intValue();

        UserListModel userListModel = new UserListModel(listName, listCount);
        userListModel.setDocumentId(document.getId());

        return userListModel;
    }

    private ListModel parseWatchListModel(DocumentSnapshot document) {
        String listName = document.getString(LIST_NAME_KEY);
        int listCount = ((Long) document.get(ITEM_COUNT_KEY)).intValue();

        WatchListModel watchListModel = new WatchListModel(listName, listCount);
        watchListModel.setDocumentId(document.getId());

        return watchListModel;
    }

    public LiveData<List<WatchListModel>> getWatchLists() {
        return watchListsLiveData;
    }

    public LiveData<List<UserListModel>> getUserLists() {
        return allUserListsLiveData;
    }
}
