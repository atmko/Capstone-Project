/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.firebase.firestore.Query;

import static com.atmko.onmywatch.CreateListActivity.LIST_NAME_KEY;

/*
 * MovieDataRecords firebase Dao
 */

public class FirebaseMovieDataRecordsDao {

    private static final String MOVIE_DATA_RECORDS_COLLECTION_PATH = "movie_data_records";

    public static Query getAllMoviesInList(String listName) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, listName);
    }
}