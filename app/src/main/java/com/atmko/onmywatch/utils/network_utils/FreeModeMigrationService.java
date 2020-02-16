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
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.database.daos.FirebaseUserDataDao;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.SeriesLog;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.List;

import static com.atmko.onmywatch.database.daos.FirebaseUserDataDao.MIGRATION_LOCAL;
import static com.atmko.onmywatch.database.daos.FirebaseUserDataDao.MIGRATION_TO_LOCAL;

public class FreeModeMigrationService extends JobIntentService {
    private static final String TAG = FreeModeMigrationService.class.getSimpleName();

    public static final int JOB_ID = 11;

    //notification channel ids
    public static final String MIGRATION_CHANNEL_ID = "Migration Channel";

    private AppDatabase mLocalDatabase;
    private AppDatabase mRemoteDatabase;

    public FreeModeMigrationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLocalDatabase = AppDatabase.getLocalDatabase(getApplicationContext());
        mRemoteDatabase = AppDatabase.getRemoteDatabase();

        startForeground(JOB_ID,
                buildNotification(getApplicationContext(),
                        getString(R.string.notification_migration_title),
                        getString(R.string.notification_free_migration_content)
                )
        );
    }

    public static void enqueueWork(Context appContext, Intent intent) {
        enqueueWork(appContext, FreeModeMigrationService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String intentAction = intent.getAction();

        try {
            if (intentAction == null) throw new NullPointerException("no intent action specified");

            if (intentAction.equals(MIGRATION_TO_LOCAL)) {
                Log.d(TAG, "migrating to free");
                migrateToLocalDatabase();
            }

        } catch (NullPointerException e) {
            e.printStackTrace();

        }
    }

    public Notification buildNotification(Context context, String notificationTitle, String notificationContent) {
        //create intent to launch activity on click
        Intent intent = new Intent(context, MasterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        return new NotificationCompat.Builder(context, MIGRATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_rate)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }

    private void migrateToLocalDatabase() {
        //other methods run one after the other starting with pullMovieData
        pullMovieData();
    }

    public void onPullMovieDataComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pullSeriesData();
            }
        });
    }

    public void onPullSeriesDataComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pullWatchLists();
            }
        });
    }

    public void onPullWatchListsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pullUserLists();
            }
        });
    }

    public void onPullUserListsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pullMovieDataRecords();
            }
        });
    }

    public void onPullMovieDataRecordsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pullSeriesDataRecords();
            }
        });
    }

    public void onPullSeriesDataRecordsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pullMovieNotifiers();
            }
        });
    }

    public void onPullMovieNotifiersComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pullSeriesNotifiers();
            }
        });
    }

    public void onPullSeriesNotifiersComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pullSeriesLogs();
            }
        });
    }

    public void onPullSeriesLogsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                deleteRemotelySavedData();
                FirebaseUserDataDao.setMigrationValue(MIGRATION_LOCAL);
                finishService();
            }
        });
    }

    private void finishService() {
        stopForeground(true);
        stopSelf();
    }

    //pull remotely saved movies to local database
    private void pullMovieData() {
        //get locally saved movies
        //get remotely saved movies
        //iterate through remote movieDataSnapshots
        //if local movie data list does not contain remote movie data: add movie data to local database

        //get locally saved movies
        final List<MovieData> localMovieDataList = mLocalDatabase.movieDataDao().getAllMoviesAlt();
        //get remotely saved movies
        List<MovieData> remoteMovies = mRemoteDatabase.movieDataDao().getAllMoviesAlt();

        for (MovieData movieData: remoteMovies) {
            //if local movie data list does not contain remote movie data: add movie data to local database
            if (!localMovieDataList.contains(movieData)) {
                mLocalDatabase.movieDataDao().addMovieData(movieData);
            }
        }

        onPullMovieDataComplete();
    }

    //pull remotely saved series to local database
    private void pullSeriesData() {
        //get locally saved series
        //get remotely saved series
        //iterate through remote seriesDataSnapshots
        //if local series data list does not contain remote series data: add series data to local database

        //get locally saved series
        final List<SeriesData> localSeriesDataList = mLocalDatabase.seriesDataDao().getAllSeriesAlt();
        //get remotely saved series
        List<SeriesData> remoteSeries = mRemoteDatabase.seriesDataDao().getAllSeriesAlt();
        for (SeriesData seriesData: remoteSeries) {
            //if local series data list does not contain remote series data: add series data to local database
            if (!localSeriesDataList.contains(seriesData)) {
                mLocalDatabase.seriesDataDao().addSeriesData(seriesData);
            }
        }

        onPullSeriesDataComplete();
    }

    //pull remotely saved watch lists to local database
    private void pullWatchLists() {
        //get locally saved watch lists
        //get remotely saved watch lists
        //iterate through remote watchListSnapshots
        //if local watch lists do not contain remote watch lists: add watch list to local database

        //get locally saved watch lists
        final List<WatchListModel> localWatchLists = mLocalDatabase.watchListsDao().getAllListsAlt();
        //get remotely saved watch lists
        List<WatchListModel> remoteLists = mRemoteDatabase.watchListsDao().getAllListsAlt();

        //iterate through remote watchListSnapshots
        //if local watch lists do not contain remote watch list: add watch list to local database
        for (WatchListModel watchListModel: remoteLists) {
            //if local watch lists do not contain remote watch lists: add watch list to local database
            if (!localWatchLists.contains(watchListModel)) {
                mLocalDatabase.watchListsDao().addList(watchListModel);
            }
        }

        onPullWatchListsComplete();
    }

    //pull remotely saved user lists to local database
    private void pullUserLists() {
        //get locally saved user lists
        //get remotely saved user lists
        //iterate through remote userListSnapshots
        //if local user lists do not contain remote user lists: add user list to local database

        //get locally saved user lists
        final List<UserListModel> localUserLists = mLocalDatabase.userListsDao().getAllListsAlt();
        //get remotely saved user lists
        List<UserListModel> remoteLists = mRemoteDatabase.userListsDao().getAllListsAlt();

        //iterate through remote userListSnapshots
        //if local user lists do not contain remote user list: add user list to local database
        for (UserListModel userListModel: remoteLists) {
            //if local user lists do not contain remote user lists: add user list to local database
            if (!localUserLists.contains(userListModel)) {
                mLocalDatabase.userListsDao().addList(userListModel);
            }
        }

        onPullUserListsComplete();
    }

    //pull remotely saved movie data records to local database
    private void pullMovieDataRecords() {
        //get locally saved movie data records
        //get remotely saved movie data records
        //iterate through remote movieDataRecordSnapshots
        //if local movie data records do not contain remote movie data records: add movie data record to local database

        //get locally saved movie data records
        final List<MovieDataRecord> localMovieDataRecords = mLocalDatabase.movieDataRecordsDao().getAllRecordsAlt();
        //get remotely saved movie data records
        List<MovieDataRecord> remoteDataRecords = mRemoteDatabase.movieDataRecordsDao().getAllRecordsAlt();

        //iterate through remote movieDataRecordSnapshots
        //if local movie data records do not contain remote movie data records: add movie data record to local database
        for (MovieDataRecord movieDataRecord: remoteDataRecords) {
            //if local user lists do not contain remote user lists: add user list to local database
            if (!localMovieDataRecords.contains(movieDataRecord)) {
                mLocalDatabase.movieDataRecordsDao().addRecord(movieDataRecord);
            }
        }

        onPullMovieDataRecordsComplete();
    }

    //pull remotely saved series data records to local database
    private void pullSeriesDataRecords() {
        //get locally saved series data records
        //get remotely saved series data records
        //iterate through remote seriesDataRecordSnapshots
        //if local series data records do not contain remote series data records: add series data record to local database

        //get locally saved series data records
        final List<SeriesDataRecord> localSeriesDataRecords = mLocalDatabase.seriesDataRecordsDao().getAllRecordsAlt();
        //get remotely saved series data records
        List<SeriesDataRecord> remoteDataRecords = mRemoteDatabase.seriesDataRecordsDao().getAllRecordsAlt();

        //iterate through remote seriesDataRecordSnapshots
        //if local series data records do not contain remote series data records: add series data record to local database
        for (SeriesDataRecord seriesDataRecord: remoteDataRecords) {
            //if local user lists do not contain remote user lists: add user list to local database
            if (!localSeriesDataRecords.contains(seriesDataRecord)) {
                mLocalDatabase.seriesDataRecordsDao().addRecord(seriesDataRecord);
            }
        }

        onPullSeriesDataRecordsComplete();
    }

    //pull remotely saved movie notifiers to local database
    private void pullMovieNotifiers() {
        //get locally saved movie notifiers
        //get remotely saved movie notifiers
        //iterate through remote movieNotifierSnapshots
        //if local movie data records do not contain remote movie data records: add movie data record to local database

        //get locally saved movie data records
        final List<MovieNotifier> localMovieNotifiers = mLocalDatabase.movieNotifierDao().getAllNotifiersAlt();
        //get remotely saved movie data records
        List<MovieNotifier> remoteNotifiers = mRemoteDatabase.movieNotifierDao().getAllNotifiersAlt();

        //iterate through remote movieNotifierSnapshots
        //if local movie data records do not contain remote movie data records: add movie data record to local database
        for (MovieNotifier movieNotifier: remoteNotifiers) {
            //if local user lists do not contain remote user lists: add user list to local database
            if (!localMovieNotifiers.contains(movieNotifier)) {
                mLocalDatabase.movieNotifierDao().addMediaNotifier(movieNotifier);
            }
        }

        onPullMovieNotifiersComplete();
    }

    //pull remotely saved series notifiers to local database
    private void pullSeriesNotifiers() {
        //get locally saved series notifiers
        //get remotely saved series notifiers
        //iterate through remote seriesNotifierSnapshots
        //if local series data records do not contain remote series data records: add series data record to local database

        //get locally saved series data records
        final List<SeriesNotifier> localSeriesNotifiers = mLocalDatabase.seriesNotifierDao().getAllNotifiersAlt();
        //get remotely saved series data records
        List<SeriesNotifier> remoteNotifiers = mRemoteDatabase.seriesNotifierDao().getAllNotifiersAlt();

        //iterate through remote seriesNotifierSnapshots
        //if local series data records do not contain remote series data records: add series data record to local database
        for (SeriesNotifier seriesNotifier: remoteNotifiers) {
            //if local user lists do not contain remote user lists: add user list to local database
            if (!localSeriesNotifiers.contains(seriesNotifier)) {
                mLocalDatabase.seriesNotifierDao().addMediaNotifier(seriesNotifier);
            }
        }

        onPullSeriesNotifiersComplete();
    }

    //pull remotely saved series logs to local database
    private void pullSeriesLogs() {
        //get locally saved series logs
        //get remotely saved series logs
        //iterate through remote seriesLogSnapshots
        //if local series data records do not contain remote series logs: add series log to local database

        //get locally saved series logs
        final List<SeriesLog> localSeriesLogs = mLocalDatabase.seriesLogsDao().getAllLogsAlt();
        //get remotely saved series logs
        List<SeriesLog> remoteLogs = mRemoteDatabase.seriesLogsDao().getAllLogsAlt();

        //iterate through remote seriesLogSnapshots
        //if local series logs do not contain remote series logs: add series log to local database
        for (SeriesLog seriesLog: remoteLogs) {
            //if local user lists do not contain remote user lists: add user list to local database
            if (!localSeriesLogs.contains(seriesLog)) {
                mLocalDatabase.seriesLogsDao().addMediaLog(seriesLog);
            }
        }

        onPullSeriesLogsComplete();
    }

    private void deleteRemotelySavedData() {
        FirebaseFunctions.getInstance().getHttpsCallable("deleteUserData").call()
                .addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
                    @Override
                    public void onSuccess(HttpsCallableResult httpsCallableResult) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }
}
