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
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseUserListDao;
import com.atmko.onmywatch.database.daos.FirebaseWatchListDao;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProModeMigrationService extends JobIntentService {
    private static final String TAG = ProModeMigrationService.class.getSimpleName();

    public static final int JOB_ID = 10;

    //notification channel ids
    public static final String MIGRATION_CHANNEL_ID = "Migration Channel";

    public static final String ACTION_USER_TIER_TO_PRO = "to_pro";

    private AppDatabase mDatabase;

    private List<MovieData> localMovieDataList;
    private List<SeriesData> localSeriesDataList;
    private List<WatchListModel> localWatchLists;
    private List<UserListModel> localUserLists;
    private List<MovieDataRecord> localMovieDataRecords;
    private List<SeriesDataRecord> localSeriesDataRecords;

    public ProModeMigrationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mDatabase = AppDatabase.getInstance(getApplicationContext());

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

            if (intentAction.equals(ACTION_USER_TIER_TO_PRO)) {
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
                deleteLocallySavedData();
                MasterActivity.sProMode = false;
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
        localMovieDataList = mDatabase.movieDataDao().getAllMoviesAlt();

        //get remotely saved movies
        FirebaseMovieDataDao.getAllMovies().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(final QuerySnapshot snapshots) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        //create list of local ids to compare with remote ids
                        final List<String> localIdList = new ArrayList<>();
                        for (MovieData movieData: localMovieDataList) {
                            localIdList.add(movieData.getId());

                        }

                        //create list of remote ids to compare with local ids
                        List<DocumentSnapshot> documentSnapshots = snapshots.getDocuments();
                        List<String> remoteIdList = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                            remoteIdList.add(((String) documentSnapshot.get(ApiConstants.ID_KEY)));

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
                                DocumentSnapshot documentSnapshot = documentSnapshots.get(correspondingRemoteIndex);
                                String documentId = documentSnapshot.getId();

                                //add document id to batchUpdateIds
                                batchUpdateIds.add(documentId);
                                //add to batchUpdateList for batch updates
                                batchUpdateList.add(currentMovieData.parseMediaDataToDataMap());

                                //if remotely saved movie's id is not in local database: add to batchCreateList
                            } else {
                                //add to batchCreateList for batch creates
                                batchCreateList.add(currentMovieData.parseMediaDataToDataMap());
                            }
                        }

                        //batch create and batch update
                        FirebaseMovieDataDao.addMovieDataBatch(batchCreateList).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                FirebaseMovieDataDao.updateMovieDataBatch(batchUpdateIds, batchUpdateList).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        onPushMovieDataComplete();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
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
        localSeriesDataList = mDatabase.seriesDataDao().getAllSeriesAlt();

        //get remotely saved series
        FirebaseSeriesDataDao.getAllSeries().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(final QuerySnapshot snapshots) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        //create list of local ids to compare with remote ids
                        final List<String> localIdList = new ArrayList<>();
                        for (SeriesData seriesData : localSeriesDataList) {
                            localIdList.add(seriesData.getId());

                        }

                        //create list of remote ids to compare with local ids
                        List<DocumentSnapshot> documentSnapshots = snapshots.getDocuments();
                        List<String> remoteIdList = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                            remoteIdList.add(((String) documentSnapshot.get(ApiConstants.ID_KEY)));

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
                                DocumentSnapshot documentSnapshot = documentSnapshots.get(correspondingRemoteIndex);
                                String documentId = documentSnapshot.getId();

                                //add document id to batchUpdateIds
                                batchUpdateIds.add(documentId);
                                //add to batchUpdateList for batch updates
                                batchUpdateList.add(currentSeriesData.parseMediaDataToDataMap());

                                //if remotely saved series's id is not in local database: add to batchCreateList
                            } else {
                                //add to batchCreateList for batch creates
                                batchCreateList.add(currentSeriesData.parseMediaDataToDataMap());
                            }
                        }

                        //batch create and batch update
                        FirebaseSeriesDataDao.addSeriesDataBatch(batchCreateList).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                FirebaseSeriesDataDao.updateSeriesDataBatch(batchUpdateIds, batchUpdateList).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        onPushSeriesDataComplete();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
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
        localWatchLists = mDatabase.watchListsDao().getAllListsAlt();

        //get remotely saved watch lists
        FirebaseWatchListDao.getAllWatchLists().get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(final QuerySnapshot snapshots) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        //create list of local ids to compare with remote ids
                        final List<String> localIdList = new ArrayList<>();
                        for (WatchListModel watchListModel : localWatchLists) {
                            localIdList.add(watchListModel.getName());

                        }

                        //create list of remote ids to compare with local ids
                        List<DocumentSnapshot> documentSnapshots = snapshots.getDocuments();
                        List<String> remoteIdList = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                            remoteIdList.add(((String) documentSnapshot.get(ListModel.LIST_NAME_KEY)));

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
                                DocumentSnapshot documentSnapshot = documentSnapshots.get(correspondingRemoteIndex);
                                String documentId = documentSnapshot.getId();

                                //add document id to batchUpdateIds
                                batchUpdateIds.add(documentId);
                                //add to batchUpdateList for batch updates
                                batchUpdateList.add(currentWatchList.parseListModelToDataMap());

                                //if remotely saved watch list's id is not in local database: add to batchCreateList
                            } else {
                                //add to batchCreateList for batch creates
                                batchCreateList.add(currentWatchList.parseListModelToDataMap());
                            }
                        }

                        //batch create and batch update
                        FirebaseWatchListDao.addWatchListBatch(batchCreateList).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                FirebaseWatchListDao.updateWatchListBatch(batchUpdateIds, batchUpdateList)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                onPushWatchListsComplete();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
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
        localUserLists = mDatabase.userListsDao().getAllListsAlt();

        //get remotely saved user lists
        FirebaseUserListDao.getAllUserLists().get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(final QuerySnapshot snapshots) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        //create list of local ids to compare with remote ids
                        final List<String> localIdList = new ArrayList<>();
                        for (UserListModel userListModel : localUserLists) {
                            localIdList.add(userListModel.getName());

                        }

                        //create list of remote ids to compare with local ids
                        List<DocumentSnapshot> documentSnapshots = snapshots.getDocuments();
                        List<String> remoteIdList = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                            remoteIdList.add(((String) documentSnapshot.get(ListModel.LIST_NAME_KEY)));

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
                                DocumentSnapshot documentSnapshot = documentSnapshots.get(correspondingRemoteIndex);
                                String documentId = documentSnapshot.getId();

                                //add document id to batchUpdateIds
                                batchUpdateIds.add(documentId);
                                //add to batchUpdateList for batch updates
                                batchUpdateList.add(currentUserList.parseListModelToDataMap());

                                //if remotely saved user list's id is not in local database: add to batchCreateList
                            } else {
                                //add to batchCreateList for batch creates
                                batchCreateList.add(currentUserList.parseListModelToDataMap());
                            }
                        }

                        //batch create and batch update
                        FirebaseUserListDao.addUserListBatch(batchCreateList).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                FirebaseUserListDao.updateUserListBatch(batchUpdateIds, batchUpdateList).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        onPushUserListsComplete();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
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
        localMovieDataRecords = mDatabase.movieDataRecordsDao().getAllRecordsAlt();

        //get remotely saved movie records
        FirebaseMovieDataRecordsDao.getAllMovieDataRecords().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(final QuerySnapshot snapshots) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        //create list of remote movie records to compare with local movie records
                        List<DocumentSnapshot> documentSnapshots = snapshots.getDocuments();
                        final List<MovieDataRecord> remoteMovieDataRecords = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                            remoteMovieDataRecords.add(MovieDataRecord.parseMediaRecord(documentSnapshot));
                        }

                        //create lists for batch writes
                        List<Map<String, Object>> batchCreateList = new ArrayList<>();

                        //compare local and remote records
                        for (MovieDataRecord localMovieDataRecord : localMovieDataRecords) {
                            //if locally saved record is not in remote database: add to batchCreateList
                            if (!remoteMovieDataRecords.contains(localMovieDataRecord)) {
                                //add to batchCreateList for batch creates
                                batchCreateList.add(localMovieDataRecord.parseListModelToDataMap());
                            }
                        }

                        //batch create
                        FirebaseMovieDataRecordsDao.addMovieDataRecordBatch(batchCreateList).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                onPushMovieDataRecordsComplete();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
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
        localSeriesDataRecords = mDatabase.seriesDataRecordsDao().getAllRecordsAlt();

        //get remotely saved series records
        FirebaseSeriesDataRecordsDao.getAllSeriesDataRecords().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(final QuerySnapshot snapshots) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        //create list of remote series records to compare with local series records
                        List<DocumentSnapshot> documentSnapshots = snapshots.getDocuments();
                        final List<SeriesDataRecord> remoteSeriesDataRecords = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                            remoteSeriesDataRecords.add(SeriesDataRecord.parseMediaRecord(documentSnapshot));
                        }

                        //create lists for batch writes
                        List<Map<String, Object>> batchCreateList = new ArrayList<>();

                        //compare local and remote records
                        for (SeriesDataRecord localSeriesDataRecord : localSeriesDataRecords) {
                            //if locally saved record is not in remote database: add to batchCreateList
                            if (!remoteSeriesDataRecords.contains(localSeriesDataRecord)) {
                                //add to batchCreateList for batch creates
                                batchCreateList.add(localSeriesDataRecord.parseListModelToDataMap());
                            }
                        }

                        //batch create
                        FirebaseSeriesDataRecordsDao.addSeriesDataRecordBatch(batchCreateList).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                onPushSeriesDataRecordsComplete();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private void deleteLocallySavedData() {
        for (MovieDataRecord movieDataRecord: localMovieDataRecords) {
            mDatabase.movieDataRecordsDao().deleteRecord(movieDataRecord);
        }

        for (SeriesDataRecord seriesDataRecord: localSeriesDataRecords) {
            mDatabase.seriesDataRecordsDao().deleteRecord(seriesDataRecord);
        }

        for (MovieData movieData: localMovieDataList) {
            mDatabase.movieDataDao().deleteMovieData(movieData);
        }

        for (SeriesData seriesData: localSeriesDataList) {
            mDatabase.seriesDataDao().deleteSeriesData(seriesData);
        }

        for (WatchListModel watchListModel: localWatchLists) {
            mDatabase.watchListsDao().deleteList(watchListModel);
        }

        for (UserListModel userListModel: localUserLists) {
            mDatabase.userListsDao().deleteList(userListModel);
        }
    }
}
