/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.models.MediaData.WATCH_STATUS_KEY;
import static com.atmko.onmywatch.models.MovieData.SCHEDULED_MEDIA_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.ID_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.RELEASE_STATUS_KEY;

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

    public static Task<Void> addMovieDataBatch(List<Map<String, Object>> movieDataMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> movieDataMap: movieDataMapList) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(MOVIES_COLLECTION_PATH)
                    .document();

            batch.set(documentReference, movieDataMap);
        }

        return batch.commit();
    }

    public static Task<QuerySnapshot> getAllMovies() {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .get();
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

    public static Query getUserUpcomingMovies() {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereGreaterThan(WATCH_STATUS_KEY, 0)
                .whereLessThan(WATCH_STATUS_KEY, 3)
                .whereIn(RELEASE_STATUS_KEY, Arrays.asList("Rumored", "Planned", "In Production", "Post Production"))
                .orderBy(WATCH_STATUS_KEY, Query.Direction.DESCENDING)
                .orderBy(SCHEDULED_MEDIA_KEY, Query.Direction.ASCENDING)
                .limit(10);
    }

    public static Query getUndatedMovies() {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereGreaterThan(WATCH_STATUS_KEY, 0)
                .whereLessThan(WATCH_STATUS_KEY, 3)
                .whereEqualTo(SCHEDULED_MEDIA_KEY, 0)
                .whereIn(RELEASE_STATUS_KEY, Arrays.asList("Rumored", "Planned", "In Production", "Post Production"))
                .limit(10);
    }

    public static Query getReleasedMovies() {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereGreaterThan(WATCH_STATUS_KEY, 0)
                .whereLessThan(WATCH_STATUS_KEY, 3)
                .whereEqualTo(RELEASE_STATUS_KEY, "Released")
                .limit(10);
    }

    public static Task<Void> updateMovieData(String documentId, Map<String, Object> movieDataMap) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .document(documentId)
                .update(movieDataMap);
    }

    public static Task<Void> updateMovieDataBatch(List<String> batchDocumentIds, List<Map<String,
            Object>> movieDataMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (int i = 0; i < batchDocumentIds.size(); i++) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(MOVIES_COLLECTION_PATH)
                    .document(batchDocumentIds.get(i));

            batch.update(documentReference, movieDataMapList.get(i));
        }

        return batch.commit();
    }

    public static Task<Void> deleteMovieData(String documentId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .document(documentId)
                .delete();
    }
}
