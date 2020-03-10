/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

import android.content.Context;
import android.util.Log;

import com.atmko.onmywatch.MasterActivity;
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
import com.google.android.gms.tasks.Tasks;
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
import java.util.concurrent.ExecutionException;

@SuppressWarnings("unchecked")
public class BackupLogic {
    private static final String TAG = com.atmko.onmywatch.utils.network_utils.work_manager_workers.BackupWorker.class.getSimpleName();

    private static final String BACKUP_NAME = "backup";
    private static final String USERS_PATH = "users";
    private static final String BACKUP_LOCAL_PATH = "backups";
    private static final String BACKUP_EXTENSION = ".json";

    private static final String MOVIES_KEY = "movies";
    private static final String SERIES_KEY = "series";
    private static final String WATCH_LISTS_KEY = "watch_lists";
    private static final String USER_LISTS_KEY = "user_lists";
    private static final String MOVIE_DATA_RECORDS_KEY = "movie_data_records";
    private static final String SERIES_DATA_RECORDS_KEY = "series_data_records";
    private static final String MOVIES_NOTIFIERS_KEY = "movie_notifiers";
    private static final String SERIES_NOTIFIERS_KEY = "series_notifiers";
    private static final String SERIES_LOGS_KEY = "series_logs";

    private Context mContext;
    private AppDatabase mLocalDatabase;
    private Map mDatabaseMap;
    private StorageReference mBackupRef;

    public BackupLogic(Context context) {
        mContext = context;
        mLocalDatabase = AppDatabase.getInstance(mContext);
        mDatabaseMap = new HashMap();
        mBackupRef = FirebaseStorage.getInstance().getReference()
                .child(USERS_PATH + "/" + MasterActivity.getCurrentUser().getUid()
                        + "/" + BACKUP_LOCAL_PATH + "/" + BACKUP_NAME + BACKUP_EXTENSION);
    }

    public boolean backupToRemoteDatabase() {
        try {
            //other methods run one after the other starting with pushMovieData
            pushMovieData();
            pushSeriesData();
            pushWatchLists();
            pushUserLists();
            pushMovieDataRecords();
            pushSeriesDataRecords();
            pushMovieNotifiers();
            pushSeriesNotifiers();
            pushSeriesLogs();
            onPushComplete();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
    }

    private void onPushComplete() {
        writeToFile(mDatabaseMap);
        writeToDatabase();
    }

    private void writeToFile(Map map) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            OutputStreamWriter outputStreamWriter =
                    new OutputStreamWriter(mContext
                            .openFileOutput(BACKUP_NAME, Context.MODE_PRIVATE));
            outputStreamWriter.write(gson.toJson(map));
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void writeToDatabase() {
        try {
            InputStream inputStream = mContext.openFileInput(BACKUP_NAME);
            UploadTask uploadTask = mBackupRef.putStream(inputStream);

            Tasks.await(uploadTask);
            deleteLocalFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            deleteLocalFile();
        } catch (ExecutionException e) {
            e.printStackTrace();
            deleteLocalFile();
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