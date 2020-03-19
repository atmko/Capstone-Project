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
    private static final String TAG = com.atmko.onmywatch.utils.network_utils.work_manager_workers.BackupWorker.class.getSimpleName();

    public static final int JOB_ID = 11;

    public static final String ACTION_WORKING_DATA = "working_data";
    public static final String ACTION_BACKUP = "backup";

    private static final String BACKUP_CHANNEL_ID = "Backup Channel";

    private static final String WORKING_DATA_FOLDER_NAME = "working_data";
    private static final String WORKING_DATA_FILE_NAME = "working_data";

    private static OnBackupCompleteListener mBackupCompleteListener;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(JOB_ID,
                buildNotification(getApplicationContext(),
                        getString(R.string.notification_migration_title),
                        getString(R.string.notification_free_migration_content)
                )
        );
    }

    public interface OnBackupCompleteListener {
        void onLogOutBackupComplete();
        void onBackupComplete();
        void onBackupFailure();
    }

    public static void enqueueWork(Context appContext, Intent intent) {
        mBackupCompleteListener = ((OnBackupCompleteListener) appContext);
        enqueueWork(appContext, BackupService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        int backupCounter = BackupLogic.getBackupCounter();
        String folderName;
        String fileName;
        String mAction = intent.getAction();
        if (mAction != null) {
            if (mAction.equals(ACTION_WORKING_DATA)) {
                folderName = WORKING_DATA_FOLDER_NAME;
                fileName = WORKING_DATA_FILE_NAME;

            } else if (mAction.equals(ACTION_BACKUP)) {
                folderName = BackupWorker.BACKUP_FOLDER_NAME;
                fileName = BackupWorker.BACKUP_FILE_NAME + "_" + backupCounter;
            } else {
                return;
            }
        } else {
            return;
        }

        BackupLogic backupLogic = new BackupLogic(getApplicationContext(), folderName, fileName);
        boolean backupSuccess = backupLogic.backupToRemoteDatabase();
        if (backupSuccess) {
            //only add update backup counter in remote database if saving backups and not working data
            if (folderName.equals(BackupWorker.BACKUP_FOLDER_NAME)) {
                FirebaseUserDataDao.setBackupCounter(backupCounter);
            }

            Log.d(TAG, "backup success");
            mBackupCompleteListener.onBackupComplete();
            mBackupCompleteListener.onLogOutBackupComplete();

        } else {
            Log.d(TAG, "backup failure");
            mBackupCompleteListener.onBackupFailure();
        }
    }

    public Notification buildNotification(Context context, String notificationTitle, String notificationContent) {
        //create intent to launch activity on click
        Intent intent = new Intent(context, MasterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        return new NotificationCompat.Builder(context, BACKUP_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify_black)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }
}