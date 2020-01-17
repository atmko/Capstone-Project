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

import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.models.MediaNotifier.NOTIFIER_ID_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.ID_KEY;

/*
 * SeriesData firebase Dao
 */

public class FirebaseSeriesNotifiersDao {

    private static final String SERIES_NOTIFIERS_COLLECTION_PATH = "series_notifiers";

    public static Task<Void> addSeriesNotifierBatch(List<Map<String, Object>> seriesDataMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> seriesDataMap: seriesDataMapList) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(SERIES_NOTIFIERS_COLLECTION_PATH)
                    .document();

            batch.set(documentReference, seriesDataMap);
        }

        return batch.commit();
    }

    public static Task<QuerySnapshot> getAllNotifiers() {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_NOTIFIERS_COLLECTION_PATH)
                .get();
    }

    public static Query getNotifiersWithMediaId(String mediaId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_NOTIFIERS_COLLECTION_PATH)
                .whereEqualTo(NOTIFIER_ID_KEY, mediaId);
    }

    public static Query getSeriesById(String mediaId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_NOTIFIERS_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, mediaId);
    }
}
