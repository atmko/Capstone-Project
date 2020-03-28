/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.atmko.onmywatch.database.daos.MovieDataDao;
import com.atmko.onmywatch.database.daos.MovieDataRecordsDao;
import com.atmko.onmywatch.database.daos.MovieNotifierDao;
import com.atmko.onmywatch.database.daos.SearchListTagsDao;
import com.atmko.onmywatch.database.daos.SearchMediaTagsDao;
import com.atmko.onmywatch.database.daos.SeriesDataDao;
import com.atmko.onmywatch.database.daos.SeriesDataRecordsDao;
import com.atmko.onmywatch.database.daos.SeriesLogsDao;
import com.atmko.onmywatch.database.daos.SeriesNotifierDao;
import com.atmko.onmywatch.database.daos.UserListsDao;
import com.atmko.onmywatch.database.daos.WatchListsDao;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SearchListTag;
import com.atmko.onmywatch.models.SearchMediaTag;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.SeriesLog;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.NotificationHandler;

@Database(entities = {WatchListModel.class, UserListModel.class, MovieData.class, SeriesData.class,
        MovieDataRecord.class, SeriesDataRecord.class, MovieNotifier.class, SeriesNotifier.class,
        SeriesLog.class, SearchMediaTag.class, SearchListTag.class},
        version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "on_my_watch_database";
    private static final Object LOCK = new Object();

    private static AppDatabase sInstance;

    public static AppDatabase getInstance(Context context, RoomDatabase.Callback callback) {
        if (sInstance != null) {
            return sInstance;
        }

        synchronized (LOCK) {
            sInstance = Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
                    //TODO remove allowance of main thread queries
                    .addCallback(callback)
                    .build();

            return sInstance;
        }
    }

    public static AppDatabase getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }

        synchronized (LOCK) {
            sInstance = Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
                    .build();

            return sInstance;
        }
    }

    public static AppDatabase getLocalDatabase(Context context) {
        if (sInstance != null) {
            return sInstance;
        }

        return Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME).build();
    }

    public static void setDatabase(AppDatabase database) {
        sInstance = database;
    }

    public static void closeDatabase() {
        if (sInstance != null) {
            sInstance = null;
        }
    }

    public static void deleteLocallySavedData(Context context) {
        AppDatabase.getInstance(context).clearAllTables();

        //cancel all alarms
        NotificationHandler.cancelAllAlarms(context);
    }

    public abstract WatchListsDao watchListsDao();
    public abstract UserListsDao userListsDao();
    public abstract MovieDataDao movieDataDao();
    public abstract SeriesDataDao seriesDataDao();
    public abstract MovieDataRecordsDao movieDataRecordsDao();
    public abstract SeriesDataRecordsDao seriesDataRecordsDao();
    public abstract MovieNotifierDao movieNotifierDao();
    public abstract SeriesNotifierDao seriesNotifierDao();
    public abstract SeriesLogsDao seriesLogsDao();
    public abstract SearchMediaTagsDao searchMediaTagsDao();
    public abstract SearchListTagsDao searchListTagsDao();
}
