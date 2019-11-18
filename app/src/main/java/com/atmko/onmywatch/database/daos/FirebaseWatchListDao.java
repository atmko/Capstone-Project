/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;
import java.util.Map;

/*
 * WatchList firebase Dao
 */

public class FirebaseWatchListDao {

    private static final String WATCH_LISTS_PATH = "watch_lists";

    public static Task<Void> addWatchListBatch(List<Map<String, Object>> watchListMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> watchListMap: watchListMaps) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(WATCH_LISTS_PATH)
                    .document();

            batch.set(documentReference, watchListMap);
        }

        return batch.commit();
    }

    public static CollectionReference getAllWatchLists() {
        return MasterActivity.getUserDbHomeReference()
                .collection(WATCH_LISTS_PATH);
    }

    public static Task<Void> updateWatchListBatch(List<String> batchDocumentIds, List<Map<String,
            Object>> watchListMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (int i = 0; i < batchDocumentIds.size(); i++) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(WATCH_LISTS_PATH)
                    .document(batchDocumentIds.get(i));

            batch.update(documentReference, watchListMaps.get(i));
        }

        return batch.commit();
    }
}
