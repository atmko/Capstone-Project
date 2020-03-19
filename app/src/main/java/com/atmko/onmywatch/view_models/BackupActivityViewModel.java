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

public class BackupActivityViewModel extends AndroidViewModel {
    private static final String TAG = BackupActivityViewModel.class.getSimpleName();

    private MutableLiveData<List<Backup>> backupsLiveData;

    public BackupActivityViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "fetching user data from the database");

        backupsLiveData = FirebaseUserDataDao.getBackups();
    }

    public LiveData<List<Backup>> getBackupsLiveData() {
        return backupsLiveData;
    }
}