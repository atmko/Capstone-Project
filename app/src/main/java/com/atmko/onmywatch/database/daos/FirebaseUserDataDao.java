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

    private static final String BACKUP_COUNTER_KEY = "backup_counter";
    public static final String MIGRATION_KEY = "migration";

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

    public static List<Backup> getBackupsAlt() {
        final List<Backup> backups = new ArrayList<>();

        StorageReference reference = FirebaseStorage.getInstance()
                .getReference()
                .child("users")
                .child(MasterActivity.getCurrentUser().getUid())
                .child("backups");
        try {
            ListResult listResult = Tasks.await(reference.listAll());

            final List<StorageReference> storageReferences = listResult.getItems();

            for (int i = 0; i < storageReferences.size(); i++) {
                final StorageReference prefix = storageReferences.get(i);

                StorageMetadata storageMetadata = Tasks.await(prefix.getMetadata());

                backups.add(new Backup(prefix.getName(), storageMetadata.getUpdatedTimeMillis()));
                if (i == storageReferences.size() -1) {
                    sortBackUps(backups);
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return backups;
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
}