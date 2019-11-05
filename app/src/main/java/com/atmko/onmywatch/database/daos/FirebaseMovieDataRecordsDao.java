/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.firebase.firestore.Query;

import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;
import static com.atmko.onmywatch.utils.network_utils.ApiConstants.ID_KEY;

/*
 * MovieDataRecords firebase Dao
 */

public class FirebaseMovieDataRecordsDao {

    private static final String MOVIE_DATA_RECORDS_COLLECTION_PATH = "movie_data_records";

    public static Query getAllListsContainingMedia(String mediaId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, mediaId);
    }

    public static Query getAllMoviesInList(String listName) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, listName);
    }
}