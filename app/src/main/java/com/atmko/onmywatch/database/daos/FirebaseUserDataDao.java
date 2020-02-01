/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.NonNull;

import com.atmko.onmywatch.MasterActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;

/*
 * MovieData firebase Dao
 */

public class FirebaseUserDataDao {
    private static final String ADMIN_COLLECTION_TITLE = "admin";
    public static final String FEATURE_PERMISSIONS_TITLE = "feature_permissions";

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
}
