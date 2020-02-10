/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
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
import com.atmko.onmywatch.utils.api_utils.SeriesDataParser;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.api_utils.MovieApiConstants;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
import com.atmko.onmywatch.utils.api_utils.SeriesApiConstants;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker;

import org.parceler.Parcels;

import static com.atmko.onmywatch.Fragments.DetailsFragment.COOL_DOWN_REQUEST_TRAKT_ID;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.models.MediaNotifier.CONDITION_ON_RELEASE;
import static com.atmko.onmywatch.models.SeriesNotifier.CONDITION_NEW_EPISODE;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;
import static com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker.NEW_MEDIA_DATA_KEY;

public class UpdateNotifierService extends JobIntentService {
    private static final String TAG = UpdateNotifierService.class.getSimpleName();

    public static final String ACTION_TESTING = "testing";
    public static final String ACTION_SET = "set";

    public static boolean ASSUME_TRAKT_NEXT_EPISODE_NULL;

    public static final int JOB_ID = 20;

    public static String sActionMode = ACTION_SET;

    private MediaData newMediaData;
    private int mMediaType;
    private AppDatabase mDatabase;

    public static void enqueueWork(Context appContext, Intent intent) {
        //set idle state to false
        if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
            NotificationIdlingResource.getNotificationIdlingResource().setIdleState(false);
        }

        enqueueWork(appContext, UpdateNotifierService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        newMediaData = Parcels.unwrap(intent.getParcelableExtra(NEW_MEDIA_DATA_KEY));
        mMediaType = newMediaData instanceof MovieData ? MEDIA_TYPE_MOVIE : MEDIA_TYPE_SERIES;
        mDatabase = AppDatabase.getInstance(getApplicationContext());

        if (sActionMode.equals(ACTION_SET) || sActionMode.equals(ACTION_TESTING)) {
            setNotifiers();
        }
    }

    private void setNotifiers() {
        if (newMediaData instanceof MovieData) {
            updateReleaseNotifier();

        } else {
            updateNewEpisodeNotifier();
        }
    }

    //creates release notifier if new watch status is to watch or watching,
    //otherwise delete notifier with this media id and cancel alarm
    private void updateReleaseNotifier() {
        int newWatchStatus = newMediaData.getWatchStatus();
        if (newWatchStatus == MediaData.WATCH_STATUS_TO_WATCH
                || newWatchStatus == MediaData.WATCH_STATUS_WATCHING) {

            //if release date exists set release notifier through date caparison
            //otherwise create a notifier via release status without creating an alarm
            if (!newMediaData.getReleaseDate().equals("") && newMediaData.getReleaseDate() != null) {
                setReleaseNotifierThroughDateComparision();

            } else {
                createReleaseNotifierPendingRelease();
            }

        } else {
            cancelMediaAlarmIfExists(MediaNotifier.CONDITION_ON_RELEASE);
            //set idle state to true
            if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
                NotificationIdlingResource.getNotificationIdlingResource().setIdleState(true);
            }
        }
    }

    //compares release date and current date and sets release notifier if release date is in the future
    //then schedules alarm notification for future
    private void setReleaseNotifierThroughDateComparision() {
        ScheduledMedia scheduledMedia = new ScheduledMedia();
        try {
            scheduledMedia.setAirDate(newMediaData.getReleaseDate());
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        //if release date has passed, return
        if (scheduledMedia.getBestLocalAirDate().before(new GeneralUtils.DateInject().currentDate())) {
            //set idle state to true
            if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
                NotificationIdlingResource.getNotificationIdlingResource().setIdleState(true);
            }

            return;
        }

        //create notifier and set alarm with release notification
        MediaNotifier releaseNotifier =
                createReleaseNotifier(newMediaData, true);

        NotificationHandler.scheduleReleaseNotification(this, newMediaData, releaseNotifier);
    }

    //create notifier if media release still pending
    //used when release date doesn't exist.
    //if release status not released, canceled, pilot, ended or returning series, save notifier object without creating accompanying alarm notification.
    //NOTE: alarm will be created when media is updated and a release date becomes available
    private void createReleaseNotifierPendingRelease() {
        String releaseStatus = newMediaData.getReleaseStatus();

        //create notifier if media release still pending
        if (!releaseStatus.equals(MovieApiConstants.RELEASE_STATUS_RELEASED)
                && !releaseStatus.equals(ApiConstants.TextReplacement.REPLACEMENT_RETURNING_SERIES)
                && !releaseStatus.equals(SeriesApiConstants.RELEASE_STATUS_PILOT)
                && !releaseStatus.equals(SeriesApiConstants.RELEASE_STATUS_ENDED)
                && !releaseStatus.equals(ApiConstants.RELEASE_STATUS_CANCELED)) {

            createReleaseNotifier(newMediaData, false);

            //set idle state to true
            if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
                NotificationIdlingResource.getNotificationIdlingResource().setIdleState(true);
            }
        }
    }

    //if notifier doesn't exist, create new Media release notifier in database and return notifier
    private MediaNotifier createReleaseNotifier(MediaData newMediaData, boolean iActive) {
        MediaNotifier savedNotifier = getNotifier(CONDITION_ON_RELEASE);
        if (savedNotifier != null) return savedNotifier;

        //create notifier
        MediaNotifier releaseNotifier;

        if (newMediaData instanceof MovieData) {
            releaseNotifier =
                    new MovieNotifier(newMediaData.getId(), MediaNotifier.CONDITION_ON_RELEASE, iActive);
            mDatabase.movieNotifierDao().addMediaNotifier(((MovieNotifier) releaseNotifier));

        } else {
            releaseNotifier =
                    new SeriesNotifier(newMediaData.getId(), MediaNotifier.CONDITION_ON_RELEASE, iActive);
            mDatabase.seriesNotifierDao().addMediaNotifier(((SeriesNotifier) releaseNotifier));
        }

        return releaseNotifier;
    }

    //creates new episode notifier if new watch status is watching,
    //creates release notifier if status is to watch
    //otherwise delete notifier with this media id and cancel alarm
    private void updateNewEpisodeNotifier() {
        int newWatchStatus = newMediaData.getWatchStatus();

        if (newWatchStatus == MediaData.WATCH_STATUS_TO_WATCH) {
            cancelMediaAlarmIfExists(CONDITION_NEW_EPISODE);
            //TODO: series release notifier current does't use trakt api but uses tmdb. Make release notifier uses trakt for better accuracy
            updateReleaseNotifier();

        } else if (newWatchStatus == MediaData.WATCH_STATUS_WATCHING) {
            getTraktNextEpisodeDetails();

        } else {
            cancelMediaAlarmIfExists(CONDITION_ON_RELEASE);
            cancelMediaAlarmIfExists(CONDITION_NEW_EPISODE);

            //set idle state to true
            if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
                NotificationIdlingResource.getNotificationIdlingResource().setIdleState(true);
            }
        }
    }

    private MediaNotifier getNotifier(int condition) {
        if (mMediaType == MEDIA_TYPE_MOVIE) {
            return mDatabase.movieNotifierDao().getNotifierByIdAlt(newMediaData.getId(), condition);

        } else {
            return mDatabase.seriesNotifierDao().getNotifierByIdAlt(newMediaData.getId(), condition);
        }
    }

    //get next episode details from trakt api
    //gets called twice: once to get matching trakt id, again to get trakt next episode details
    //if trakt id already exists, its called only once
    private void getTraktNextEpisodeDetails() {
        //if inputTraktId id is null make url to get trakt id
        //otherwise make url to get next episode details

        final String inputTraktId = newMediaData.getTraktId();

        String[] traktFetchUrls;
        ANRequest request;

        if (inputTraktId == (null)) {
            traktFetchUrls = getResources().getStringArray(R.array.trakt_matching_media_urls);
            String traktFetchUrl = traktFetchUrls[mMediaType];
            request = NetworkFunctions.traktAgnosticRequestById(
                    traktFetchUrl, newMediaData.getId());

        } else {
            String traktFetchUrl = getString(R.string.trakt_next_episode_urls);
            request = NetworkFunctions.traktAgnosticRequestById(
                    traktFetchUrl, newMediaData.getTraktId());
        }

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (inputTraktId == null) {
                                String outputTraktId = SeriesDataParser.parseAndGetTraktId(returnedJSONString);

                                //rerun the function with non null trakt id
                                if (outputTraktId != null) {
                                    newMediaData.setTraktId(outputTraktId);

                                    getTraktNextEpisodeDetails();
                                }

                            } else {
                                //parse trakt info
                                newMediaData =
                                        SeriesDataParser.parseTraktNextEpisodeDetails(returnedJSONString, ((SeriesData) newMediaData));

                                //if ASSUME_TRAKT_NEXT_EPISODE_NULL is false, use production code
                                if (!ASSUME_TRAKT_NEXT_EPISODE_NULL) {
                                    //if there is a next episode and date, create notifier using date, otherwise try using tmdb details
                                    Episode nextEpisode = ((SeriesData) newMediaData).getNextEpisodeToAir();
                                    if (nextEpisode != null && nextEpisode.getBestAvailableDateString() != null) {
                                        setNewEpisodeNotifierThroughDateComparison();

                                    } else {
                                        getTmdbNextEpisodeDetails();
                                    }

                                } else {
                                    getTmdbNextEpisodeDetails();
                                }
                            }

                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final ANError anError) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (anError.getErrorCode() == TraktApiConstants.TOO_MANY_REQUESTS) {
                            retryAfterCoolDOwn(anError, COOL_DOWN_REQUEST_TRAKT_ID);

                        } else {
                            getTmdbNextEpisodeDetails();
                        }

                        //notify user of error
                        Log.d(TAG, getString(R.string.details_error_message));
                    }
                });
            }
        });
    }

    //creates new episode notifier and notification alarm if release date exists and is in the future
    private void setNewEpisodeNotifierThroughDateComparison() {
        //update media data to save trakt id and countdown
        mDatabase.seriesDataDao().updateSeriesData(((SeriesData) newMediaData));

        Episode nextEpisode = ((SeriesData) newMediaData).getNextEpisodeToAir();

        boolean releaseDateInPast =
                nextEpisode.getBestLocalAirDate().before(GeneralUtils.DateInject.getInstance().currentDate());

        //if logic bypass is false, use production code
        if (!GeneralUtils.LOGIC_BYPASS) {
            //if release date is null or if release date has passed, return
            if (nextEpisode.getBestLocalAirDate() == null || releaseDateInPast) {
                //set idle state to true
                if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
                    NotificationIdlingResource.getNotificationIdlingResource().setIdleState(true);
                }

                return;
            }
        }

        SeriesNotifier newEpisodeNotifier = createNewEpisodeNotifier(true);

        NotificationHandler
                .scheduleNewEpisodeNotification(this, ((SeriesData) newMediaData), newEpisodeNotifier);
    }

    //Checks if media has been released
    //if episode and air date available, create notification alarm using date
    //if no new episode and or episode date available, save notifier object without creating accompanying alarm notification.
    private void getTmdbNextEpisodeDetails() {
        //if release status exists create notifier and return
        //otherwise fetch release status from media details, then create notifier
        //NOTE: release status will be null when not accessing this activity via DetailsFragment, because details won't have been fetched
        try {
            //if there is a next episode and date, create notifier using date, otherwise create notifier without alarm
            Episode nextEpisode = ((SeriesData) newMediaData).getNextEpisodeToAir();
            if (nextEpisode != null && nextEpisode.getBestAvailableDateString() != null) {
                setNewEpisodeNotifierThroughDateComparison();

            } else {
                //NOTE: alarm will be created when media is updated and a release date becomes available
                createEpisodeNotifierPendingDateInfo();
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    //create notifier if episodes still pending
    private void createEpisodeNotifierPendingDateInfo() {
        String releaseStatus = newMediaData.getReleaseStatus();

        //create notifier if new episodes still pending, otherwise if series isn't yet released, set release notifier
        if (releaseStatus.equals(ApiConstants.TextReplacement.REPLACEMENT_RETURNING_SERIES)) {
            createNewEpisodeNotifier(false);

        } else if (releaseStatus.equals(ApiConstants.RELEASE_STATUS_PLANNED)
                || releaseStatus.equals(ApiConstants.TextReplacement.REPLACEMENT_IN_PRODUCTION)) {

            createReleaseNotifierPendingRelease();
        }

        //set idle state to true
        if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
            NotificationIdlingResource.getNotificationIdlingResource().setIdleState(true);
        }
    }

    private SeriesNotifier createNewEpisodeNotifier(boolean isActive) {
        MediaNotifier savedNotifier = getNotifier(CONDITION_NEW_EPISODE);
        if (savedNotifier != null) return ((SeriesNotifier) savedNotifier);

        SeriesNotifier newEpisodeNotifier =
                new SeriesNotifier(newMediaData.getId(), CONDITION_NEW_EPISODE, isActive);
        mDatabase.seriesNotifierDao().addMediaNotifier(newEpisodeNotifier);

        return newEpisodeNotifier;
    }

    //retry method if api returns too may requests error
    private void retryAfterCoolDOwn(ANError anError, final int coolDownRequestId) {
        Log.d(TAG, "retrying details fetch");

        int coolDown;

        try {
            //noinspection ConstantConditions
            coolDown = Integer.parseInt(anError.getResponse().header(ApiConstants.RETRY_AFTER_KEY));

        } catch (NullPointerException e) {
            e.printStackTrace();
            coolDown = UpdateMediaWorker.REQUEST_COOL_DOWN;

        }

        int coolDownInMilliSecs = coolDown * MILLISECOND_CONVERSION;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (coolDownRequestId == COOL_DOWN_REQUEST_TRAKT_ID){
                        getTraktNextEpisodeDetails();
                    }

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

            }
        }, coolDownInMilliSecs);
    }

    //cancels all alarms with media id and deletes notifiers
    private void cancelMediaAlarmIfExists(int condition) {
        MediaNotifier notifier;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            notifier = mDatabase.movieNotifierDao().getNotifierByIdAlt(newMediaData.getId(), condition);

        } else {
            notifier = mDatabase.seriesNotifierDao().getNotifierByIdAlt(newMediaData.getId(), condition);
        }

        if (notifier != null) {
            //cancel alarm and delete media notifier
            NotificationHandler.cancelAlarm(this, notifier);
        }
    }
}