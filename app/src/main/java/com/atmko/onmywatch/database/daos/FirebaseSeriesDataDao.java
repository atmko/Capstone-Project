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
import static com.atmko.onmywatch.models.SeriesData.NEXT_EPISODE_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.ID_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.RELEASE_STATUS_KEY;

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

    public static Task<Void> addSeriesDataBatch(List<Map<String, Object>> seriesDataMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> seriesDataMap: seriesDataMapList) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(SERIES_COLLECTION_PATH)
                    .document();

            batch.set(documentReference, seriesDataMap);
        }

        return batch.commit();
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

    public static Query getUserUpcomingEpisodes() {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereGreaterThan(NEXT_EPISODE_KEY, 0)
                .whereEqualTo(WATCH_STATUS_KEY, 2)
                .orderBy(NEXT_EPISODE_KEY, Query.Direction.ASCENDING)
                .limit(10);
    }

    public static Query getUndatedSeries() {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereGreaterThan(WATCH_STATUS_KEY, 0)
                .whereLessThan(WATCH_STATUS_KEY, 3)
                .whereEqualTo(NEXT_EPISODE_KEY, 0)
                .whereIn(RELEASE_STATUS_KEY, Arrays.asList("Planned", "In Production", "Pilot"))
                .limit(10);
    }

    public static Query getEndedSeries() {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereGreaterThan(WATCH_STATUS_KEY, 0)
                .whereLessThan(WATCH_STATUS_KEY, 3)
                .whereIn(RELEASE_STATUS_KEY, Arrays.asList("Canceled", "Ended"))
                .orderBy(WATCH_STATUS_KEY, Query.Direction.DESCENDING)
                .limit(10);
    }

    public static Task<Void> updateSeriesData(String documentId, Map<String, Object> seriesDataMap) {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .document(documentId)
                .update(seriesDataMap);
    }

    public static Task<Void> updateSeriesDataBatch(List<String> batchDocumentIds, List<Map<String,
            Object>> seriesDataMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (int i = 0; i < batchDocumentIds.size(); i++) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(SERIES_COLLECTION_PATH)
                    .document(batchDocumentIds.get(i));

            batch.update(documentReference, seriesDataMapList.get(i));
        }

        return batch.commit();
    }

    public static Task<Void> deleteSeriesData(String documentId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .document(documentId)
                .delete();
    }
}
