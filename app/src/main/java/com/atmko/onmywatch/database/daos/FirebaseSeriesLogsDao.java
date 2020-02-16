/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.SeriesLog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;/*
 * UserList firebase Dao
 */
import java.util.Map;

public class FirebaseSeriesLogsDao implements SeriesLogsDao{
    public static final String SERIES_LOGS_COLLECTION_PATH = "series_logs";

    @Override
    public void addMediaLog(SeriesLog mediaLog) {

    }

    //TODO: remove update code from pro migrations since set method can handle create and updates
    public static void addLogBatch(List<Map<String, Object>> seriesLogMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> seriesLogMap: seriesLogMapList) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(SERIES_LOGS_COLLECTION_PATH)
                    .document();

            batch.set(documentReference, seriesLogMap);
        }

        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    @Override
    public List<SeriesLog> getAllLogsAlt() {
        return null;
    }

    @Override
    public List<SeriesLog> getAllLogsWithMediaIdAlt(String parentId) {
        return null;
    }

    @Override
    public LiveData<List<SeriesLog>> getUpcoming() {
        return null;
    }

    @Override
    public LiveData<List<SeriesLog>> getAired() {
        return null;
    }

    @Override
    public LiveData<List<SeriesLog>> getUndated() {
        return null;
    }

    @Override
    public void updateMediaLog(SeriesLog mediaLog) {

    }

    @Override
    public void deleteMediaLog(SeriesLog mediaLog) {

    }
}
