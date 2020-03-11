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
import com.atmko.onmywatch.database.daos.SeriesNotifierDao;
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
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker;

import org.parceler.Parcels;

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

                    //if notifier condition is new episode, update notifier
                    if (condition == SeriesNotifier.CONDITION_NEW_EPISODE) {
                        SeriesNotifierDao seriesNotifierDao =
                                AppDatabase.getInstance(context).seriesNotifierDao();

                        SeriesNotifier seriesNotifier = seriesNotifierDao.getNotifierByIdAlt(mediaId, condition);
                        if (seriesNotifier != null) {
                            //set series notifier to inactive
                            seriesNotifier.setIsActive(false);
                            seriesNotifierDao.updateNotifier(seriesNotifier);

                            // The IdlingResource is null in production.
                            if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
                                NotificationIdlingResource.getNotificationIdlingResource().addToIdleCounter();
                            }

                            //skip notifier deletion if condition is new episodes
                            return;
                        }
                    }

                    //remove media notifier from the database
                    removeMediaNotifier(context, mediaType, mediaId, condition);

                    // The IdlingResource is null in production.
                    if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
                        NotificationIdlingResource.getNotificationIdlingResource().addToIdleCounter();
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
            MediaData mediaData = database.movieDataDao().getMovieByIdAlt(notifier.getMediaId());

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
            SeriesData seriesData = database.seriesDataDao().getSeriesByIdAlt(notifier.getMediaId());
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

    public static void scheduleReleaseNotification(Context context, MediaData mediaData,
                                                   MediaNotifier notifier) {
        int mediaType;

        Notification notification = notifier.createReleaseNotification(context, mediaData);

        if (mediaData instanceof MovieData) {
            mediaType = MEDIA_TYPE_MOVIE;

        } else {
            mediaType = MEDIA_TYPE_SERIES;
        }

        //create pending intent to house notification for when alarm is triggered
        PendingIntent releasePendingIntent =
                notifier.createPendingIntent(context, mediaType, mediaData.getId(), notification);

        ScheduledMedia scheduledMedia = new ScheduledMedia();

        //TODO: make movie data have scheduled media object like series data to hold release information
        try {
            scheduledMedia.setAirDate(mediaData.getReleaseDate());
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        long releaseTimestamp = scheduledMedia.getBestLocalAirDate().getTime();

        setNotificationAlarm(context, releasePendingIntent, releaseTimestamp);
    }

    public static void scheduleNewEpisodeNotification(Context context, SeriesData mediaData,
                                                      SeriesNotifier notifier) {
        Notification notification = notifier.createNewEpisodeNotification(context, mediaData);

        //create pending intent to house notification for when alarm is triggered
        PendingIntent releasePendingIntent =
                notifier.createPendingIntent(context, MEDIA_TYPE_SERIES, mediaData.getId(), notification);

        long releaseTimestamp = mediaData.getNextEpisodeToAir().getBestLocalAirDate().getTime();

        setNotificationAlarm(context, releasePendingIntent, releaseTimestamp);
    }

    private static void setNotificationAlarm(Context context, PendingIntent releasePendingIntent,
                                             long timestamp) {

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, timestamp, releasePendingIntent);

        enableBootReceiver(context);
    }

    //deletes notifiers and cancel alarm notifications
    public static void cancelAlarm(Context context, MediaNotifier notifier) {
        AppDatabase database = AppDatabase.getInstance(context);

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent intent = notifier.createPendingIntent(context);
        alarmMgr.cancel(intent);

        if (notifier instanceof MovieNotifier) {
            database.movieNotifierDao().deleteNotifier(((MovieNotifier) notifier));

        } else {
            database.seriesNotifierDao().deleteNotifier(((SeriesNotifier) notifier));
        }
    }

    public static void cancelAllAlarms(final Context context) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                AppDatabase database = AppDatabase.getLocalDatabase(context);
                //cancel all movie alarms
                List<MovieNotifier> movieNotifiers = database.movieNotifierDao().getAllNotifiersAlt();
                for (MovieNotifier movieNotifier: movieNotifiers) {
                    cancelAlarm(context, movieNotifier);
                }

                //cancel all series alarms
                List<SeriesNotifier> seriesNotifiers = database.seriesNotifierDao().getAllNotifiersAlt();
                for (SeriesNotifier seriesNotifier: seriesNotifiers) {
                    cancelAlarm(context, seriesNotifier);
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

    //disables boot receiver
    private static void disableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
