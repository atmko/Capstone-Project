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

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.database.daos.SeriesNotifierDao;
import com.atmko.onmywatch.models.Episode;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.NotificationIdlingResource;
import com.atmko.onmywatch.models.ScheduledMedia;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class NotificationHandler {
    private static final long TIME_DILATION = TimeUnit.MINUTES.toMillis(10);
    //for testing
    public static long TEST_TIME_DILATION = 0;
    public static boolean IS_TESTING = false;

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
                    long releaseTimestamp = intent.getLongExtra(MediaNotifier.TIMESTAMP_KEY, 0);
                    int condition = intent.getIntExtra(MediaNotifier.CONDITION_KEY, 0);
                    Notification notification = intent.getParcelableExtra(MediaNotifier.NOTIFICATIONS_KEY);
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    //and media id and condition as unique ids
                    //show notification
                    if (notification != null) {
                        notificationManager.notify(mediaId, condition, notification);
                    }

                    //if notifier condition is new episode, update notifier
                    if (condition == SeriesNotifier.CONDITION_NEW_EPISODE) {
                        SeriesNotifierDao seriesNotifierDao =
                                AppDatabase.getInstance(context).seriesNotifierDao();

                        SeriesNotifier seriesNotifier =
                                seriesNotifierDao.getNotifierByIdAlt(mediaId, condition);
                        if (seriesNotifier != null) {
                            //set series notifier to inactive
                            seriesNotifier.setIsActive(false);
                            seriesNotifierDao.updateNotifier(seriesNotifier);

                            LogUpdateReceiver.createLogUpdateAlarm(mediaType, mediaId, condition, releaseTimestamp, context);

                            // The IdlingResource is null in production.
                            if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
                                NotificationIdlingResource.getNotificationIdlingResource().setIdleState(true);
                            }

                            //skip notifier deletion if condition is new episodes
                            return;
                        }
                    }

                    if (mediaType == MEDIA_TYPE_MOVIE && condition == MediaNotifier.CONDITION_ON_RELEASE) {
                        LogUpdateReceiver.createLogUpdateAlarm(mediaType, mediaId, condition, releaseTimestamp, context);
                    }

                    //remove media notifier from the database
                    removeMediaNotifier(context, mediaType, mediaId, condition);

                    // The IdlingResource is null in production.
                    if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
                        NotificationIdlingResource.getNotificationIdlingResource().setIdleState(true);
                    }
                }
            });
        }

        private void removeMediaNotifier(Context context, int mediaType, String mediaId, int condition) {
            AppDatabase database = AppDatabase.getInstance(context);
            MediaNotifier notifier;

            if (mediaType == MEDIA_TYPE_MOVIE) {
                notifier = database.movieNotifierDao().getNotifierByIdAlt(mediaId, condition);
                //prevent crash
                if (notifier != null) {
                    database.movieNotifierDao().deleteNotifier(((MovieNotifier) notifier));
                }

            } else {
                notifier = database.seriesNotifierDao().getNotifierByIdAlt(mediaId, condition);
                //prevent crash
                if (notifier != null) {
                    database.seriesNotifierDao().deleteNotifier(((SeriesNotifier) notifier));
                }
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

    public static void restoreNotifiers(final Context context) {
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
        List<MovieNotifier> notifiers = database.movieNotifierDao().getActiveNotifiersAlt();

        for (MovieNotifier notifier: notifiers) {
            if (notifier == null) continue;
            MediaData mediaData = database.movieDataDao().getMovieByIdAlt(notifier.getMediaId());
            if (mediaData == null) continue;

            if (notifier.getCondition() == MediaNotifier.CONDITION_ON_RELEASE) {
                //skip if release date is empty
                if (mediaData.getReleaseDate().equals("")) continue;

                scheduleReleaseNotification(context, mediaData, notifier);
            }
        }
    }

    private static void setSeriesNotifiers(Context context, AppDatabase database) {
        List<SeriesNotifier> notifiers = database.seriesNotifierDao().getActiveNotifiersAlt();

        for (SeriesNotifier notifier: notifiers) {
            if (notifier == null) continue;
            SeriesData seriesData = database.seriesDataDao().getSeriesByIdAlt(notifier.getMediaId());
            if (seriesData == null || seriesData.getNextEpisodeToAir() == null) continue;
            String alarmDate = seriesData.getNextEpisodeToAir().getBestAvailableDateString();

            //skip if alarm date is empty
            if (alarmDate == null || alarmDate.equals("")) continue;
            //TODO: remove redundant get(get already performed above)
            SeriesData mediaData = database.seriesDataDao().getSeriesByIdAlt(notifier.getMediaId());

            if (notifier.getCondition() == MediaNotifier.CONDITION_ON_RELEASE) {
                scheduleReleaseNotification(context, mediaData, notifier);

            } else if (notifier.getCondition() == SeriesNotifier.CONDITION_NEW_EPISODE) {
                scheduleNewEpisodeNotification(context, mediaData, notifier);
            }
        }
    }

    static void scheduleReleaseNotification(Context context, MediaData mediaData,
                                            MediaNotifier notifier) {
        int mediaType;
        ScheduledMedia scheduledMedia;
        int source;
        if (mediaData instanceof MovieData) {
            mediaType = MEDIA_TYPE_MOVIE;
            scheduledMedia = ((MovieData) mediaData).getScheduledMedia();
            source = ScheduledMedia.SOURCE_TMDB;

        } else {
            mediaType = MEDIA_TYPE_SERIES;
            scheduledMedia = ((SeriesData) mediaData).getNextEpisodeToAir();
            source = ((Episode) scheduledMedia).source;
        }

        Notification notification =
                notifier.createReleaseNotification(context, mediaData, scheduledMedia.isInFuture(), source);

        long releaseTimestamp = scheduledMedia.getBestLocalAirDate().getTime();

        //create pending intent to house notification for when alarm is triggered
        PendingIntent releasePendingIntent =
                notifier.createPendingIntent(context, mediaType, mediaData.getId(),
                        releaseTimestamp, notification);

        setNotificationAlarm(context, releasePendingIntent,
                getNotificationTimestamp(mediaType, releaseTimestamp));
    }

    static void scheduleNewEpisodeNotification(Context context, SeriesData mediaData,
                                               SeriesNotifier notifier) {

        Episode nextEpisode = mediaData.getNextEpisodeToAir();
        int source = nextEpisode.source;

        Notification notification = notifier.createNewEpisodeNotification(
                context, mediaData, nextEpisode.isInFuture(), source, nextEpisode.getShorthand());

        long releaseTimestamp = nextEpisode.getBestLocalAirDate().getTime();

        //create pending intent to house notification for when alarm is triggered
        PendingIntent releasePendingIntent =
                notifier.createPendingIntent(context, MEDIA_TYPE_SERIES, mediaData.getId(),
                        releaseTimestamp, notification);

        setNotificationAlarm(context, releasePendingIntent,
                getNotificationTimestamp(MasterActivity.MEDIA_TYPE_SERIES, releaseTimestamp));
    }

    private static long getNotificationTimestamp(int mediaType, long releaseTimestamp) {
        if (mediaType == MEDIA_TYPE_MOVIE) {
            return releaseTimestamp;
        } else {
            if (IS_TESTING) {
                return releaseTimestamp + TEST_TIME_DILATION;

            } else {
                return releaseTimestamp - TIME_DILATION;
            }
        }
    }

    private static void setNotificationAlarm(Context context, PendingIntent releasePendingIntent,
                                             long timestamp) {

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr != null) {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, timestamp, releasePendingIntent);
        }

        enableBootReceiver(context);
    }

    //deletes notifiers and cancel alarm notifications
    static void cancelNotificationAlarm(Context context, MediaNotifier notifier) {
        AppDatabase database = AppDatabase.getInstance(context);

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent intent = notifier.createPendingIntent(context);
        if (alarmMgr != null) alarmMgr.cancel(intent);

        if (notifier instanceof MovieNotifier) {
            database.movieNotifierDao().deleteNotifier(((MovieNotifier) notifier));

        } else {
            database.seriesNotifierDao().deleteNotifier(((SeriesNotifier) notifier));
        }
    }

    public static void cancelAllNotificationAlarms(final Context context) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                AppDatabase database = AppDatabase.getLocalDatabase(context);
                //cancel all movie alarms
                List<MovieNotifier> movieNotifiers = database.movieNotifierDao().getAllNotifiersAlt();
                for (MovieNotifier movieNotifier: movieNotifiers) {
                    cancelNotificationAlarm(context, movieNotifier);
                }

                //cancel all series alarms
                List<SeriesNotifier> seriesNotifiers = database.seriesNotifierDao().getAllNotifiersAlt();
                for (SeriesNotifier seriesNotifier: seriesNotifiers) {
                    cancelNotificationAlarm(context, seriesNotifier);
                }
            }
        });
    }

    //enables boot receiver
    private static void enableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
}
