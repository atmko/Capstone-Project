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
import com.atmko.onmywatch.database.daos.FirebaseMovieDataDao;
import com.atmko.onmywatch.database.daos.FirebaseMovieDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseUserListDao;
import com.atmko.onmywatch.database.daos.FirebaseWatchListDao;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.List;

public class FreeModeMigrationService extends JobIntentService {
    private static final String TAG = FreeModeMigrationService.class.getSimpleName();

    public static final int JOB_ID = 11;

    //notification channel ids
    public static final String MIGRATION_CHANNEL_ID = "Migration Channel";

    public static final String ACTION_USER_TIER_TO_FREE = "to_free";

    private AppDatabase mDatabase;

    public FreeModeMigrationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mDatabase = AppDatabase.getInstance(getApplicationContext());

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

            if (intentAction.equals(ACTION_USER_TIER_TO_FREE)) {
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
                deleteRemotelySavedData();
                stopService();
            }
        });
    }

    private void  stopService() {
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
        final List<MovieData> localMovieDataList = mDatabase.movieDataDao().getAllMoviesAlt();

        //get remotely saved movies
        FirebaseMovieDataDao.getAllMovies().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(final QuerySnapshot movieDataSnapshots) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        //iterate through remote movieDataSnapshots
                        //if local movie data list does not contain remote movie data: add movie data to local database
                        for (DocumentSnapshot movieDataDocument: movieDataSnapshots.getDocuments()) {
                            if (movieDataDocument.getData() == null) continue;

                            final MovieData remoteMovieData =
                                    MovieData.parseDataMapToMediaData(movieDataDocument.getData());

                            //if local movie data list does not contain remote movie data: add movie data to local database
                            if (!localMovieDataList.contains(remoteMovieData)) {
                                mDatabase.movieDataDao().addMovieData(remoteMovieData);
                            }
                        }

                        onPullMovieDataComplete();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                
            }
        });
    }

    //pull remotely saved series to local database
    private void pullSeriesData() {
        //get locally saved series
        //get remotely saved series
        //iterate through remote seriesDataSnapshots
        //if local series data list does not contain remote series data: add series data to local database

        //get locally saved series
        final List<SeriesData> localSeriesDataList = mDatabase.seriesDataDao().getAllSeriesAlt();

        //get remotely saved series
        FirebaseSeriesDataDao.getAllSeries().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(final QuerySnapshot seriesDataSnapshots) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        //iterate through remote seriesDataSnapshots
                        //if local series data list does not contain remote series data: add series data to local database
                        for (DocumentSnapshot seriesDataDocument: seriesDataSnapshots.getDocuments()) {
                            if (seriesDataDocument.getData() == null) continue;

                            final SeriesData remoteSeriesData =
                                    SeriesData.parseDataMapToMediaData(seriesDataDocument.getData());

                            //if local series data list does not contain remote series data: add series data to local database
                            if (!localSeriesDataList.contains(remoteSeriesData)) {
                                //TODO: CONTINUATION check if null is given by api or if mode by app
                                //TODO: if by app fix the issue; if my api make typesafe catch when parsing
                                Log.d(TAG, remoteSeriesData.getTitle());
                                mDatabase.seriesDataDao().addSeriesData(remoteSeriesData);
                            }
                        }

                        onPullSeriesDataComplete();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    //pull remotely saved watch lists to local database
    private void pullWatchLists() {
        //get locally saved watch lists
        //get remotely saved watch lists
        //iterate through remote watchListSnapshots
        //if local watch lists do not contain remote watch lists: add watch list to local database

        //get locally saved watch lists
        final List<WatchListModel> localWatchLists = mDatabase.watchListsDao().getAllListsAlt();

        //get remotely saved watch lists
        FirebaseWatchListDao.getAllWatchLists().get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(final QuerySnapshot watchListSnapshots) {
                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                            @Override
                            public void run() {
                                //iterate through remote watchListSnapshots
                                //if local watch lists do not contain remote watch list: add watch list to local database
                                for (DocumentSnapshot watchListDocument: watchListSnapshots.getDocuments()) {
                                    if (watchListDocument.getData() == null) continue;

                                    final WatchListModel remoteWatchList =
                                            WatchListModel.parseWatchListModel(watchListDocument);

                                    //if local watch lists do not contain remote watch lists: add watch list to local database
                                    if (!localWatchLists.contains(remoteWatchList)) {
                                        mDatabase.watchListsDao().addList(remoteWatchList);
                                    }
                                }

                                onPullWatchListsComplete();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

            }
        });
    }

    //pull remotely saved user lists to local database
    private void pullUserLists() {
        //get locally saved user lists
        //get remotely saved user lists
        //iterate through remote userListSnapshots
        //if local user lists do not contain remote user lists: add user list to local database

        //get locally saved user lists
        final List<UserListModel> localUserLists = mDatabase.userListsDao().getAllListsAlt();

        //get remotely saved user lists
        FirebaseUserListDao.getAllUserLists().get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(final QuerySnapshot userListSnapshots) {
                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                            @Override
                            public void run() {
                                //iterate through remote userListSnapshots
                                //if local user lists do not contain remote user list: add user list to local database
                                for (DocumentSnapshot userListDocument: userListSnapshots.getDocuments()) {
                                    if (userListDocument.getData() == null) continue;

                                    final UserListModel remoteUserList =
                                            UserListModel.parseUserListModel(userListDocument);

                                    //if local user lists do not contain remote user lists: add user list to local database
                                    if (!localUserLists.contains(remoteUserList)) {
                                        mDatabase.userListsDao().addList(remoteUserList);
                                    }
                                }

                                onPullUserListsComplete();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    //pull remotely saved movie data records to local database
    private void pullMovieDataRecords() {
        //get locally saved movie data records
        //get remotely saved movie data records
        //iterate through remote movieDataRecordSnapshots
        //if local movie data records do not contain remote movie data records: add movie data record to local database

        //get locally saved movie data records
        final List<MovieDataRecord> localMovieDataRecords = mDatabase.movieDataRecordsDao().getAllRecordsAlt();

        //get remotely saved movie data records
        FirebaseMovieDataRecordsDao.getAllMovieDataRecords()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(final QuerySnapshot movieDataRecordSnapshots) {
                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                            @Override
                            public void run() {
                                //iterate through remote movieDataRecordSnapshots
                                //if local movie data records do not contain remote movie data records: add movie data record to local database
                                for (DocumentSnapshot movieDataRecordDocument: movieDataRecordSnapshots.getDocuments()) {
                                    if (movieDataRecordDocument.getData() == null) continue;

                                    final MovieDataRecord remoteMovieDataRecord =
                                            MovieDataRecord.parseMediaRecord(movieDataRecordDocument);

                                    //if local user lists do not contain remote user lists: add user list to local database
                                    if (!localMovieDataRecords.contains(remoteMovieDataRecord)) {
                                        mDatabase.movieDataRecordsDao().addRecord(remoteMovieDataRecord);
                                    }
                                }

                                onPullMovieDataRecordsComplete();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    //pull remotely saved series data records to local database
    private void pullSeriesDataRecords() {
        //get locally saved series data records
        //get remotely saved series data records
        //iterate through remote seriesDataRecordSnapshots
        //if local series data records do not contain remote series data records: add series data record to local database

        //get locally saved series data records
        final List<SeriesDataRecord> localSeriesDataRecords = mDatabase.seriesDataRecordsDao().getAllRecordsAlt();

        //get remotely saved series data records
        FirebaseSeriesDataRecordsDao.getAllSeriesDataRecords()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(final QuerySnapshot seriesDataRecordSnapshots) {
                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                            @Override
                            public void run() {
                                //iterate through remote seriesDataRecordSnapshots
                                //if local series data records do not contain remote series data records: add series data record to local database
                                for (DocumentSnapshot seriesDataRecordDocument: seriesDataRecordSnapshots.getDocuments()) {
                                    if (seriesDataRecordDocument.getData() == null) continue;

                                    final SeriesDataRecord remoteSeriesDataRecord =
                                            SeriesDataRecord.parseMediaRecord(seriesDataRecordDocument);

                                    //if local user lists do not contain remote user lists: add user list to local database
                                    if (!localSeriesDataRecords.contains(remoteSeriesDataRecord)) {
                                        mDatabase.seriesDataRecordsDao().addRecord(remoteSeriesDataRecord);
                                    }
                                }

                                onPullSeriesDataRecordsComplete();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
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
