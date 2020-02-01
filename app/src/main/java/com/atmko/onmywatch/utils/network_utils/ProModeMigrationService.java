/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.database.daos.FirebaseMovieDataDao;
import com.atmko.onmywatch.database.daos.FirebaseMovieDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseMovieNotifiersDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesNotifiersDao;
import com.atmko.onmywatch.database.daos.FirebaseUserDataDao;
import com.atmko.onmywatch.database.daos.FirebaseUserListDao;
import com.atmko.onmywatch.database.daos.FirebaseWatchListDao;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.database.daos.FirebaseUserDataDao.MIGRATION_CLOUD;
import static com.atmko.onmywatch.database.daos.FirebaseUserDataDao.MIGRATION_TO_CLOUD;

public class ProModeMigrationService extends JobIntentService {
    private static final String TAG = ProModeMigrationService.class.getSimpleName();

    public static final int JOB_ID = 10;

    //notification channel ids
    public static final String MIGRATION_CHANNEL_ID = "Migration Channel";

    private AppDatabase mLocalDatabase;
    private AppDatabase mRemoteDatabase;

    private List<MovieData> localMovieDataList;
    private List<SeriesData> localSeriesDataList;
    private List<WatchListModel> localWatchLists;
    private List<UserListModel> localUserLists;
    private List<MovieDataRecord> localMovieDataRecords;
    private List<SeriesDataRecord> localSeriesDataRecords;
    private List<MovieNotifier> localMovieNotifiers;
    private List<SeriesNotifier> localSeriesNotifiers;

    public ProModeMigrationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLocalDatabase = AppDatabase.getLocalDatabase(getApplicationContext());
        mRemoteDatabase = AppDatabase.getRemoteDatabase();

        startForeground(JOB_ID,
                buildNotification(getApplicationContext(),
                        getString(R.string.notification_migration_title),
                        getString(R.string.notification_pro_migration_content)
                )
        );
    }

    public static void enqueueWork(Context appContext, Intent intent) {
        enqueueWork(appContext, ProModeMigrationService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String intentAction = intent.getAction();

        try {
            if (intentAction == null) throw new NullPointerException("no intent action specified");

            if (intentAction.equals(MIGRATION_TO_CLOUD)) {
                Log.d(TAG, "migrating to pro");
                migrateToRemoteDatabase();

            }

        } catch (NullPointerException e) {
            e.printStackTrace();

        }
    }

    public static void createMigrationNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.notification_channel_migration);
            String description = context.getString(R.string.notification_channel_migration_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(MIGRATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
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

    private void migrateToRemoteDatabase() {
        //other methods run one after the other starting with pushMovieData
        pushMovieData();
    }

    public void onPushMovieDataComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushSeriesData();
            }
        });
    }

    public void onPushSeriesDataComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushWatchLists();
            }
        });
    }

    public void onPushWatchListsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushUserLists();
            }
        });
    }

    public void onPushUserListsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushMovieDataRecords();
            }
        });
    }

    public void onPushMovieDataRecordsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushSeriesDataRecords();
            }
        });
    }

    public void onPushSeriesDataRecordsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushMovieNotifiers();
            }
        });
    }

    public void onPushMovieNotifiersComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushSeriesNotifiers();
            }
        });
    }

    public void onPushSeriesNotifiersComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                deleteLocallySavedData();
                FirebaseUserDataDao.setMigrationValue(MIGRATION_CLOUD);
                finishService();
            }
        });
    }

    private void finishService() {
        stopForeground(true);
        stopSelf();
    }

    //pushes locally saved movies to remote database
    private void pushMovieData() {
        //get locally saved movies
        //get remotely saved movies
        //create list of local ids to compare with remote ids
        //create list of remote ids to compare with local ids
        //compare their ids
        //if remotely saved movie's id is in local database: add to batchUpdateList
        //if remotely saved movie's id is not in local database: add to batchCreateList
        //batch create and batch update

        //get locally saved movies
        localMovieDataList = mLocalDatabase.movieDataDao().getAllMoviesAlt();
        //get remotely saved movies
        List<MovieData> remoteMovies = mRemoteDatabase.movieDataDao().getAllMoviesAlt();

        //create list of local ids to compare with remote ids
        final List<String> localIdList = new ArrayList<>();
        for (MovieData movieData : localMovieDataList) {
            localIdList.add(movieData.getId());
        }

        //create list of remote ids to compare with local ids
        List<String> remoteIdList = new ArrayList<>();
        for (MovieData movieData : remoteMovies) {
            remoteIdList.add(movieData.getId());
        }

        //create lists for batch writes
        List<Map<String, Object>> batchCreateList = new ArrayList<>();
        final List<String> batchUpdateIds = new ArrayList<>();
        final List<Map<String, Object>> batchUpdateList = new ArrayList<>();

        //compare their ids
        for (String currentLocalId : localIdList) {
            int index = localIdList.indexOf(currentLocalId);
            //get local movie data
            MovieData currentMovieData = localMovieDataList.get(index);

            //if remotely saved movie's id is in local database: add to batchUpdateList
            if (remoteIdList.contains(currentLocalId)) {
                //get corresponding document id
                int correspondingRemoteIndex = remoteIdList.indexOf(currentLocalId);
                MovieData movieData = remoteMovies.get(correspondingRemoteIndex);
                String uniqueExternalId = movieData.getUniqueExternalId();

                //add document id to batchUpdateIds
                batchUpdateIds.add(uniqueExternalId);
                //add to batchUpdateList for batch updates
                batchUpdateList.add(currentMovieData.parseMediaDataToDataMap());

                //if remotely saved movie's id is not in local database: add to batchCreateList
            } else {
                //add to batchCreateList for batch creates
                batchCreateList.add(currentMovieData.parseMediaDataToDataMap());
            }
        }

        //batch create and batch update
        FirebaseMovieDataDao.addMovieDataBatch(batchCreateList);
        FirebaseMovieDataDao.updateMovieDataBatch(batchUpdateIds, batchUpdateList);

        onPushMovieDataComplete();
    }

    //pushes locally saved series to remote database
    private void pushSeriesData() {
        //get locally saved series
        //get remotely saved series
        //create list of local ids to compare with remote ids
        //create list of remote ids to compare with local ids
        //compare their ids
        //if remotely saved series's id is in local database: add to batchUpdateList
        //if remotely saved series's id is not in local database: add to batchCreateList
        //batch create and batch update

        //get locally saved series
        localSeriesDataList = mLocalDatabase.seriesDataDao().getAllSeriesAlt();
        //get remotely saved series
        List<SeriesData> remoteSeries = mRemoteDatabase.seriesDataDao().getAllSeriesAlt();

        //create list of local ids to compare with remote ids
        final List<String> localIdList = new ArrayList<>();
        for (SeriesData seriesData : localSeriesDataList) {
            localIdList.add(seriesData.getId());
        }

        //create list of remote ids to compare with local ids
        List<String> remoteIdList = new ArrayList<>();
        for (SeriesData seriesData : remoteSeries) {
            remoteIdList.add(seriesData.getId());
        }

        //create lists for batch writes
        List<Map<String, Object>> batchCreateList = new ArrayList<>();
        final List<String> batchUpdateIds = new ArrayList<>();
        final List<Map<String, Object>> batchUpdateList = new ArrayList<>();

        //compare their ids
        for (String currentLocalId : localIdList) {
            int index = localIdList.indexOf(currentLocalId);
            //get local series data
            SeriesData currentSeriesData = localSeriesDataList.get(index);

            //if remotely saved series's id is in local database: add to batchUpdateList
            if (remoteIdList.contains(currentLocalId)) {
                //get corresponding document id
                int correspondingRemoteIndex = remoteIdList.indexOf(currentLocalId);
                SeriesData seriesData = remoteSeries.get(correspondingRemoteIndex);
                String uniqueExternalId = seriesData.getUniqueExternalId();

                //add document id to batchUpdateIds
                batchUpdateIds.add(uniqueExternalId);
                //add to batchUpdateList for batch updates
                batchUpdateList.add(currentSeriesData.parseMediaDataToDataMap());

                //if remotely saved series's id is not in local database: add to batchCreateList
            } else {
                //add to batchCreateList for batch creates
                batchCreateList.add(currentSeriesData.parseMediaDataToDataMap());
            }
        }

        //batch create and batch update
        FirebaseSeriesDataDao.addSeriesDataBatch(batchCreateList);
        FirebaseSeriesDataDao.updateSeriesDataBatch(batchUpdateIds, batchUpdateList);

        onPushSeriesDataComplete();
    }

    //pushes locally saved watch lists to remote database
    private void pushWatchLists() {
        //get locally saved watchLists
        //get remotely saved watch lists
        //create list of local ids to compare with remote ids
        //create list of remote ids to compare with local ids
        //compare their ids
        //if remotely saved watch list's id is in local database: add to batchUpdateList
        //if remotely saved watch list's id is not in local database: add to batchCreateList
        //batch create and batch update

        //get locally saved watchLists
        localWatchLists = mLocalDatabase.watchListsDao().getAllListsAlt();
        //get remotely saved watch lists
        List<WatchListModel> remoteLists = mRemoteDatabase.watchListsDao().getAllListsAlt();

        //create list of local ids to compare with remote ids
        final List<String> localIdList = new ArrayList<>();
        for (WatchListModel watchListModel : localWatchLists) {
            localIdList.add(watchListModel.getName());
        }

        //create list of remote ids to compare with local ids
        List<String> remoteIdList = new ArrayList<>();
        for (WatchListModel watchListModel : remoteLists) {
            remoteIdList.add(watchListModel.getName());
        }

        //create lists for batch writes
        List<Map<String, Object>> batchCreateList = new ArrayList<>();
        final List<String> batchUpdateIds = new ArrayList<>();
        final List<Map<String, Object>> batchUpdateList = new ArrayList<>();

        //compare their ids
        for (String currentLocalId : localIdList) {
            int index = localIdList.indexOf(currentLocalId);
            //get local watch list
            WatchListModel currentWatchList = localWatchLists.get(index);

            //if remotely saved watch list's id is in local database: add to batchUpdateList
            if (remoteIdList.contains(currentLocalId)) {
                //get corresponding document id
                int correspondingRemoteIndex = remoteIdList.indexOf(currentLocalId);
                WatchListModel watchListModel = remoteLists.get(correspondingRemoteIndex);
                String uniqueExternalId = watchListModel.getUniqueExternalId();

                //add document id to batchUpdateIds
                batchUpdateIds.add(uniqueExternalId);
                //add to batchUpdateList for batch updates
                batchUpdateList.add(currentWatchList.parseListModelToDataMap());

                //if remotely saved watch list's id is not in local database: add to batchCreateList
            } else {
                //add to batchCreateList for batch creates
                batchCreateList.add(currentWatchList.parseListModelToDataMap());
            }
        }

        //batch create and batch update
        FirebaseWatchListDao.addWatchListBatch(batchCreateList);
        FirebaseWatchListDao.updateWatchListBatch(batchUpdateIds, batchUpdateList);

        onPushWatchListsComplete();
    }

    //pushes locally saved user lists to remote database
    private void pushUserLists() {
        //get locally saved userLists
        //get remotely saved user lists
        //create list of local ids to compare with remote ids
        //create list of remote ids to compare with local ids
        //compare their ids
        //if remotely saved user list's id is in local database: add to batchUpdateList
        //if remotely saved user list's id is not in local database: add to batchCreateList
        //batch create and batch update

        //get locally saved userLists
        localUserLists = mLocalDatabase.userListsDao().getAllListsAlt();
        //get remotely saved user lists
        List<UserListModel> remoteLists = mRemoteDatabase.userListsDao().getAllListsAlt();

        //create list of local ids to compare with remote ids
        final List<String> localIdList = new ArrayList<>();
        for (UserListModel userListModel : localUserLists) {
            localIdList.add(userListModel.getName());
        }

        //create list of remote ids to compare with local ids
        List<String> remoteIdList = new ArrayList<>();
        for (UserListModel userListModel : remoteLists) {
            remoteIdList.add(userListModel.getName());
        }

        //create lists for batch writes
        List<Map<String, Object>> batchCreateList = new ArrayList<>();
        final List<String> batchUpdateIds = new ArrayList<>();
        final List<Map<String, Object>> batchUpdateList = new ArrayList<>();

        //compare their ids
        for (String currentLocalId : localIdList) {
            int index = localIdList.indexOf(currentLocalId);
            //get local user list
            UserListModel currentUserList = localUserLists.get(index);

            //if remotely saved user list's id is in local database: add to batchUpdateList
            if (remoteIdList.contains(currentLocalId)) {
                //get corresponding document id
                int correspondingRemoteIndex = remoteIdList.indexOf(currentLocalId);
                UserListModel userListModel = remoteLists.get(correspondingRemoteIndex);
                String uniqueExternalId = userListModel.getUniqueExternalId();

                //add document id to batchUpdateIds
                batchUpdateIds.add(uniqueExternalId);
                //add to batchUpdateList for batch updates
                batchUpdateList.add(currentUserList.parseListModelToDataMap());

                //if remotely saved user list's id is not in local database: add to batchCreateList
            } else {
                //add to batchCreateList for batch creates
                batchCreateList.add(currentUserList.parseListModelToDataMap());
            }
        }

        //batch create and batch update
        FirebaseUserListDao.addUserListBatch(batchCreateList);
        FirebaseUserListDao.updateUserListBatch(batchUpdateIds, batchUpdateList);

        onPushUserListsComplete();
    }

    //pushes locally saved movie data records to remote database
    private void pushMovieDataRecords() {
        //get locally saved movie records
        //get remotely saved movie records
        //create list of remote movie records to compare with local movie records
        //compare local and remote records
        //if locally saved record is not in remote database: add to batchCreateList
        //batch create

        //get locally saved movie records
        localMovieDataRecords = mLocalDatabase.movieDataRecordsDao().getAllRecordsAlt();
        //get remotely saved movie records
        List<MovieDataRecord> remoteDataRecords = mRemoteDatabase.movieDataRecordsDao().getAllRecordsAlt();

        //create lists for batch writes
        List<Map<String, Object>> batchCreateList = new ArrayList<>();

        //compare local and remote records
        for (MovieDataRecord localMovieDataRecord : localMovieDataRecords) {
            //if locally saved record is not in remote database: add to batchCreateList
            if (!remoteDataRecords.contains(localMovieDataRecord)) {
                //add to batchCreateList for batch creates
                batchCreateList.add(localMovieDataRecord.parseListModelToDataMap());
            }
        }

        //batch create
        FirebaseMovieDataRecordsDao.addMovieDataRecordBatch(batchCreateList);
        onPushMovieDataRecordsComplete();
    }

    //pushes locally saved series data records to remote database
    private void pushSeriesDataRecords() {
        //get locally saved series records
        //get remotely saved series records
        //create list of remote series records to compare with local series records
        //compare local and remote records
        //if locally saved record is not in remote database: add to batchCreateList
        //batch create

        //get locally saved series records
        localSeriesDataRecords = mLocalDatabase.seriesDataRecordsDao().getAllRecordsAlt();
        //get remotely saved series records
        List<SeriesDataRecord> remoteDataRecords = mRemoteDatabase.seriesDataRecordsDao().getAllRecordsAlt();

        //create lists for batch writes
        List<Map<String, Object>> batchCreateList = new ArrayList<>();

        //compare local and remote records
        for (SeriesDataRecord localSeriesDataRecord : localSeriesDataRecords) {
            //if locally saved record is not in remote database: add to batchCreateList
            if (!remoteDataRecords.contains(localSeriesDataRecord)) {
                //add to batchCreateList for batch creates
                batchCreateList.add(localSeriesDataRecord.parseListModelToDataMap());
            }
        }

        //batch create
        FirebaseSeriesDataRecordsDao.addSeriesDataRecordBatch(batchCreateList);
        onPushSeriesDataRecordsComplete();
    }

    //pushes locally saved movie notifiers to remote database
    private void pushMovieNotifiers() {
        //get locally saved movie notifiers
        //get remotely saved movie notifiers
        //create list of remote movie notifiers to compare with local movie notifiers
        //compare local and remote notifiers
        //if locally saved notifier is not in remote database: add to batchCreateList
        //batch create

        //get locally saved movie notifiers
        localMovieNotifiers = mLocalDatabase.movieNotifierDao().getAllNotifiersAlt();
        //get remotely saved movie records
        List<MovieNotifier> remoteNotifiers = mRemoteDatabase.movieNotifierDao().getAllNotifiersAlt();

        //create lists for batch writes
        List<Map<String, Object>> batchCreateList = new ArrayList<>();

        //compare local and remote records
        for (MovieNotifier localMovieNotifier : localMovieNotifiers) {
            //if locally saved record is not in remote database: add to batchCreateList
            if (!remoteNotifiers.contains(localMovieNotifier)) {
                //add to batchCreateList for batch creates
                batchCreateList.add(localMovieNotifier.parseNotifierToDataMap());
            }
        }

        //batch create
        FirebaseMovieNotifiersDao.addMovieNotifierBatch(batchCreateList);

        onPushMovieNotifiersComplete();
    }

    //pushes locally saved series notifiers to remote database
    private void pushSeriesNotifiers() {
        //get locally saved series notifiers
        //get remotely saved series notifiers
        //create list of remote series notifiers to compare with local series notifiers
        //compare local and remote notifiers
        //if locally saved notifier is not in remote database: add to batchCreateList
        //batch create

        //get locally saved series notifiers
        localSeriesNotifiers = mLocalDatabase.seriesNotifierDao().getAllNotifiersAlt();
        //get remotely saved series records
        List<SeriesNotifier> remoteNotifiers = mRemoteDatabase.seriesNotifierDao().getAllNotifiersAlt();

        //create lists for batch writes
        List<Map<String, Object>> batchCreateList = new ArrayList<>();

        //compare local and remote records
        for (SeriesNotifier localSeriesNotifier : localSeriesNotifiers) {
            //if locally saved record is not in remote database: add to batchCreateList
            if (!remoteNotifiers.contains(localSeriesNotifier)) {
                //add to batchCreateList for batch creates
                batchCreateList.add(localSeriesNotifier.parseNotifierToDataMap());
            }
        }

        //batch create
        FirebaseSeriesNotifiersDao.addSeriesNotifierBatch(batchCreateList);

        onPushSeriesNotifiersComplete();
    }

    private void deleteLocallySavedData() {
        for (MovieDataRecord movieDataRecord: localMovieDataRecords) {
            mLocalDatabase.movieDataRecordsDao().deleteRecord(movieDataRecord);
        }

        for (SeriesDataRecord seriesDataRecord: localSeriesDataRecords) {
            mLocalDatabase.seriesDataRecordsDao().deleteRecord(seriesDataRecord);
        }

        for (MovieData movieData: localMovieDataList) {
            mLocalDatabase.movieDataDao().deleteMovieData(movieData);
        }

        for (SeriesData seriesData: localSeriesDataList) {
            mLocalDatabase.seriesDataDao().deleteSeriesData(seriesData);
        }

        for (WatchListModel watchListModel: localWatchLists) {
            mLocalDatabase.watchListsDao().deleteList(watchListModel);
        }

        for (UserListModel userListModel: localUserLists) {
            mLocalDatabase.userListsDao().deleteList(userListModel);
        }

        for (MovieNotifier movieNotifier: localMovieNotifiers) {
            mLocalDatabase.movieNotifierDao().deleteNotifier(movieNotifier);
        }

        for (SeriesNotifier seriesNotifier: localSeriesNotifiers) {
            mLocalDatabase.seriesNotifierDao().deleteNotifier(seriesNotifier);
        }
    }
}
