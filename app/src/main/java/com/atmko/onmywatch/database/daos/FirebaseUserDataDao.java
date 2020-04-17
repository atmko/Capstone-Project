/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.Backup;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/*
 * User data firebase Dao
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

        StorageReference reference = FirebaseStorage.getInstance()
                .getReference()
                .child("users")
                .child(MasterActivity.getCurrentUser().getUid())
                .child("backups");
        reference.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        final List<StorageReference> storageReferences = listResult.getItems();

                        final List<Backup> backups = new ArrayList<>();
                        for (int i = 0; i < storageReferences.size(); i++) {
                            final StorageReference prefix = storageReferences.get(i);
                            final int finalI = i;
                            prefix.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                @Override
                                public void onSuccess(StorageMetadata storageMetadata) {
                                    backups.add(new Backup(prefix.getName(), storageMetadata.getUpdatedTimeMillis()));
                                    if (finalI == storageReferences.size() -1) {
                                        sortBackUps(backups);
                                        liveData.setValue(backups);
                                    }
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Uh-oh, an error occurred!
                        liveData.setValue(new ArrayList<Backup>());
                    }
                });

        return liveData;
    }

    private static void sortBackUps(final List<Backup> backups) {
        Comparator<Backup> comparator = new Comparator<Backup>() {
            @Override
            public int compare(Backup backup1, Backup backup2) {
                //noinspection UseCompareMethod
                if (backup1.mTimestamp < backup2.mTimestamp) {

                    return 1;
                } else if (backup1.mTimestamp > backup2.mTimestamp) {
                    return -1;
                } else {
                    return 0;
                }
            }
        };
        Collections.sort(backups, comparator);
    }

    private static List<Backup> getBackupsAlt() {
        List<Backup> backups = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(BACKUPS_PATH)
                .orderBy(Backup.TIMESTAMP_KEY, Query.Direction.DESCENDING)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                Backup backup = parseDataMapToBackup(documentSnapshot);
                backups.add(backup);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return backups;
    }

    public static Backup getLatestBackupAlt() {
        List<Backup> backups = FirebaseUserDataDao.getBackupsAlt();
        if (backups.size() != 0) {
            return backups.get(0);
        } else {
            return null;
        }
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