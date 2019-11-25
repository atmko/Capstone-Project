package com.atmko.onmywatch.utils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationManagerCompat;

import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;

import java.util.Calendar;
import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;


public class NotificationHandler {
    public static class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    if (intent == null) return;
                    if (!intent.hasExtra(MediaData.MEDIA_TYPE_KEY)) return;
                    if (!intent.hasExtra(ApiConstants.ID_KEY)) return;
                    if (!intent.hasExtra(MediaNotifier.CONDITION_KEY)) return;
                    if (!intent.hasExtra(MediaNotifier.NOTIFICATIONS_KEY)) return;

                    int mediaType = intent.getIntExtra(MediaData.MEDIA_TYPE_KEY, 0);
                    String mediaId = intent.getStringExtra(ApiConstants.ID_KEY);
                    int condition = intent.getIntExtra(MediaNotifier.CONDITION_KEY, 0);
                    Notification notification = intent.getParcelableExtra(MediaNotifier.NOTIFICATIONS_KEY);
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    //and media id and condition as unique ids
                    //show notification
                    notificationManager.notify(mediaId, condition, notification);
                    //remove media notifier from the database
                    removeMediaNotifier(context, mediaType, mediaId, condition);
                }
            });
        }

        private void removeMediaNotifier(Context context, int mediaType, String mediaId, int condition) {
            AppDatabase database = AppDatabase.getInstance(context);
            MediaNotifier notifier;

            if (mediaType == MEDIA_TYPE_MOVIE) {
                notifier = database.movieNotifierDao().getNotifierByIdAlt(mediaId, condition);
                database.movieNotifierDao().deleteNotifier(((MovieNotifier) notifier));

            } else {
                notifier = database.seriesNotifierDao().getNotifierByIdAlt(mediaId, condition);
                database.seriesNotifierDao().deleteNotifier(((SeriesNotifier) notifier));
            }
        }
    }

    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            //TODO: get action never null (receiver has filter for Intent.ACTION_BOOT_COMPLETED only)
            //noinspection ConstantConditions
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                restoreNotifiers(context);
            }
        }
    }

    public static class TimeChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //TODO: get action never null (receiver has filter for Intent.ACTION_TIME_CHANGED only)
            //noinspection ConstantConditions
            if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)) {
                restoreNotifiers(context);
            }
        }
    }

    private static void restoreNotifiers(final Context context) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                AppDatabase database = AppDatabase.getInstance(context);

                //get media notifiers
                setMovieNotifiers(context, database);
                setSeriesNotifiers(context, database);
            }
        });
    }

    private static void setMovieNotifiers(Context context, AppDatabase database) {
        List<MovieNotifier> notifiers = database.movieNotifierDao().getAllNotifiersAlt();

        for (MovieNotifier notifier: notifiers) {
            MediaData mediaData = database.movieDataDao().getMovieByIdAlt(notifier.getMediaId());

            if (notifier.getCondition() == MediaNotifier.CONDITION_ON_RELEASE) {
                //skip if release date is empty
                if (mediaData.getReleaseDate().equals("")) continue;

                scheduleReleaseNotification(context, mediaData, notifier);
            }
        }
    }

    private static void setSeriesNotifiers(Context context, AppDatabase database) {
        List<SeriesNotifier> notifiers = database.seriesNotifierDao().getAllNotifiersAlt();

        for (SeriesNotifier notifier: notifiers) {
            MediaData mediaData = database.seriesDataDao().getSeriesByIdAlt(notifier.getMediaId());

            if (notifier.getCondition() == MediaNotifier.CONDITION_ON_RELEASE) {
                //skip if release date is empty
                if (mediaData.getReleaseDate().equals("")) continue;

                scheduleReleaseNotification(context, mediaData, notifier);
            }
        }
    }

    public static void scheduleReleaseNotification(Context context, MediaData mediaData,
                                                   MediaNotifier notifier) {
        int mediaType;

        Notification notification = notifier.createNotification(
                context, MediaNotifier.CONDITION_ON_RELEASE, mediaData.getTitle());

        if (mediaData instanceof MovieData) {
            mediaType = MEDIA_TYPE_MOVIE;

        } else {
            mediaType = MEDIA_TYPE_SERIES;
        }

        //create pending intent to house notification for when alarm is triggered
        PendingIntent releasePendingIntent =
                notifier.createPendingIntent(context, mediaType, mediaData.getId(), notification);

        //configure calender
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int[] releaseDateArray =
                GeneralUtils.separateDateToIntegers(mediaData.getReleaseDate());
        calendar.set(releaseDateArray[0], releaseDateArray[1], releaseDateArray[2]);

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), releasePendingIntent);

        enableBootReceiver(context);
    }

    //deletes notifiers and cancel alarm notifications
    public static void cancelAlarms(Context context, List<MediaNotifier> notifiers) {
        AppDatabase database = AppDatabase.getInstance(context);

        for (MediaNotifier notifier: notifiers) {
            AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent intent = notifier.createPendingIntent(context);
            alarmMgr.cancel(intent);

            if (notifier instanceof MovieNotifier) {
                database.movieNotifierDao().deleteNotifier(((MovieNotifier) notifier));

            } else {
                database.seriesNotifierDao().deleteNotifier(((SeriesNotifier) notifier));
            }
        }

        //disable boot receiver if there are no notifiers in the database

        List<MovieNotifier> movieNotifiers = database.movieNotifierDao().getAllNotifiersAlt();
        List<SeriesNotifier> seriesNotifiers = database.seriesNotifierDao().getAllNotifiersAlt();

        if (movieNotifiers.size() == 0 && seriesNotifiers.size() == 0) {
            disableBootReceiver(context);
        }
    }

    //enables boot receiver
    private static void enableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    //disables boot receiver
    private static void disableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
