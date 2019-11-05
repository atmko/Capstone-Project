/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;

import java.util.Map;

import static com.atmko.onmywatch.models.MediaData.WATCH_STATUS_KEY;
import static com.atmko.onmywatch.utils.network_utils.ApiConstants.ID_KEY;

/*
 * MovieData firebase Dao
 */

public class FirebaseMovieDataDao {

    private static final String MOVIES_COLLECTION_PATH = "movies";

    public static Task<DocumentReference> addMovieData(Map<String, Object> movieDataMap) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .add(movieDataMap);
    }

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

    public static Task<Void> updateMovieData(String documentId, Map<String, Object> movieDataMap) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .document(documentId)
                .update(movieDataMap);
    }

    public static Task<Void> deleteMovieData(String documentId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .document(documentId)
                .delete();
    }
}
