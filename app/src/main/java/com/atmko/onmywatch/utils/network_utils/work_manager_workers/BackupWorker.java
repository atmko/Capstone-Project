/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils.work_manager_workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.daos.FirebaseUserDataDao;
import com.atmko.onmywatch.utils.network_utils.BackupLogic;

public class BackupWorker extends Worker {
    private static final String TAG = BackupWorker.class.getSimpleName();

    private static final String BACKUP_CHANNEL_ID = "Backup Channel";

    public static final String BACKUP_FOLDER_NAME = "backups";
    public static final String BACKUP_FILE_NAME = "backup";

    private BackupLogic mBackupLogic;

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (MasterActivity.getCurrentUser() == null) {
            Log.d(TAG, "backup failure");
            return Result.failure();
        }

        int backupCounter = BackupLogic.getBackupCounter();
        String fileName = BackupWorker.BACKUP_FILE_NAME + "_" + backupCounter;

        mBackupLogic = new BackupLogic(getApplicationContext(),
                BackupWorker.BACKUP_FOLDER_NAME, fileName);
        boolean backupSuccess = mBackupLogic.backupToRemoteDatabase();
        if (backupSuccess) {
            FirebaseUserDataDao.setBackupCounter(backupCounter);

            Log.d(TAG, "backup success");
            return Result.success();

        } else {
            Log.d(TAG, "backup failure");
            return Result.failure();
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        mBackupLogic.deleteLocalFile();
    }

    public static void createBackupNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.notification_channel_backup);
            String description = context.getString(R.string.notification_channel_backup_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(BACKUP_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) notificationManager.createNotificationChannel(channel);
        }
    }
}