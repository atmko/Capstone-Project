/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.database.daos.MovieLogsDao;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieLog;
import com.atmko.onmywatch.models.NotificationIdlingResource;
import com.atmko.onmywatch.models.ScheduledMedia;

import org.parceler.Parcels;

import java.util.List;

import static com.atmko.onmywatch.models.MovieLog.CONDITION_AIRED;
import static com.atmko.onmywatch.models.MovieLog.CONDITION_UNDATED;
import static com.atmko.onmywatch.models.MovieLog.CONDITION_UPCOMING;
import static com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker.NEW_MEDIA_DATA_KEY;

public class MovieTracker extends JobIntentService {
    private static final String ACTION_TESTING = "testing";
    public static final String ACTION_SET = "set";
    public static final String ACTION_DELETE = "delete";

    private static final int JOB_ID = 41;

    public static String sActionMode = ACTION_SET;

    private AppDatabase mDatabase;
    private MovieData newMediaData;

    public static void enqueueWork(Context appContext, Intent intent) {
        //set idle state to false
        if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
            NotificationIdlingResource.getNotificationIdlingResource().setIdleState(false);
        }

        enqueueWork(appContext, MovieTracker.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        newMediaData = Parcels.unwrap(intent.getParcelableExtra(NEW_MEDIA_DATA_KEY));
        mDatabase = AppDatabase.getInstance(getApplicationContext());

        if (sActionMode.equals(ACTION_SET) || sActionMode.equals(ACTION_TESTING)) {
            if (newMediaData.getWatchStatus() == MediaData.WATCH_STATUS_TO_WATCH
                    || newMediaData.getWatchStatus() == MediaData.WATCH_STATUS_WATCHING) {
                trackMovieMedia();
            }
        } else if (sActionMode.equals(ACTION_DELETE)) {
            if (newMediaData.getWatchStatus() != MediaData.WATCH_STATUS_WATCHING) {
                deleteTrackedMedia();
            }
        }
    }

    private void trackMovieMedia() {
        deleteTrackedMedia();

        //set upcoming movie if air date available else set without air date
        ScheduledMedia scheduledMedia = newMediaData.getScheduledMedia();
        String airDate = scheduledMedia.getBestAvailableDateString();

        if (airDate != null && !airDate.equals("")) {
            if (scheduledMedia.isInFuture()) {
                //set movie upcoming
                insertLog(CONDITION_UPCOMING, scheduledMedia.getTimestamp());

            } else {
                //set movie aired
                insertLog(CONDITION_AIRED, scheduledMedia.getTimestamp());
            }

        } else {
            //set movie undated
            insertLog(CONDITION_UNDATED, scheduledMedia.getTimestamp());
        }
    }

    private void insertLog(int condition, long timestamp) {
        MovieLog mediaLog = new MovieLog(condition, timestamp, newMediaData.getTitle(),
                newMediaData.getPosterPath(), newMediaData.getBackdropPath(), newMediaData.getId());

        mDatabase.movieLogsDao().addMediaLog(mediaLog);
    }

    //deletes the upcoming log and updates upcoming log variable condition to CONDITION_AIRED
    //deletes the upcoming log from database
    //writes upcoming log(now with CONDITION_AIRED) to database, by creating or updating existing
    public static void transferUpcomingLogToReleased(Context context, String mediaId) {
        MovieLogsDao logsDao = AppDatabase.getLocalDatabase(context).movieLogsDao();

        MovieLog upcomingLog = logsDao.getLog(mediaId, CONDITION_UPCOMING);
        if (upcomingLog != null) {
            logsDao.deleteMediaLog(upcomingLog);
            upcomingLog.condition = CONDITION_AIRED;

            MovieLog lastAiredLog = logsDao.getLog(mediaId, CONDITION_AIRED);
            if (lastAiredLog != null) {
                logsDao.updateLog(upcomingLog);

            } else {
                logsDao.addMediaLog(upcomingLog);
            }
        }
    }

    private void deleteTrackedMedia() {
        List<MovieLog> mediaLogs =
                mDatabase.movieLogsDao().getAllLogsWithMediaIdAlt(newMediaData.getId());

        for (MovieLog mediaLog : mediaLogs) {
            mDatabase.movieLogsDao().deleteMediaLog(mediaLog);
        }
    }
}