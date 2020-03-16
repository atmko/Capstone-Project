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

@SuppressWarnings("unchecked")
public class LogoutService extends JobIntentService {
    private static final String TAG = com.atmko.onmywatch.utils.network_utils.work_manager_workers.BackupWorker.class.getSimpleName();

    public static final int JOB_ID = 11;

    public static final String ACTION_LOG_OUT = "log_out";

    private static final String BACKUP_CHANNEL_ID = "Backup Channel";

    private static final String WORKING_DATA_FOLDER_NAME = "working_data";
    private static final String WORKING_DATA_FILE_NAME = "working_data";

    private String mAction;
    private static OnLogOutBackupCompleteListener mBackupCompleteListener;

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

    public interface OnLogOutBackupCompleteListener {
        void onLogOutBackupComplete();
        void onLogOutBackupFailure();
    }

    public static void enqueueWork(Context appContext, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(ACTION_LOG_OUT)) {
            mBackupCompleteListener = ((OnLogOutBackupCompleteListener) appContext);
        }
        enqueueWork(appContext, LogoutService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        mAction = intent.getAction();
        BackupLogic backupLogic = new BackupLogic(getApplicationContext(), WORKING_DATA_FOLDER_NAME,
                WORKING_DATA_FILE_NAME);
        boolean backupSuccess = backupLogic.backupToRemoteDatabase();
        if (backupSuccess) {
            Log.d(TAG, "backup success");
            if (mAction.equals(ACTION_LOG_OUT)) {
                mBackupCompleteListener.onLogOutBackupComplete();
            }
        } else {
            Log.d(TAG, "backup failure");
            mBackupCompleteListener.onLogOutBackupFailure();
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