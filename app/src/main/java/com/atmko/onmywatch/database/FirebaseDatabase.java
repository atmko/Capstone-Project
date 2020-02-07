/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import com.atmko.onmywatch.database.daos.FirebaseMovieDataDao;
import com.atmko.onmywatch.database.daos.FirebaseMovieDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseMovieNotifiersDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesNotifiersDao;
import com.atmko.onmywatch.database.daos.FirebaseUserListDao;
import com.atmko.onmywatch.database.daos.FirebaseWatchListDao;
import com.atmko.onmywatch.database.daos.MovieDataDao;
import com.atmko.onmywatch.database.daos.MovieDataRecordsDao;
import com.atmko.onmywatch.database.daos.MovieNotifierDao;
import com.atmko.onmywatch.database.daos.SearchMediaTagsDao;
import com.atmko.onmywatch.database.daos.SeriesDataDao;
import com.atmko.onmywatch.database.daos.SeriesDataRecordsDao;
import com.atmko.onmywatch.database.daos.SeriesNotifierDao;
import com.atmko.onmywatch.database.daos.UserListsDao;
import com.atmko.onmywatch.database.daos.WatchListsDao;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

@TypeConverters({Converters.class})
public class FirebaseDatabase extends AppDatabase {
    @Override
    public WatchListsDao watchListsDao() {
        return new FirebaseWatchListDao();
    }

    @Override
    public UserListsDao userListsDao() {
        return new FirebaseUserListDao();
    }

    @Override
    public MovieDataDao movieDataDao() {
        return new FirebaseMovieDataDao();
    }

    @Override
    public SeriesDataDao seriesDataDao() {
        return new FirebaseSeriesDataDao();
    }

    @Override
    public MovieDataRecordsDao movieDataRecordsDao() {
        return new FirebaseMovieDataRecordsDao();
    }

    @Override
    public SeriesDataRecordsDao seriesDataRecordsDao() {
        return new FirebaseSeriesDataRecordsDao();
    }

    @Override
    public MovieNotifierDao movieNotifierDao() {
        return new FirebaseMovieNotifiersDao();
    }

    @Override
    public SeriesNotifierDao seriesNotifierDao() {
        return new FirebaseSeriesNotifiersDao();
    }

    //search tags dao only available on local db
    @Override
    public SearchMediaTagsDao searchMediaTagsDao() {
        return null;
    }

    public static DocumentSnapshot getFirstDocument(QuerySnapshot snapshots) {
        DocumentSnapshot documentSnapshot = null;

        //TODO: adding a check for value greater than one can help clean up duplicate database records
        if (snapshots.getDocuments().size() != 0) {
            documentSnapshot = snapshots.getDocuments().get(0);
        }

        return documentSnapshot;
    }

    @NonNull
    @Override
    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration config) {
        return null;
    }

    @NonNull
    @Override
    protected InvalidationTracker createInvalidationTracker() {
        return null;
    }

    @Override
    public void clearAllTables() {
    }
}
