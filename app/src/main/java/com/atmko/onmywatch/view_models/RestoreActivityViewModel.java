/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.view_models;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.database.daos.FirebaseUserDataDao;
import com.atmko.onmywatch.models.Backup;

import java.util.List;

public class RestoreActivityViewModel extends AndroidViewModel {
    private static final String TAG = RestoreActivityViewModel.class.getSimpleName();

    private MutableLiveData<List<Backup>> backupsLiveData;

    public RestoreActivityViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "fetching user data from the database");

        backupsLiveData = FirebaseUserDataDao.getBackups();
    }

    public LiveData<List<Backup>> getBackupsLiveData() {
        return backupsLiveData;
    }
}