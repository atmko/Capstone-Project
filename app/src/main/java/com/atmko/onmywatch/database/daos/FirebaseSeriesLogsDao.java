/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.MediaLog;
import com.atmko.onmywatch.models.SeriesLog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
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
        DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_LOGS_COLLECTION_PATH)
                .document();

        mediaLog.setUniqueExternalId(documentReference.getId());

        documentReference.set(mediaLog.parseLogToDataMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
        });
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
        List<SeriesLog> seriesLogs = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_LOGS_COLLECTION_PATH)
                .whereEqualTo(MediaLog.PARENT_ID_KEY, parentId)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                SeriesLog mediaData = parseDataMapToMediaLog(documentSnapshot);
                seriesLogs.add(mediaData);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return seriesLogs;    }

    @Override
    public LiveData<List<SeriesLog>> getUpcoming() {
        final MutableLiveData<List<SeriesLog>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_LOGS_COLLECTION_PATH)
                .whereEqualTo(MediaLog.CONDITION_KEY, MediaLog.CONDITION_UPCOMING)
                .orderBy(MediaLog.TIMESTAMP_KEY, Query.Direction.ASCENDING)
                .limit(10);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<SeriesLog> seriesLogs = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        SeriesLog seriesLog = parseDataMapToMediaLog(document);

                        seriesLogs.add(seriesLog);
                    }

                    liveData.setValue(seriesLogs);

                } else {
                    liveData.setValue(seriesLogs);
                }
            }
        });

        return liveData;
    }

    @Override
    public LiveData<List<SeriesLog>> getAired() {
        final MutableLiveData<List<SeriesLog>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_LOGS_COLLECTION_PATH)
                .whereEqualTo(MediaLog.CONDITION_KEY, MediaLog.CONDITION_AIRED)
                .orderBy(MediaLog.TIMESTAMP_KEY, Query.Direction.DESCENDING)
                .limit(10);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<SeriesLog> seriesLogs = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        SeriesLog seriesLog = parseDataMapToMediaLog(document);

                        seriesLogs.add(seriesLog);
                    }

                    liveData.setValue(seriesLogs);

                } else {
                    liveData.setValue(seriesLogs);
                }
            }
        });

        return liveData;
    }

    @Override
    public LiveData<List<SeriesLog>> getUndated() {
        final MutableLiveData<List<SeriesLog>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_LOGS_COLLECTION_PATH)
                .whereEqualTo(MediaLog.CONDITION_KEY, MediaLog.CONDITION_UNDATED)
                .limit(10);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<SeriesLog> seriesLogs = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        SeriesLog seriesLog = parseDataMapToMediaLog(document);

                        seriesLogs.add(seriesLog);
                    }

                    liveData.setValue(seriesLogs);

                } else {
                    liveData.setValue(seriesLogs);
                }
            }
        });

        return liveData;
    }

    @Override
    public void deleteMediaLog(SeriesLog mediaLog) {
        MasterActivity.getUserDbHomeReference()
                .collection(SERIES_LOGS_COLLECTION_PATH)
                .document(mediaLog.getUniqueExternalId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private static SeriesLog parseDataMapToMediaLog(DocumentSnapshot document) {
        Long seasonNumberLong = (Long) document.get(SeriesLog.SEASON_NUMBER_KEY);
        Long episodeNumberLong = (Long) document.get(SeriesLog.EPISODE_NUMBER_KEY);
        Long conditionLong = (Long) document.get(SeriesLog.CONDITION_KEY);
        Long timestampLong = (Long) document.get(SeriesLog.TIMESTAMP_KEY);
        int seasonNumber = seasonNumberLong != null? seasonNumberLong.intValue(): 0;
        int episodeNumber = episodeNumberLong != null? episodeNumberLong.intValue(): 0;
        int condition = conditionLong != null? conditionLong.intValue(): 0;
        int timestamp = timestampLong != null? timestampLong.intValue(): 0;

        SeriesLog seriesLog = new SeriesLog(
                (String) document.get(MediaLog.TYPE_KEY),
                seasonNumber,
                episodeNumber,
                condition,
                timestamp,
                (String) document.get(MediaLog.TITLE_KEY),
                (String) document.get(MediaLog.POSTER_PATH_KEY),
                (String) document.get(MediaLog.PARENT_ID_KEY),
                (boolean) document.get(SeriesLog.IS_BUNDLED_KEY)
        );

        seriesLog.setUniqueExternalId(document.getId());

        return seriesLog;
    }
}
