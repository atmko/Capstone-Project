/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.database.daos.FirebaseUserDataDao;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;

import static com.atmko.onmywatch.database.daos.FirebaseUserDataDao.MIGRATION_KEY;
import static com.atmko.onmywatch.database.daos.FirebaseUserDataDao.FEATURE_PERMISSIONS_TITLE;

public class MasterActivityViewModel extends AndroidViewModel {
    private static final String TAG = MasterActivityViewModel.class.getSimpleName();

    private static final String PRO_MODE_KEY = "pro_mode";
    private static final String ALLOW_CLOUD_BACKUP_KEY = "allow_cloud_backup";

    private MutableLiveData<Boolean> isProModeLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> allowCloudBackupLiveData  = new MutableLiveData<>();
    private MutableLiveData<String> migrationLiveData = new MutableLiveData<>();

    public MasterActivityViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "fetching user data from the database");

        FirebaseUserDataDao.getFeaturePermissions().addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                try {
                    //throw exception if firebase error exists
                    if (e != null) throw e;
                    //throw exception if snapshot is null or if doesn't exist
                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        throw new DataBaseIntegrityException(TAG,
                                DataBaseIntegrityException.FEATURE_PERMISSIONS_NONEXISTENT);
                    }

                    Boolean isProMode = ((Boolean) documentSnapshot.get(PRO_MODE_KEY));
                    Boolean allowCloudBackup = ((Boolean) documentSnapshot.get(ALLOW_CLOUD_BACKUP_KEY));

                    //throw exception if user tier is null
                    if (isProMode == null) {
                        throw new DataBaseIntegrityException(TAG,
                                DataBaseIntegrityException.PRO_MODE_KEY_NONEXISTENT);
                    }

                    //throw exception if user tier is null
                    if (allowCloudBackup == null) {
                        throw new DataBaseIntegrityException(TAG,
                                DataBaseIntegrityException.ALLOW_CLOUD_BACKUP_KEY_NONEXISTENT);
                    }

                    isProModeLiveData.setValue(isProMode);
                    allowCloudBackupLiveData.setValue(allowCloudBackup);

                } catch (FirebaseFirestoreException ex) {
                    ex.printStackTrace();

                } catch (DataBaseIntegrityException ex) {
                    ex.printStackTrace();
                }
            }
        });

        FirebaseUserDataDao.getMigrationValue().addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                try {
                    //throw exception if firebase error exists
                    if (e != null) throw e;
                    //throw exception if snapshot is null or if doesn't exist
                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        throw new DataBaseIntegrityException(TAG,
                                DataBaseIntegrityException.USER_DATA_NONEXISTENT);
                    }

                    String migration = ((String) documentSnapshot.get(MIGRATION_KEY));

                    //throw exception if user tier is null
                    if (migration == null) {
                        throw new DataBaseIntegrityException(TAG,
                                DataBaseIntegrityException.MIGRATION_KEY_NONEXISTENT);
                    }

                    migrationLiveData.setValue(migration);

                } catch (FirebaseFirestoreException ex) {
                    ex.printStackTrace();

                } catch (DataBaseIntegrityException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public LiveData<Boolean> getIsProModeLiveData() {
        return isProModeLiveData;
    }

    private static class DataBaseIntegrityException extends Exception {
        final static Integer FEATURE_PERMISSIONS_NONEXISTENT = 0;
        final static Integer PRO_MODE_KEY_NONEXISTENT = 1;
        final static Integer ALLOW_CLOUD_BACKUP_KEY_NONEXISTENT = 2;

        final static Integer USER_DATA_NONEXISTENT = 3;
        final static Integer MIGRATION_KEY_NONEXISTENT = 4;

        DataBaseIntegrityException(String TAG, int errorCondition) {
            if (errorCondition == FEATURE_PERMISSIONS_NONEXISTENT) {
                Log.d(TAG, FEATURE_PERMISSIONS_TITLE + " key does not exist in database");

            } else  if (errorCondition == PRO_MODE_KEY_NONEXISTENT) {
                Log.d(TAG, PRO_MODE_KEY + " key does not exist in database");

            } else  if (errorCondition == USER_DATA_NONEXISTENT) {
                Log.d(TAG, "user data does not exist in database");

            } else  if (errorCondition == ALLOW_CLOUD_BACKUP_KEY_NONEXISTENT) {
                Log.d(TAG, ALLOW_CLOUD_BACKUP_KEY + " key does not exist in database");

            } else if (errorCondition == MIGRATION_KEY_NONEXISTENT) {
                Log.d(TAG, MIGRATION_KEY + " key does not exist in database");
            }
        }
    }
}