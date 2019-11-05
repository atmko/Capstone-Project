/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.firebase.firestore.Query;

import static com.atmko.onmywatch.models.MediaData.WATCH_STATUS_KEY;
import static com.atmko.onmywatch.utils.network_utils.ApiConstants.ID_KEY;

/*
 * MovieData firebase Dao
 */

public class FirebaseMovieDataDao {

    private static final String MOVIES_COLLECTION_PATH = "movies";

    public static Query getMovieById(String mediaId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, mediaId);
    }

    public static Query getMoviesByWatchStatus(int watchStatus) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereEqualTo(WATCH_STATUS_KEY, watchStatus);
    }
}
