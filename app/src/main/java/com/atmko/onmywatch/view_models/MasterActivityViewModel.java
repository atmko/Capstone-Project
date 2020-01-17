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

public class MasterActivityViewModel extends AndroidViewModel {
    private static final String TAG = MasterActivityViewModel.class.getSimpleName();

    public static final String USER_TIER_PRO = "pro";
    public static final String USER_TIER_FREE = "free";
    private static final String USER_TIER_KEY = "user_tier";

    private MutableLiveData<String> userTierLiveData = new MutableLiveData<>();

    public MasterActivityViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "fetching user data from the database");

        FirebaseUserDataDao.getUserTier().addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
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

                    userTierLiveData.setValue(userTier);

                } catch (FirebaseFirestoreException ex) {
                    ex.printStackTrace();

                } catch (DataBaseIntegrityException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public LiveData<String> getUserTierLiveData() {
        return userTierLiveData;
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