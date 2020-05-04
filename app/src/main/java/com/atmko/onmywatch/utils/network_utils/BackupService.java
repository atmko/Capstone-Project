/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.daos.FirebaseUserDataDao;
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.BackupWorker;

public class BackupService extends JobIntentService {
    private static final String TAG = BackupService.class.getSimpleName();

    private static final int JOB_ID = 11;

    private static final String BACKUP_CHANNEL_ID = "Backup Channel";

    private static OnBackupCompleteListener mBackupCompleteListener;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(JOB_ID,
                buildNotification(getApplicationContext(),
                        getString(R.string.notification_backup_title),
                        getString(R.string.notification_backup_content)
                )
        );
    }

    public interface OnBackupCompleteListener {
        void onLogOutBackupComplete();
        void onBackupComplete();
        void onBackupFailure(String error);
    }

    public static void enqueueWork(Context appContext, Intent intent) {
        mBackupCompleteListener = ((OnBackupCompleteListener) appContext);
        enqueueWork(appContext, BackupService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (MasterActivity.getCurrentUser() == null) {
            Log.d(TAG, "backup failure");
            return;
        }

        int backupCounter = BackupLogic.getBackupCounter();
        String fileName = BackupWorker.BACKUP_FILE_NAME + "_" + backupCounter;

        BackupLogic backupLogic = new BackupLogic(getApplicationContext(),
                BackupWorker.BACKUP_FOLDER_NAME, fileName);
        boolean backupSuccess = backupLogic.backupToRemoteDatabase();
        if (backupSuccess) {
            FirebaseUserDataDao.setBackupCounter(backupCounter);

            Log.d(TAG, "backup success");
            mBackupCompleteListener.onBackupComplete();
            mBackupCompleteListener.onLogOutBackupComplete();

        } else {
            Log.d(TAG, "backup failure");
            mBackupCompleteListener.onBackupFailure(backupLogic.getErrorMessage());
        }
    }

    private Notification buildNotification(Context context, String notificationTitle, String notificationContent) {
        //create intent to launch activity on click
        Intent intent = new Intent(context, MasterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        return new NotificationCompat.Builder(context, BACKUP_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }
}