package com.atmko.onmywatch.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaLog;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieLog;
import com.atmko.onmywatch.models.SeriesLog;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.widget.ListWidgetProvider;

import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;

public class LogUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                if (intent == null) return;
                if (!intent.hasExtra(MediaData.MEDIA_TYPE_KEY)) return;
                if (!intent.hasExtra(ApiConstants.ID_KEY)) return;
                if (!intent.hasExtra(MediaNotifier.CONDITION_KEY)) return;

                int mediaType = intent.getIntExtra(MediaData.MEDIA_TYPE_KEY, 0);
                String mediaId = intent.getStringExtra(ApiConstants.ID_KEY);
                int condition = intent.getIntExtra(MediaNotifier.CONDITION_KEY, 0);

                //if condition is new episode, update logs
                if (condition == SeriesNotifier.CONDITION_NEW_EPISODE) {
                    //update logs
                    SeriesTracker.transferUpcomingLogToReleased(context, mediaId);
                    //update widgets
                    ListWidgetProvider.updateWidgets(context);
                    return;
                }

                //if condition is release and is movie, update logs
                if (mediaType == MEDIA_TYPE_MOVIE && condition == MediaNotifier.CONDITION_ON_RELEASE) {
                    //update logs
                    MovieTracker.transferUpcomingLogToReleased(context, mediaId);
                    //update widgets
                    ListWidgetProvider.updateWidgets(context);
                }
            }
        });
    }

    public static void createLogUpdateAlarm(int mediaType, String mediaId, int condition,
                                            long updateTimestamp, Context context) {
        //create pending intent to for log update
        PendingIntent releasePendingIntent =
                MediaLog.createUpdatePendingIntent(context, mediaType, mediaId, condition);

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr != null) {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, updateTimestamp, releasePendingIntent);
        }
    }

    public static void cancelAllLogUpdateAlarms(final Context context) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                AppDatabase database = AppDatabase.getLocalDatabase(context);
                //cancel all movie alarms
                List<MovieLog> movieLogs = database.movieLogsDao().getAllLogsAlt();
                for (MovieLog movieLog: movieLogs) {
                    cancelLogUpdateAlarm(context, movieLog);
                }

                //cancel all series alarms
                List<SeriesLog> seriesLogs = database.seriesLogsDao().getAllLogsAlt();
                for (SeriesLog seriesLog: seriesLogs) {
                    cancelLogUpdateAlarm(context, seriesLog);
                }
            }
        });
    }

    public static void cancelLogUpdateAlarm(Context context, MediaLog mediaLog) {
        AppDatabase database = AppDatabase.getInstance(context);

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent intent = mediaLog.createPendingIntent(context, mediaLog.parentId);
        if (alarmMgr != null) alarmMgr.cancel(intent);

        if (mediaLog instanceof MovieLog) {
            database.movieLogsDao().deleteMediaLog(((MovieLog) mediaLog));

        } else {
            database.seriesLogsDao().deleteMediaLog(((SeriesLog) mediaLog));
        }
    }
}

