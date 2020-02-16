/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.MediaLog;
import com.atmko.onmywatch.models.SeriesLog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;/*
 * UserList firebase Dao
 */
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FirebaseSeriesLogsDao implements SeriesLogsDao{
    private static final String SERIES_LOGS_COLLECTION_PATH = "series_logs";

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
        List<SeriesLog> movieList = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_LOGS_COLLECTION_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                SeriesLog log = parseDataMapToMediaLog(documentSnapshot);
                movieList.add(log);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return movieList;    }

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
    public void deleteMediaLog(SeriesLog mediaLog) {

    }

    private static SeriesLog parseDataMapToMediaLog(DocumentSnapshot document) {
        return new SeriesLog(
                (String) document.get(MediaLog.TYPE_KEY),
                ((int) document.get(SeriesLog.SEASON_NUMBER_KEY)),
                (int) document.get(SeriesLog.EPISODE_NUMBER_KEY),
                (int) document.get(MediaLog.CONDITION_KEY),
                (long) document.get(MediaLog.TIMESTAMP_KEY),
                (String) document.get(MediaLog.TITLE_KEY),
                (String) document.get(MediaLog.POSTER_PATH_KEY),
                (String) document.get(MediaLog.PARENT_ID_KEY),
                (boolean) document.get(SeriesLog.IS_BUNDLED_KEY)
        );
    }
}
