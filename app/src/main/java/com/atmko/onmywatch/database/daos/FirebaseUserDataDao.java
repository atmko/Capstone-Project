/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.Backup;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/*
 * MovieData firebase Dao
 */

public class FirebaseUserDataDao {
    private static final String ADMIN_COLLECTION_TITLE = "admin";
    public static final String FEATURE_PERMISSIONS_TITLE = "feature_permissions";

    private static final String BACKUPS_PATH = "backups";
    private static final String BACKUP_COUNTER_KEY = "backup_counter";
    public static final String MIGRATION_KEY = "migration";
    public static final String MIGRATION_TO_LOCAL = "to_local";
    public static final String MIGRATION_LOCAL = "local";
    public static final String MIGRATION_TO_CLOUD = "to_cloud";
    public static final String MIGRATION_CLOUD = "cloud";

    public static DocumentReference getFeaturePermissions() {
        return MasterActivity.getUserDbHomeReference()
                .collection(ADMIN_COLLECTION_TITLE)
                .document(FEATURE_PERMISSIONS_TITLE);
    }

    public static DocumentReference getMigrationValue() {
        return MasterActivity.getUserDbHomeReference();
    }

    public static void setMigrationValue(String migrationValue) {
        MasterActivity.getUserDbHomeReference()
                .update(MIGRATION_KEY, migrationValue)
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

    public static MutableLiveData<List<Backup>> getBackups() {
        final MutableLiveData<List<Backup>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(BACKUPS_PATH)
                .orderBy(Backup.TIMESTAMP_KEY, Query.Direction.DESCENDING);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<Backup> backups = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        Backup backup = parseDataMapToBackup(document);

                        backups.add(backup);
                    }

                    liveData.setValue(backups);

                } else {
                    liveData.setValue(backups);
                }
            }
        });

        return liveData;
    }

    public static void addBackupAlt(Backup backup) {
        MasterActivity.getUserDbHomeReference()
                .collection(BACKUPS_PATH).document(backup.getFileName())
                .set(backup.parseBackupToDataMap());
    }

    public static void setBackupCounter(int backupCounter) {
        Map<String, Integer> map = new HashMap<>();
        map.put(BACKUP_COUNTER_KEY, backupCounter);
        MasterActivity.getUserDbHomeReference()
                .set(map, SetOptions.merge())
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

    public static Integer getBackupCounterAlt() {
        Task<DocumentSnapshot> documentSnapshotTask = MasterActivity.getUserDbHomeReference().get();
        try {
            DocumentSnapshot documentSnapshot = Tasks.await(documentSnapshotTask);
            if (documentSnapshot != null) {
                Long counterLong = ((Long) documentSnapshot.get(BACKUP_COUNTER_KEY));
                if (counterLong != null) {
                    return counterLong.intValue();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Backup parseDataMapToBackup(DocumentSnapshot document) {
        Long timestampLong = (Long) document.get(Backup.TIMESTAMP_KEY);
        String filename = document.getId();
        long timestamp = timestampLong != null? timestampLong : 0;

        return new Backup(filename, timestamp);
    }
}