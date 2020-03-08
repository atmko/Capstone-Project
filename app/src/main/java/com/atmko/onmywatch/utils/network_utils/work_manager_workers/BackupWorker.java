/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils.work_manager_workers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.SeriesLog;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class BackupWorker extends Worker {
    private static final String TAG = BackupWorker.class.getSimpleName();

    public static final int JOB_ID = 10;

    private static final String BACKUP_CHANNEL_ID = "Backup Channel";
    private static final String BACKUP_NAME = "backup";
    private static final String BACKUP_LOCAL_PATH = "backups";

    private static final String MOVIES_KEY = "movies";
    private static final String SERIES_KEY = "series";
    private static final String WATCH_LISTS_KEY = "watch_lists";
    private static final String USER_LISTS_KEY = "user_lists";
    private static final String MOVIE_DATA_RECORDS_KEY = "movie_data_records";
    private static final String SERIES_DATA_RECORDS_KEY = "series_data_records";
    private static final String MOVIES_NOTIFIERS_KEY = "movie_notifiers";
    private static final String SERIES_NOTIFIERS_KEY = "series_notifiers";
    private static final String SERIES_LOGS_KEY = "series_logs";

    private final Context mContext;
    private AppDatabase mLocalDatabase;
    private Map mDatabaseMap;
    private StorageReference mBackupRef;

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        mContext = context;
        mLocalDatabase = AppDatabase.getInstance(mContext);
        mDatabaseMap = new HashMap();
        mBackupRef = FirebaseStorage.getInstance().getReference()
                .child(BACKUP_LOCAL_PATH + "/" + BACKUP_NAME + ".json");
    }

    @NonNull
    @Override
    public Result doWork() {
        backupToRemoteDatabase();
        return Result.success();
    }

    public static void createBackupNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.notification_channel_backup);
            String description = context.getString(R.string.notification_channel_backup_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(BACKUP_CHANNEL_ID, name, importance);
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

        return new NotificationCompat.Builder(context, BACKUP_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify_black)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }

    private void backupToRemoteDatabase() {
        //other methods run one after the other starting with pushMovieData
        pushMovieData();
    }

    private void onPushMovieDataComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushSeriesData();
            }
        });
    }

    private void onPushSeriesDataComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushWatchLists();
            }
        });
    }

    private void onPushWatchListsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushUserLists();
            }
        });
    }

    private void onPushUserListsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushMovieDataRecords();
            }
        });
    }

    private void onPushMovieDataRecordsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushSeriesDataRecords();
            }
        });
    }

    private void onPushSeriesDataRecordsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushMovieNotifiers();
            }
        });
    }

    private void onPushMovieNotifiersComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushSeriesNotifiers();
            }
        });
    }

    private void onPushSeriesNotifiersComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                pushSeriesLogs();
            }
        });
    }

    private void onPushMediaLogsComplete() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                writeToFile(mDatabaseMap);
                writeToDatabase();
            }
        });
    }

    //pushes locally saved movies to remote database
    private void pushMovieData() {
        //get locally saved movies
        //save to cloud

        //get locally saved movies
        List<MovieData> localMovieDataList = mLocalDatabase.movieDataDao().getAllMoviesAlt();
        List<Map> mapList = new ArrayList<>();
        for (MovieData movieData: localMovieDataList) {
            Map movieDataMap = movieData.parseMediaDataToDataMap();
            mapList.add(movieDataMap);
        }

        mDatabaseMap.put(MOVIES_KEY, mapList);

        onPushMovieDataComplete();
    }

    //pushes locally saved series to remote database
    private void pushSeriesData() {
        //get locally saved series
        //save to cloud

        //get locally saved series
        List<SeriesData> localSeriesDataList = mLocalDatabase.seriesDataDao().getAllSeriesAlt();
        List<Map> mapList = new ArrayList<>();
        for (SeriesData seriesData: localSeriesDataList) {
            Map seriesDataMap = seriesData.parseMediaDataToDataMap();
            mapList.add(seriesDataMap);
        }

        mDatabaseMap.put(SERIES_KEY, mapList);

        onPushSeriesDataComplete();
    }

    //pushes locally saved watch lists to remote database
    private void pushWatchLists() {
        //get locally saved watch lists
        //save to cloud

        //get locally saved watch lists
        List<WatchListModel> localWatchLists = mLocalDatabase.watchListsDao().getAllListsAlt();
        List<Map> mapList = new ArrayList<>();
        for (WatchListModel watchListModel: localWatchLists) {
            Map watchListsMap = watchListModel.parseListModelToDataMap();
            mapList.add(watchListsMap);
        }

        mDatabaseMap.put(WATCH_LISTS_KEY, mapList);

        onPushWatchListsComplete();
    }

    //pushes locally saved user lists to remote database
    private void pushUserLists() {
        //get locally saved user lists
        //save to cloud

        //get locally saved user lists
        List<UserListModel> localUserLists = mLocalDatabase.userListsDao().getAllListsAlt();
        List<Map> mapList = new ArrayList<>();
        for (UserListModel userListModel: localUserLists) {
            Map userListsMap = userListModel.parseListModelToDataMap();
            mapList.add(userListsMap);
        }

        mDatabaseMap.put(USER_LISTS_KEY, mapList);

        onPushUserListsComplete();
    }

    //pushes locally saved movie data records to remote database
    private void pushMovieDataRecords() {
        //get locally saved movie records
        //save to cloud

        //get locally saved movie records
        List<MovieDataRecord> localMovieDataRecords = mLocalDatabase.movieDataRecordsDao().getAllRecordsAlt();
        List<Map> mapList = new ArrayList<>();
        for (MovieDataRecord movieDataRecord: localMovieDataRecords) {
            Map movieRecordsMap = movieDataRecord.parseListModelToDataMap();
            mapList.add(movieRecordsMap);
        }

        mDatabaseMap.put(MOVIE_DATA_RECORDS_KEY, mapList);

        onPushMovieDataRecordsComplete();
    }

    //pushes locally saved series data records to remote database
    private void pushSeriesDataRecords() {
        //get locally saved series records
        //save to cloud

        //get locally saved series records
        List<SeriesDataRecord> localSeriesDataRecords = mLocalDatabase.seriesDataRecordsDao().getAllRecordsAlt();
        List<Map> mapList = new ArrayList<>();
        for (SeriesDataRecord seriesDataRecord: localSeriesDataRecords) {
            Map seriesRecordsMap = seriesDataRecord.parseListModelToDataMap();
            mapList.add(seriesRecordsMap);
        }

        mDatabaseMap.put(SERIES_DATA_RECORDS_KEY, mapList);

        onPushSeriesDataRecordsComplete();
    }

    //pushes locally saved movie notifiers to remote database
    private void pushMovieNotifiers() {
        //get locally saved movie notifiers
        //save to cloud

        //get locally saved movie notifiers
        List<MovieNotifier> localMovieNotifiers = mLocalDatabase.movieNotifierDao().getAllNotifiersAlt();
        List<Map> mapList = new ArrayList<>();
        for (MovieNotifier movieNotifier: localMovieNotifiers) {
            Map movieNotifiersDataMap = movieNotifier.parseNotifierToDataMap();
            mapList.add(movieNotifiersDataMap);
        }

        mDatabaseMap.put(MOVIES_NOTIFIERS_KEY, mapList);

        onPushMovieNotifiersComplete();
    }

    //pushes locally saved series notifiers to remote database
    private void pushSeriesNotifiers() {
        //get locally saved series notifiers
        //save to cloud

        //get locally saved series notifiers
        List<SeriesNotifier> localSeriesNotifiers = mLocalDatabase.seriesNotifierDao().getAllNotifiersAlt();
        List<Map> mapList = new ArrayList<>();
        for (SeriesNotifier seriesNotifier: localSeriesNotifiers) {
            Map seriesNotifiersDataMap = seriesNotifier.parseNotifierToDataMap();
            mapList.add(seriesNotifiersDataMap);
        }

        mDatabaseMap.put(SERIES_NOTIFIERS_KEY, mapList);

        onPushSeriesNotifiersComplete();
    }

    //pushes locally saved mediaLogs to remote database
    private void pushSeriesLogs() {
        //get locally saved series logs
        //save to cloud

        //get locally saved series logs
        List<SeriesLog> localSeriesLogs = mLocalDatabase.seriesLogsDao().getAllLogsAlt();
        List<Map> mapList = new ArrayList<>();
        for (SeriesLog seriesLog: localSeriesLogs) {
            Map seriesLogsDataMap = seriesLog.parseLogToDataMap();
            mapList.add(seriesLogsDataMap);
        }

        mDatabaseMap.put(SERIES_LOGS_KEY, mapList);

        onPushMediaLogsComplete();
    }

    private void writeToFile(Map map) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            OutputStreamWriter outputStreamWriter =
                    new OutputStreamWriter(mContext.openFileOutput(BACKUP_NAME, Context.MODE_PRIVATE));
            outputStreamWriter.write(gson.toJson(map));
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void writeToDatabase() {
        try {
            InputStream inputStream = mContext.openFileInput(BACKUP_NAME);
            mBackupRef.putStream(inputStream).addOnSuccessListener(
                    new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            deleteLocalFile();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {

                        }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void deleteLocalFile() {
        File dir = mContext.getFilesDir();
        File file = new File(dir, BACKUP_NAME);
        if (file.exists()) {
            if (file.delete()) {
                Log.d(TAG, "file Deleted");
            } else {
                Log.d(TAG, "file not Deleted");
            }
        }
    }
}