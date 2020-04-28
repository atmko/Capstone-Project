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
import com.atmko.onmywatch.database.Converters;
import com.atmko.onmywatch.models.Episode;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaLog;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.MovieLog;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.ScheduledMedia;
import com.atmko.onmywatch.models.SearchListTag;
import com.atmko.onmywatch.models.SearchMediaTag;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.SeriesLog;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.NotificationHandler;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.MovieApiConstants;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
import com.atmko.onmywatch.utils.api_utils.SeriesApiConstants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private static final String TAG = RestoreService.class.getSimpleName();

    private static final int JOB_ID = 11;

    private static final String BACKUP_CHANNEL_ID = "Backup Channel";

    public static final String FOLDER_KEY = "folder";
    public static final String FILENAME_KEY = "file_name";

    public static final String BACKUP_FOLDER_NAME = "backups";

    private static final String USERS_PATH = "users";

    private static final String MOVIES_KEY = "movies";
    private static final String SERIES_KEY = "series";
    private static final String WATCH_LISTS_KEY = "watch_lists";
    private static final String USER_LISTS_KEY = "user_lists";
    private static final String MOVIE_DATA_RECORDS_KEY = "movie_data_records";
    private static final String SERIES_DATA_RECORDS_KEY = "series_data_records";
    private static final String MOVIES_NOTIFIERS_KEY = "movie_notifiers";
    private static final String SERIES_NOTIFIERS_KEY = "series_notifiers";
    private static final String MOVIE_LOGS_KEY = "movie_logs";
    private static final String SERIES_LOGS_KEY = "series_logs";

    private AppDatabase mLocalDatabase;
    private StorageReference mBackupRef;
    private String mJsonString;
    private boolean isRestoreSuccessful;

    private static OnRestoreCompleteListener mOnRestoreCompleteListener;

    public RestoreService() {
    }

    public interface OnRestoreCompleteListener {
        void onRestoreComplete();
        void onRestoreFailed();
    }

    public static void enqueueWork(Context appContext, Intent intent) {
        mOnRestoreCompleteListener = ((OnRestoreCompleteListener) appContext);
        enqueueWork(appContext, RestoreService.class, JOB_ID, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalDatabase = AppDatabase.getLocalDatabase(getApplicationContext());
        startForeground(JOB_ID,
                buildNotification(getApplicationContext(),
                        getString(R.string.notification_restore_title),
                        getString(R.string.notification_restore_content)
                )
        );
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String folder = intent.getStringExtra(FOLDER_KEY);
        String mFileName = intent.getStringExtra(FILENAME_KEY);

        boolean isFolderExists = folder != null && !folder.equals("");
        boolean isFileNameExists = mFileName != null && !mFileName.equals("");
        Log.d(TAG, isFolderExists+"");
        Log.d(TAG, isFileNameExists+"");
        if (isFolderExists && isFileNameExists) {
            mBackupRef = FirebaseStorage.getInstance().getReference()
                    .child(USERS_PATH + "/" + MasterActivity.getCurrentUser().getUid()
                            + "/" + folder + "/" + mFileName);
            restoreBackup();
            Log.d(TAG, mBackupRef.getPath());

        } else {
            Log.d(TAG, "folder and or file name does't exist");
            respondWithFailure();
        }
    }

    private Notification buildNotification(Context context, String notificationTitle, String notificationContent) {
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

    private void restoreBackup() {
        //if not online, respond with failure
        if (!NetworkFunctions.isOnline()) {
            respondWithFailure();
            return;
        }

        mBackupRef.getStream(new StreamDownloadTask.StreamProcessor() {
            @Override
            public void doInBackground(@NonNull StreamDownloadTask.TaskSnapshot taskSnapshot,
                                       @NonNull InputStream inputStream) {
                try {
                    mJsonString = readFullyAsString(inputStream);
                    if (mJsonString.equals("")) return;
                    AppDatabase.deleteLocallySavedData(getApplicationContext());

                    pullMovieData();

                } catch (Exception e) {
                    e.printStackTrace();
                    respondWithFailure();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                respondWithFailure();
            }
        });
    }

    private String readFullyAsString(InputStream inputStream)
            throws IOException {
        return readFully(inputStream).toString("UTF-8");
    }

    private ByteArrayOutputStream readFully(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            stream.write(buffer, 0, length);
        }
        return stream;
    }

    private void onPullMovieDataComplete() {
        pullSeriesData();
    }

    private void onPullSeriesDataComplete() {
        pullWatchLists();
    }

    private void onPullWatchListsComplete() {
        pullUserLists();
    }

    private void onPullUserListsComplete() {
        pullMovieDataRecords();
    }

    private void onPullMovieDataRecordsComplete() {
        pullSeriesDataRecords();
    }

    private void onPullSeriesDataRecordsComplete() {
        pullMovieNotifiers();
    }

    private void onPullMovieNotifiersComplete() {
        pullSeriesNotifiers();
    }

    private void onPullSeriesNotifiersComplete() {
        restoreNotifications();
    }

    private void onRestoreNotificationsComplete() {
        pullMovieLogs();
    }

    private void onPullMovieLogsComplete() {
        pullSeriesLogs();
    }

    private void onPullSeriesLogsComplete() {
        //update success value fail and finish service
        isRestoreSuccessful = true;
        finishService();
    }

    private void respondWithFailure() {
        //update success value fail, delete local database files and finish service
        isRestoreSuccessful = false;
        AppDatabase.deleteLocallySavedData(getApplicationContext());
        finishService();
    }

    private void finishService() {
        stopForeground(true);
        stopSelf();

        if (isRestoreSuccessful) {
            Log.d(TAG, "restore success");
            mOnRestoreCompleteListener.onRestoreComplete();

        } else {
            Log.d(TAG, "restore failed");
            mOnRestoreCompleteListener.onRestoreFailed();
        }
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private static MovieData parseDataMapToMovieData(Map map) {
        ScheduledMedia scheduledMedia = map.get(SCHEDULED_MEDIA_KEY) == null ? null
                : Converters.longToScheduledMedia(((Double) map.get(SCHEDULED_MEDIA_KEY)).longValue());

        List<String> tagStrings = (ArrayList<String>) map.get(TAGS_KEY);
        List<SearchMediaTag> searchTags = new ArrayList<>();
        for (String tagString : tagStrings) {
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
    private static SeriesData parseDataMapToSeriesData(Map map) {
        Episode episode = map.get(NEXT_EPISODE_KEY) == null ? null
                : Converters.stringToEpisode((String) map.get(NEXT_EPISODE_KEY));

        List<String> tagStrings = (ArrayList<String>) map.get(TAGS_KEY);
        List<SearchMediaTag> searchTags = new ArrayList<>();
        for (String tagString : tagStrings) {
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
                searchTags,
                (String) map.get(SeriesApiConstants.NETWORK_KEY)
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
    private static UserListModel parseUserListModel(Map map) {
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

    private static MovieLog parseMovieLog(Map map) {
        Double conditionDouble = (Double) map.get(MediaLog.CONDITION_KEY);
        Double timestampDouble = (Double) map.get(MediaLog.TIMESTAMP_KEY);
        int condition = conditionDouble != null ? conditionDouble.intValue() : 0;
        long timestamp = timestampDouble != null ? timestampDouble.longValue() : 0;

        return new MovieLog(
                condition,
                timestamp,
                (String) map.get(MediaLog.TITLE_KEY),
                (String) map.get(MediaLog.POSTER_PATH_KEY),
                (String) map.get(MediaLog.BACKDROP_PATH_KEY),
                (String) map.get(MediaLog.PARENT_ID_KEY)
        );
    }

    private static SeriesLog parseSeriesLog(Map map) {
        Double seasonNumberDouble = (Double) map.get(SeriesLog.SEASON_NUMBER_KEY);
        Double episodeNumberDouble = (Double) map.get(SeriesLog.EPISODE_NUMBER_KEY);
        Double conditionDouble = (Double) map.get(MediaLog.CONDITION_KEY);
        Double timestampDouble = (Double) map.get(MediaLog.TIMESTAMP_KEY);
        int seasonNumber = seasonNumberDouble != null ? seasonNumberDouble.intValue() : 0;
        int episodeNumber = episodeNumberDouble != null ? episodeNumberDouble.intValue() : 0;
        int condition = conditionDouble != null ? conditionDouble.intValue() : 0;
        long timestamp = timestampDouble != null ? timestampDouble.longValue() : 0;

        return new SeriesLog(
                (String) map.get(SeriesLog.TYPE_KEY),
                seasonNumber,
                episodeNumber,
                condition,
                timestamp,
                (String) map.get(MediaLog.TITLE_KEY),
                (String) map.get(MediaLog.POSTER_PATH_KEY),
                (String) map.get(MediaLog.BACKDROP_PATH_KEY),
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

        for (Map map : movieMaps) {
            MovieData movieData = parseDataMapToMovieData(map);
            mLocalDatabase.movieDataDao().addMovieData(movieData);
            //restore search tags
            restoreSearchMediaTags(movieData.searchTags);
        }

        onPullMovieDataComplete();
    }

    //pull remotely saved series to local database
    private void pullSeriesData() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> seriesMaps = (List<Map>) returnedMap.get(SERIES_KEY);
        if (seriesMaps == null) return;

        for (Map map : seriesMaps) {
            SeriesData seriesData = parseDataMapToSeriesData(map);
            mLocalDatabase.seriesDataDao().addSeriesData(seriesData);
            //restore search tags
            restoreSearchMediaTags(seriesData.searchTags);
        }

        onPullSeriesDataComplete();
    }

    private void restoreSearchMediaTags(List<SearchMediaTag> searchMediaTags) {
        for (SearchMediaTag tag: searchMediaTags) {
            SearchMediaTag savedTag = mLocalDatabase.searchMediaTagsDao().getTagAlt(tag.mTag);

            if (savedTag == null) {
                mLocalDatabase.searchMediaTagsDao().addTag(tag);
            }
        }
    }

    //pull remotely saved watch lists to local database
    private void pullWatchLists() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> listMaps = (List<Map>) returnedMap.get(WATCH_LISTS_KEY);
        if (listMaps == null) return;

        String noneWatchListName = getApplicationContext()
                .getResources().getStringArray(R.array.watch_status_titles)[0];

        for (Map map : listMaps) {
            WatchListModel watchListModel = parseWatchListModel(map);
            if (watchListModel.getName().equals(noneWatchListName)) continue;
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

        for (Map map : listMaps) {
            UserListModel userListModel = parseUserListModel(map);
            mLocalDatabase.userListsDao().addList(userListModel);
            //restore search tags
            restoreSearchListTags(userListModel);
        }

        onPullUserListsComplete();
    }

    private void restoreSearchListTags(ListModel listModel) {
        SearchListTag savedTag = mLocalDatabase.searchListTagsDao()
                .getTagAlt(listModel.getName().toLowerCase());

        if (savedTag == null) {
            mLocalDatabase.searchListTagsDao().addTag(new SearchListTag(listModel.getName()));
        }
    }

    //pull remotely saved movie data records to local database
    private void pullMovieDataRecords() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> recordMaps = (List<Map>) returnedMap.get(MOVIE_DATA_RECORDS_KEY);
        if (recordMaps == null) return;

        for (Map map : recordMaps) {
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

        for (Map map : recordMaps) {
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

        for (Map map : recordMaps) {
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

        for (Map map : notifierMaps) {
            SeriesNotifier seriesNotifier = parseSeriesNotifier(map);
            mLocalDatabase.seriesNotifierDao().addMediaNotifier(seriesNotifier);
        }

        onPullSeriesNotifiersComplete();
    }

    //pull remotely saved movie logs to local database
    private void pullMovieLogs() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> recordMaps = (List<Map>) returnedMap.get(MOVIE_LOGS_KEY);
        if (recordMaps == null) return;

        for (Map map : recordMaps) {
            MovieLog movieLog = parseMovieLog(map);
            mLocalDatabase.movieLogsDao().addMediaLog(movieLog);
        }

        onPullMovieLogsComplete();
    }

    //pull remotely saved series logs to local database
    private void pullSeriesLogs() {
        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(mJsonString, Map.class);

        List<Map> recordMaps = (List<Map>) returnedMap.get(SERIES_LOGS_KEY);
        if (recordMaps == null) return;

        for (Map map : recordMaps) {
            SeriesLog seriesLog = parseSeriesLog(map);
            mLocalDatabase.seriesLogsDao().addMediaLog(seriesLog);
        }

        onPullSeriesLogsComplete();
    }

    //restore all movie and series notifications
    private void restoreNotifications() {
        NotificationHandler.restoreNotifiers(getApplicationContext());
        onRestoreNotificationsComplete();
    }
}