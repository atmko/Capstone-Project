package com.atmko.onmywatch.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.daos.MovieDataDao;
import com.atmko.onmywatch.database.daos.SeriesDataDao;
import com.atmko.onmywatch.database.daos.UserListsDao;
import com.atmko.onmywatch.database.daos.MovieDataRecordsDao;
import com.atmko.onmywatch.database.daos.SeriesDataRecordsDao;
import com.atmko.onmywatch.database.daos.WatchListsDao;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;

@Database(entities = {WatchListModel.class, UserListModel.class, MovieData.class, SeriesData.class,
        MovieDataRecord.class, SeriesDataRecord.class},
        version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "on_my_watch_database";
    private static final Object LOCK = new Object();

    private static AppDatabase sInstance;

    public static AppDatabase getInstance(Context context) {
        if (sInstance == null) {
            RoomDatabase.Callback callback = databaseInitializer(context);

            synchronized (LOCK) {
                sInstance = Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
                        //TODO remove allowance of main thread queries
                        .addCallback(callback)
                        .build();
                return sInstance;
            }

        } else {
            return sInstance;
        }
    }

    private static RoomDatabase.Callback databaseInitializer(final Context context) {
        //reference
        //https://medium.com/@srinuraop/database-create-and-open-callbacks-in-room-7ca98c3286ab
        RoomDatabase.Callback callback = new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        String[] seriesWatchListTitles =
                                context.getResources().getStringArray(R.array.watch_status_series_titles);
                        for (String title: seriesWatchListTitles) {
                            WatchListModel watchListModel = new WatchListModel(title);
                            AppDatabase.getInstance(context).watchListsDao().addList(watchListModel);
                        }
                    }
                });
            }

            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
            }
        };

        return callback;
    }

    public abstract WatchListsDao watchListsDao();
    public abstract UserListsDao userListsDao();
    public abstract MovieDataDao movieDataDao();
    public abstract SeriesDataDao seriesDataDao();
    public abstract MovieDataRecordsDao movieDataRecordsDao();
    public abstract SeriesDataRecordsDao seriesDataRecordsDao();
}
