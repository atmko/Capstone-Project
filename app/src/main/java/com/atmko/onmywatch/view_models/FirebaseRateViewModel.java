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
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;

/*
 * Firebase View model implementation for RateActivity
 */

public class FirebaseRateViewModel extends ViewModel {
    private static final String TAG = FirebaseRateViewModel.class.getSimpleName();

    private MutableLiveData mediaDataLiveData;
    private String documentId;

    FirebaseRateViewModel(int mediaType, String mediaId) {
        Log.d(TAG, "fetching media from the database");
        Log.d(TAG, "fetching user containing lists from the database");

        mediaDataLiveData = new MutableLiveData();

        //fetch media data from firestore
        fetchMediaData(mediaType, mediaId);
    }

    private void fetchMediaData(final int mediaType, String mediaId) {
        //get media data from the database (to get watch status)
        Query mediaDataListener;

        //TODO: consider moving type determination into dao
        if (mediaType == MEDIA_TYPE_MOVIE) {
            //check if movie exists in db
            mediaDataListener = FirebaseMovieDataDao.getMovieById(mediaId);

        } else {
            //check if series exists in db
            mediaDataListener = FirebaseSeriesDataDao.getSeriesById(mediaId);

        }

        mediaDataListener.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                //TODO: adding a check for value greater than one can help clean up duplicate database records
                if (snapshots.getDocuments().size() != 0) {
                    DocumentSnapshot mediaDataDocument =
                            snapshots.getDocuments().get(0);

                    documentId = mediaDataDocument.getId();
                    Map<String, Object> mediaDataMap = mediaDataDocument.getData();

                    //set watch status
                    //TODO: mediaDataMap null check already done by getting by id and checking getDocuments != 0
                    if (mediaType == MEDIA_TYPE_MOVIE) {
                        //noinspection ConstantConditions
                        mediaDataLiveData.setValue(MovieData.parseDataMapToMediaData(mediaDataMap));

                    } else {
                        //noinspection ConstantConditions
                        mediaDataLiveData.setValue(SeriesData.parseDataMapToMediaData(mediaDataMap));
                    }

                } else {
                    mediaDataLiveData.setValue(null);
                }
            }
        });
    }

    public LiveData getMediaData () {
        return mediaDataLiveData;
    }

    public String getDocumentId() {
        return documentId;
    }
}
