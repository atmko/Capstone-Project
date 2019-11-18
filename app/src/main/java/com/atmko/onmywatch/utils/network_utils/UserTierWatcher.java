/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atmko.onmywatch.MasterActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;

import static com.atmko.onmywatch.utils.network_utils.FreeModeMigrationService.ACTION_USER_TIER_TO_FREE;
import static com.atmko.onmywatch.utils.network_utils.ProModeMigrationService.ACTION_USER_TIER_TO_PRO;

public class UserTierWatcher {
    private static final String TAG = UserTierWatcher.class.getSimpleName();

    private static final String USER_TIER_PRO = "pro";
    private static final String USER_TIER_FREE = "free";
    private static final String USER_TIER_KEY = "user_tier";

    //watch firebase user_tier value and migrate data to_pro / to_free accordingly
    public static void watch(@NonNull final Context context) {
        //check for user tier
        MasterActivity.getUserDbHomeReference()
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        try {
                            //throw exception if firebase error exists
                            if (e != null) throw e;
                            //throw exception if snapshot is null or if doesn't exist
                            if (documentSnapshot == null || !documentSnapshot.exists()) {
                                throw new DataBaseIntegrityException(TAG,
                                        DataBaseIntegrityException.USER_TIER_NONEXISTENT);
                            }

                            String userTier = ((String) documentSnapshot.get(USER_TIER_KEY));

                            //throw exception if user tier is null
                            if (userTier == null) {
                                throw new DataBaseIntegrityException(TAG,
                                        DataBaseIntegrityException.USER_TIER_NONEXISTENT);
                            }

                            //if user tier is not transitioning "to_pro"/"to_free", do nothing
                            if (userTier.equals(USER_TIER_FREE) || userTier.equals(USER_TIER_PRO)) return;

                            Intent userTierMigrationIntent = new Intent(context, ProModeMigrationService.class);

                            //start migration foreground service for appropriate user tier
                            if (userTier.equals(ACTION_USER_TIER_TO_PRO)) {
                                userTierMigrationIntent.setAction(ACTION_USER_TIER_TO_PRO);

                                context.startForegroundService(userTierMigrationIntent);
                                ProModeMigrationService.enqueueWork(context, userTierMigrationIntent);

                            } else if (userTier.equals(ACTION_USER_TIER_TO_FREE)) {
                                userTierMigrationIntent.setAction(ACTION_USER_TIER_TO_FREE);

                                context.startForegroundService(userTierMigrationIntent);
                                FreeModeMigrationService.enqueueWork(context, userTierMigrationIntent);
                            }

                        } catch (FirebaseFirestoreException ex) {
                            ex.printStackTrace();

                        } catch (DataBaseIntegrityException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
    }

    private static class DataBaseIntegrityException extends Exception {
        final static Integer USER_TIER_NONEXISTENT = 0;

        DataBaseIntegrityException(String TAG, int errorCondition) {
            if (errorCondition == USER_TIER_NONEXISTENT) {
                Log.d(TAG, USER_TIER_KEY + " does not exist in database");

            }
        }
    }
}
