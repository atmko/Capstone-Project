/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.atmko.onmywatch.MasterActivity;
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
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class BackupLogic {
    private static final String TAG = BackupLogic.class.getSimpleName();

    private static final String PREFS_NAME = "com.atmko.onmywatch.backup";
    private static final String LAST_BACKED_UP_SUFFIX = "_last_backed_up";
    private static final int SIZE_LIMIT = 10000000;
    private static final int BACKUP_COOL_DOWN = 4;

    private static final int COUNTER_LIMIT = 10;

    private static final String USERS_PATH = "users";
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

    private final Context mContext;
    private final AppDatabase mLocalDatabase;
    private final Map mDatabaseMap;
    private final String mFileName;
    private final StorageReference mBackupRef;
    private String mErrorMessage;

    public BackupLogic(Context context, String folder, String fileName) {
        mContext = context;
        mLocalDatabase = AppDatabase.getInstance(mContext);
        mDatabaseMap = new HashMap();
        mFileName = fileName;
        mBackupRef = FirebaseStorage.getInstance().getReference()
                .child(USERS_PATH + "/" + MasterActivity.getCurrentUser().getUid()
                        + "/" + folder + "/" + fileName + BACKUP_EXTENSION);
    }

    public static int getBackupCounter() {
        Integer lastCounter = FirebaseUserDataDao.getBackupCounterAlt();
        if (lastCounter == null || lastCounter == COUNTER_LIMIT) {
            return 1;

        } else {
            return lastCounter + 1;
        }
    }

    String getErrorMessage() {
        return mErrorMessage;
    }

    private static class BackupValidationException extends Exception {
        final static Integer FILE_TOO_LARGE = 0;
        final static Integer TOO_MANY_REQUESTS = 1;

        static class Builder {
            String message;

            Builder(int errorCondition) {
                if (errorCondition == FILE_TOO_LARGE) {
                    message = "File too large, contact developer";

                } else  if (errorCondition == TOO_MANY_REQUESTS) {
                    message = "Too many requests";
                }
            }

            BackupValidationException Build() {
                return new BackupValidationException(message);
            }
        }

        BackupValidationException(String message) {
            super(message);
        }
    }

    //compares time since last backup with cool down time
    private boolean isTooManyRequests() {
        SharedPreferences backupPrefs =
                mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastBackedUp = backupPrefs.getLong(mFileName + LAST_BACKED_UP_SUFFIX, -1);

        if (lastBackedUp == -1) {
            return false;

        } else {
            long timeSinceLastBackup = new Date().getTime() - lastBackedUp;
            return timeSinceLastBackup <= TimeUnit.MINUTES.toMillis(BACKUP_COOL_DOWN);
        }
    }

    //compares time since last backup with cool down time
    @SuppressWarnings("CharsetObjectCanBeUsed")
    private boolean isFileTooLarge() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            long sizeInBytes = gson.toJson(mDatabaseMap).getBytes("UTF-8").length;
            return sizeInBytes > SIZE_LIMIT;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return true;
        }
    }

    //compares time since last backup with cool down time
    private void updateBackupPreferences(long lastBackedUp) {
        SharedPreferences.Editor backupPrefs =
                mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        backupPrefs.putLong(mFileName + LAST_BACKED_UP_SUFFIX, lastBackedUp);
        backupPrefs.apply();
    }

    public boolean backupToRemoteDatabase() {
        try {
            if (isTooManyRequests()) {
                BackupValidationException.Builder builder =
                        new BackupValidationException.Builder(BackupValidationException.TOO_MANY_REQUESTS);
                BackupValidationException exception = builder.Build();
                mErrorMessage = exception.getMessage();
                throw exception;
            }

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

            updateBackupPreferences(new Date().getTime());
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

    private void onPushComplete() throws IOException, ExecutionException, InterruptedException,
            BackupException, BackupValidationException {
        if (isFileTooLarge()) {
            BackupValidationException.Builder builder =
                    new BackupValidationException.Builder(BackupValidationException.FILE_TOO_LARGE);
            BackupValidationException exception = builder.Build();
            mErrorMessage = exception.getMessage();
            throw exception;
        }

        writeToFile(mDatabaseMap);
        writeToDatabase();
    }

    private void writeToFile(Map map) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        OutputStreamWriter outputStreamWriter =
                new OutputStreamWriter(mContext
                        .openFileOutput(mFileName, Context.MODE_PRIVATE));
        outputStreamWriter.write(gson.toJson(map));
        outputStreamWriter.close();
    }

    static class BackupException extends Exception {
        static final String ERROR_MESSAGE = "Failed to upload backup to database";
        BackupException() {
            super(ERROR_MESSAGE);
        }
    }

    private void writeToDatabase() throws FileNotFoundException,
            ExecutionException, InterruptedException, BackupException {
        InputStream inputStream = mContext.openFileInput(mFileName);
        UploadTask uploadTask = mBackupRef.putStream(inputStream);

        if (NetworkFunctions.isOnline()) {
            Tasks.await(uploadTask);
            deleteLocalFile();
        } else {
            deleteLocalFile();
            throw new BackupException();
        }
    }

    public void deleteLocalFile() {
        File dir = mContext.getFilesDir();
        File file = new File(dir, mFileName);
        if (file.exists()) {
            if (file.delete()) {
                Log.d(TAG, "file Deleted");
            } else {
                Log.d(TAG, "file not Deleted");
            }
        }
    }
}