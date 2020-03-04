/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.database.Converters;
import com.atmko.onmywatch.database.daos.FirebaseUserDataDao;
import com.atmko.onmywatch.models.Episode;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaLog;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.ScheduledMedia;
import com.atmko.onmywatch.models.SearchMediaTag;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.SeriesLog;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.MovieApiConstants;
import com.atmko.onmywatch.utils.api_utils.SeriesApiConstants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.database.daos.FirebaseUserDataDao.MIGRATION_LOCAL;
import static com.atmko.onmywatch.models.ListModel.ITEM_COUNT_KEY;
import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;
import static com.atmko.onmywatch.models.MediaData.TAGS_KEY;
import static com.atmko.onmywatch.models.MediaNotifier.CONDITION_KEY;
import static com.atmko.onmywatch.models.MediaNotifier.IS_ACTIVE_KEY;
import static com.atmko.onmywatch.models.MediaNotifier.NOTIFIER_ID_KEY;
import static com.atmko.onmywatch.models.MovieData.SCHEDULED_MEDIA_KEY;
import static com.atmko.onmywatch.models.SeriesData.NEXT_EPISODE_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.ID_KEY;

@SuppressWarnings("unchecked")
public class RestoreService extends JobIntentService {
    public static final int JOB_ID = 11;

    public static final String BACKUP_CHANNEL_ID = "Backup Channel";
    public static final String BACKUP_NAME = "backup";
    public static final String BACKUP_LOCAL_PATH = "backups";

    public static final String MOVIES_KEY = "movies";
    public static final String SERIES_KEY = "series";
    public static final String WATCH_LISTS_KEY = "watch_lists";
    public static final String USER_LISTS_KEY = "user_lists";
    public static final String MOVIE_DATA_RECORDS_KEY = "movie_data_records";
    public static final String SERIES_DATA_RECORDS_KEY = "series_data_records";
    public static final String MOVIES_NOTIFIERS_KEY = "movie_notifiers";
    public static final String SERIES_NOTIFIERS_KEY = "series_notifiers";
    public static final String SERIES_LOGS_KEY = "series_logs";

    private AppDatabase mLocalDatabase;
    private StorageReference mBackupRef;
    private String mJsonString;

    private OnRestoreCompleteListener mOnRestoreCompleteListener;

    public RestoreService() {
    }

    public interface  OnRestoreCompleteListener {
        void onRestoreComplete();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLocalDatabase = AppDatabase.getLocalDatabase(getApplicationContext());
        mBackupRef = FirebaseStorage.getInstance().getReference()
                .child(BACKUP_LOCAL_PATH + "/" + BACKUP_NAME + ".json");

        startForeground(JOB_ID,
                buildNotification(getApplicationContext(),
                        getString(R.string.notification_migration_title),
                        getString(R.string.notification_free_migration_content)
                )
        );
    }

    public static void enqueueWork(Context appContext, Intent intent) {
        enqueueWork(appContext, RestoreService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        restoreBackup();
    }

    public Notification buildNotification(Context context, String notificationTitle, String notificationContent) {
        //create intent to launch activity on click
        Intent intent = new Intent(context, MasterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        return new NotificationCompat.Builder(context, BACKUP_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_rate)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }

    private void restoreBackup() {
        mBackupRef.getStream(new StreamDownloadTask.StreamProcessor() {
            @Override
            public void doInBackground(@NonNull StreamDownloadTask.TaskSnapshot taskSnapshot,
                                       @NonNull InputStream inputStream) {
                try {
                    mJsonString = readFullyAsString(inputStream, "UTF-8");
                    if (mJsonString.equals("")) return;
                    deleteLocallySavedData();

                    pullMovieData();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle failed download
                // ...
            }
        });
    }

    public String readFullyAsString(InputStream inputStream, String encoding)
            throws IOException {
        return readFully(inputStream).toString(encoding);
    }

    private ByteArrayOutputStream readFully(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos;
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
                FirebaseUserDataDao.setMigrationValue(MIGRATION_LOCAL);
                finishService();
            }
        });
    }

    private void finishService() {
        stopForeground(true);
        stopSelf();
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    static MovieData parseDataMapToMovieData(Map map) {
        ScheduledMedia scheduledMedia = map.get(SCHEDULED_MEDIA_KEY) == null ? null
                : Converters.longToScheduledMedia(((Double) map.get(SCHEDULED_MEDIA_KEY)).longValue());

        List<String> tagStrings = (ArrayList<String>) map.get(TAGS_KEY);
        List<SearchMediaTag> searchTags = new ArrayList<>();
        for (String tagString: tagStrings) {
            searchTags.add(new SearchMediaTag(tagString));
        }

        MovieData movieData = new MovieData(
                (String) map.get(ID_KEY),
                ((String) map.get(TraktApiConstants.TRAKT_ID_KEY)),
                (String) map.get(ApiConstants.VOTE_AVERAGE_KEY),
                (String) map.get(MovieApiConstants.TITLE_KEY),
                (String) map.get(ApiConstants.POSTER_PATH_KEY),
                (String) map.get(ApiConstants.ORIG_LANG_KEY),
                (String) map.get(MovieApiConstants.ORIG_TITLE_KEY),
                (ArrayList<String>) map.get(ApiConstants.GENRES_KEY),
                (boolean) map.get(MovieApiConstants.ADULT_KEY),
                (String) map.get(ApiConstants.BACKDROP_PATH_KEY),
                (String) map.get(ApiConstants.OVERVIEW_KEY),
                (String) map.get(MovieApiConstants.RELEASE_DATE_KEY),
                (String) map.get(MovieApiConstants.CERTIFICATION_KEY),
                (String) map.get(ApiConstants.RELEASE_STATUS_KEY),
                scheduledMedia,
                searchTags
        );

        movieData.setWatchStatus(
                ((Double) map.get(MediaData.WATCH_STATUS_KEY)).intValue());
        movieData.setUserRating(
                ((Double) map.get(MediaData.USER_RATING_KEY)).intValue());

        return movieData;
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    static SeriesData parseDataMapToSeriesData(Map map) {
        Episode episode = map.get(NEXT_EPISODE_KEY) == null ? null
                : Converters.stringToEpisode((String) map.get(NEXT_EPISODE_KEY));

        List<String> tagStrings = (ArrayList<String>) map.get(TAGS_KEY);
        List<SearchMediaTag> searchTags = new ArrayList<>();
        for (String tagString: tagStrings) {
            searchTags.add(new SearchMediaTag(tagString));
        }

        SeriesData seriesData = new SeriesData(
                (String) map.get(ID_KEY),
                ((String) map.get(TraktApiConstants.TRAKT_ID_KEY)),
                (String) map.get(ApiConstants.VOTE_AVERAGE_KEY),
                (String) map.get(SeriesApiConstants.NAME_KEY),
                (String) map.get(ApiConstants.POSTER_PATH_KEY),
                (String) map.get(ApiConstants.ORIG_LANG_KEY),
                (String) map.get(SeriesApiConstants.ORIG_NAME_KEY),
                (ArrayList<String>) map.get(SeriesApiConstants.ORIGIN_COUNTRY_KEY),
                (ArrayList<String>) map.get(ApiConstants.GENRES_KEY),
                (String) map.get(ApiConstants.BACKDROP_PATH_KEY),
                (String) map.get(ApiConstants.OVERVIEW_KEY),
                (String) map.get(SeriesApiConstants.FIRST_AIR_DATE_KEY),
                (String) map.get(SeriesApiConstants.RATING_KEY),
                (String) map.get(ApiConstants.RELEASE_STATUS_KEY),
                episode,
                searchTags
        );

        seriesData.setWatchStatus(
                ((Double) map.get(MediaData.WATCH_STATUS_KEY)).intValue());
        seriesData.setUserRating(
                ((Double) map.get(MediaData.USER_RATING_KEY)).intValue());

        return seriesData;
    }

    @SuppressWarnings("ConstantConditions")
    private static WatchListModel parseWatchListModel(Map map) {
        String listName = ((String) map.get(LIST_NAME_KEY));
        int listCount = ((Double) map.get(ITEM_COUNT_KEY)).intValue();

        return new WatchListModel(listName, listCount);
    }

    @SuppressWarnings("ConstantConditions")
    public static UserListModel parseUserListModel(Map map) {
        String listName = ((String) map.get(LIST_NAME_KEY));
        int listCount = ((Double) map.get(ITEM_COUNT_KEY)).intValue();

        return new UserListModel(listName, listCount);
    }

    @SuppressWarnings("ConstantConditions")
    private static MovieDataRecord parseMovieRecord(Map map) {
        return new MovieDataRecord(
                (String) map.get(ID_KEY),
                (String) map.get(LIST_NAME_KEY)
        );
    }

    @SuppressWarnings("ConstantConditions")
    private static SeriesDataRecord parseSeriesRecord(Map map) {
        return new SeriesDataRecord(
                (String) map.get(ID_KEY),
                (String) map.get(LIST_NAME_KEY)
        );
    }

    @SuppressWarnings("ConstantConditions")
    private static MovieNotifier parseMovieNotifier(Map map) {
        return new MovieNotifier(
                (String) map.get(NOTIFIER_ID_KEY),
                ((Double) map.get(CONDITION_KEY)).intValue(),
                ((boolean) map.get(IS_ACTIVE_KEY))
        );
    }

    @SuppressWarnings("ConstantConditions")
    private static SeriesNotifier parseSeriesNotifier(Map map) {
        return new SeriesNotifier(
                (String) map.get(NOTIFIER_ID_KEY),
                ((Double) map.get(CONDITION_KEY)).intValue(),
                ((boolean) map.get(IS_ACTIVE_KEY))
        );
    }

    private static SeriesLog parseSeriesLog(Map map) {
        Double seasonNumberDouble = (Double) map.get(SeriesLog.SEASON_NUMBER_KEY);
        Double episodeNumberDouble = (Double) map.get(SeriesLog.EPISODE_NUMBER_KEY);
        Double conditionDouble = (Double) map.get(SeriesLog.CONDITION_KEY);
        Double timestampDouble = (Double) map.get(SeriesLog.TIMESTAMP_KEY);
        int seasonNumber = seasonNumberDouble != null? seasonNumberDouble.intValue(): 0;
        int episodeNumber = episodeNumberDouble != null? episodeNumberDouble.intValue(): 0;
        int condition = conditionDouble != null? conditionDouble.intValue(): 0;
        int timestamp = timestampDouble != null? timestampDouble.intValue(): 0;

        return new SeriesLog(
                (String) map.get(MediaLog.TYPE_KEY),
                seasonNumber,
                episodeNumber,
                condition,
                timestamp,
                (String) map.get(MediaLog.TITLE_KEY),
                (String) map.get(MediaLog.POSTER_PATH_KEY),
                (String) map.get(MediaLog.PARENT_ID_KEY),
                (boolean) map.get(SeriesLog.IS_BUNDLED_KEY)
        );
    }

    //pull remotely saved movies to local database
    private void pullMovieData() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> movieMaps = (List<Map>) returnedMap.get(MOVIES_KEY);
        if (movieMaps == null) return;

        for (Map map: movieMaps) {
            MovieData movieData = parseDataMapToMovieData(map);
            mLocalDatabase.movieDataDao().addMovieData(movieData);
        }

        onPullMovieDataComplete();
    }

    //pull remotely saved series to local database
    private void pullSeriesData() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> seriesMaps = (List<Map>) returnedMap.get(SERIES_KEY);
        if (seriesMaps == null) return;

        for (Map map: seriesMaps) {
            SeriesData seriesData = parseDataMapToSeriesData(map);
            mLocalDatabase.seriesDataDao().addSeriesData(seriesData);
        }

        onPullSeriesDataComplete();
    }

    //pull remotely saved watch lists to local database
    private void pullWatchLists() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> listMaps = (List<Map>) returnedMap.get(WATCH_LISTS_KEY);
        if (listMaps == null) return;

        for (Map map: listMaps) {
            WatchListModel watchListModel = parseWatchListModel(map);
            mLocalDatabase.watchListsDao().addList(watchListModel);
        }

        onPullWatchListsComplete();
    }

    //pull remotely saved user lists to local database
    private void pullUserLists() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> listMaps = (List<Map>) returnedMap.get(USER_LISTS_KEY);
        if (listMaps == null) return;

        for (Map map: listMaps) {
            UserListModel userListModel = parseUserListModel(map);
            mLocalDatabase.userListsDao().addList(userListModel);
        }

        onPullUserListsComplete();
    }

    //pull remotely saved movie data records to local database
    private void pullMovieDataRecords() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> recordMaps = (List<Map>) returnedMap.get(MOVIE_DATA_RECORDS_KEY);
        if (recordMaps == null) return;

        for (Map map: recordMaps) {
            MovieDataRecord movieDataRecord = parseMovieRecord(map);
            mLocalDatabase.movieDataRecordsDao().addRecord(movieDataRecord);
        }

        onPullMovieDataRecordsComplete();
    }

    //pull remotely saved series data records to local database
    private void pullSeriesDataRecords() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> recordMaps = (List<Map>) returnedMap.get(SERIES_DATA_RECORDS_KEY);
        if (recordMaps == null) return;

        for (Map map: recordMaps) {
            SeriesDataRecord seriesDataRecord = parseSeriesRecord(map);
            mLocalDatabase.seriesDataRecordsDao().addRecord(seriesDataRecord);
        }

        onPullSeriesDataRecordsComplete();
    }

    //pull remotely saved movie notifiers to local database
    private void pullMovieNotifiers() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> recordMaps = (List<Map>) returnedMap.get(MOVIES_NOTIFIERS_KEY);
        if (recordMaps == null) return;

        for (Map map: recordMaps) {
            MovieNotifier movieNotifier = parseMovieNotifier(map);
            mLocalDatabase.movieNotifierDao().addMediaNotifier(movieNotifier);
        }

        onPullMovieNotifiersComplete();
    }

    //pull remotely saved series notifiers to local database
    private void pullSeriesNotifiers() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> notifierMaps = (List<Map>) returnedMap.get(SERIES_NOTIFIERS_KEY);
        if (notifierMaps == null) return;

        for (Map map: notifierMaps) {
            SeriesNotifier seriesNotifier = parseSeriesNotifier(map);
            mLocalDatabase.seriesNotifierDao().addMediaNotifier(seriesNotifier);
        }

        onPullSeriesNotifiersComplete();
    }

    //pull remotely saved series logs to local database
    private void pullSeriesLogs() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> recordMaps = (List<Map>) returnedMap.get(SERIES_LOGS_KEY);
        if (recordMaps == null) return;

        for (Map map: recordMaps) {
            SeriesLog seriesLog = parseSeriesLog(map);
            mLocalDatabase.seriesLogsDao().addMediaLog(seriesLog);
        }

        onPullSeriesLogsComplete();
    }

    private void deleteLocallySavedData() {
        List<MovieDataRecord> localMovieDataRecords =
                mLocalDatabase.movieDataRecordsDao().getAllRecordsAlt();
        for (MovieDataRecord movieDataRecord: localMovieDataRecords) {
            mLocalDatabase.movieDataRecordsDao().deleteRecord(movieDataRecord);
        }

        List<SeriesDataRecord> localSeriesDataRecords =
                mLocalDatabase.seriesDataRecordsDao().getAllRecordsAlt();
        for (SeriesDataRecord seriesDataRecord: localSeriesDataRecords) {
            mLocalDatabase.seriesDataRecordsDao().deleteRecord(seriesDataRecord);
        }

        List<MovieData> localMovieDataList =
                mLocalDatabase.movieDataDao().getAllMoviesAlt();
        for (MovieData movieData: localMovieDataList) {
            mLocalDatabase.movieDataDao().deleteMovieData(movieData);
        }

        List<SeriesData> localSeriesDataList =
                mLocalDatabase.seriesDataDao().getAllSeriesAlt();
        for (SeriesData seriesData: localSeriesDataList) {
            mLocalDatabase.seriesDataDao().deleteSeriesData(seriesData);
        }

        List<WatchListModel> localWatchLists =
                mLocalDatabase.watchListsDao().getAllListsAlt();
        for (WatchListModel watchListModel: localWatchLists) {
            mLocalDatabase.watchListsDao().deleteList(watchListModel);
        }

        List<UserListModel> localUserLists =
                mLocalDatabase.userListsDao().getAllListsAlt();
        for (UserListModel userListModel: localUserLists) {
            mLocalDatabase.userListsDao().deleteList(userListModel);
        }

        List<MovieNotifier> localMovieNotifiers =
                mLocalDatabase.movieNotifierDao().getAllNotifiersAlt();
        for (MovieNotifier movieNotifier: localMovieNotifiers) {
            mLocalDatabase.movieNotifierDao().deleteNotifier(movieNotifier);
        }

        List<SeriesNotifier> localSeriesNotifiers =
                mLocalDatabase.seriesNotifierDao().getAllNotifiersAlt();
        for (SeriesNotifier seriesNotifier: localSeriesNotifiers) {
            mLocalDatabase.seriesNotifierDao().deleteNotifier(seriesNotifier);
        }

        List<SeriesLog> localSeriesLogs =
                mLocalDatabase.seriesLogsDao().getAllLogsAlt();
        for (SeriesLog seriesLog: localSeriesLogs) {
            mLocalDatabase.seriesLogsDao().deleteMediaLog(seriesLog);
        }
    }}