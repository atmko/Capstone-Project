/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

import static com.atmko.onmywatch.models.MediaData.WATCH_STATUS_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.ID_KEY;

/*
 * SeriesData firebase Dao
 */

public class FirebaseSeriesDataDao {

    private static final String SERIES_COLLECTION_PATH = "series";

    public static Task<DocumentReference> addSeriesData(Map<String, Object> seriesDataMap) {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .add(seriesDataMap);
    }

    public static Task<QuerySnapshot> getAllSeries() {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .get();
    }

    public static Query getSeriesById(String seriesId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, seriesId);
    }

    public static Query getSeriesByWatchStatus(int watchStatus) {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereEqualTo(WATCH_STATUS_KEY, watchStatus);
    }

    public static Task<Void> updateSeriesData(String documentId, Map<String, Object> seriesDataMap) {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .document(documentId)
                .update(seriesDataMap);
    }

    public static Task<Void> deleteSeriesData(String documentId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .document(documentId)
                .delete();
    }
}
