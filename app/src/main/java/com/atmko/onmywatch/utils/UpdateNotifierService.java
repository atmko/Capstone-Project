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
import com.atmko.onmywatch.models.ScheduledMedia;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.MovieApiConstants;
import com.atmko.onmywatch.utils.network_utils.NetworkFunctions;
import com.atmko.onmywatch.utils.network_utils.SeriesApiConstants;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;

import org.parceler.Parcels;

import java.util.Date;

import static com.atmko.onmywatch.Fragments.DetailsFragment.COOL_DOWN_REQUEST_TMDB_ID;
import static com.atmko.onmywatch.Fragments.DetailsFragment.COOL_DOWN_REQUEST_TRAKT_ID;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.models.SeriesNotifier.CONDITION_NEW_EPISODE;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;
import static com.atmko.onmywatch.utils.UpdateMediaWorker.NEW_MEDIA_DATA_KEY;

public class UpdateNotifierService extends JobIntentService {
    private static final String TAG = UpdateNotifierService.class.getSimpleName();

    public static final String ACTION_SET = "set";

    public static final int JOB_ID = 20;

    private MediaData newMediaData;
    private int mMediaType;
    private AppDatabase mDatabase;

    public static void enqueueWork(Context appContext, Intent intent) {
        enqueueWork(appContext, UpdateNotifierService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String intentAction = intent.getAction();

        if (intentAction == null) return;

        newMediaData = Parcels.unwrap(intent.getParcelableExtra(NEW_MEDIA_DATA_KEY));
        mMediaType = newMediaData instanceof MovieData ? MEDIA_TYPE_MOVIE : MEDIA_TYPE_SERIES;
        mDatabase = AppDatabase.getInstance(getApplicationContext());

        if (intentAction.equals(ACTION_SET)) {
            setNotifiers();
        }
    }

    private void setNotifiers() {
        if (this.newMediaData instanceof MovieData) {
            updateReleaseNotifier(newMediaData);

        } else {
            updateNewEpisodeNotifier(((SeriesData) newMediaData));
        }
    }

    //creates release notifier if new watch status is to watch or watching,
    //otherwise delete notifier with this media id and cancel alarm
    private void updateReleaseNotifier(MediaData newMediaData) {
        int newWatchStatus = newMediaData.getWatchStatus();
        if (newWatchStatus == MediaData.WATCH_STATUS_TO_WATCH
                || newWatchStatus == MediaData.WATCH_STATUS_WATCHING) {

            //if release date exists set release notifier through date caparison
            //otherwise create a notifier via release status without creating an alarm
            if (!newMediaData.getReleaseDate().equals("")) {
                setReleaseNotifierThroughDateComparision(newMediaData);

            } else {
                setReleaseNotifierThroughReleaseStatus(newMediaData);
            }

        } else {
            cancelMediaAlarmIfExists(MediaNotifier.CONDITION_ON_RELEASE);
        }
    }

    //compares release date and current date and sets release notifier if release date is in the future
    //then schedules alarm notification for future
    private void setReleaseNotifierThroughDateComparision(MediaData newMediaData) {
        ScheduledMedia scheduledMedia = new ScheduledMedia();
        try {
            scheduledMedia.setAirDate(newMediaData.getReleaseDate());
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        //if release date has passed, return
        if (scheduledMedia.getBestLocalAirDate().before(new Date())) return;

        //create notifier and set alarm with release notification
        MediaNotifier releaseNotifier =
                createReleaseNotifier(newMediaData, scheduledMedia.getBestAvailableDateString());

        NotificationHandler.scheduleReleaseNotification(this, newMediaData, releaseNotifier);
    }

    //used when release date doesn't exist. Checks if media has been released by getting release status via media's details
    //if release status not released, canceled, pilot, ended or returning series, save notifier object without creating accompanying alarm notification.
    //NOTE: alarm will be created when media is updated and a release date becomes available
    private void setReleaseNotifierThroughReleaseStatus(final MediaData newMediaData) {
        //if release status exists create notifier and return
        //otherwise fetch release status from media details, then create notifier
        //NOTE: release status will be null when not accessing this activity via DetailsFragment, because details won't have been fetched
        if (newMediaData.getReleaseStatus() != null) {
            createReleaseNotifierPendingRelease(newMediaData);
            return;
        }

        String[] detailUrls = getResources().getStringArray(R.array.details_urls);
        String detailUrl = null;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            detailUrl = detailUrls[MEDIA_TYPE_SERIES];
        }

        SearchPreferences searchPreferences =  new SearchPreferences();

        //build AN request
        ANRequest request = NetworkFunctions.agnosticDetailRequestById(detailUrl, newMediaData.getId(),
                searchPreferences, this);

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                try {
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            //get release status, set release status and create notifier
                            MediaData detailsMediaData;

                            if (mMediaType == MEDIA_TYPE_MOVIE) {
                                detailsMediaData =
                                        MovieDataParser.parseDetails(returnedJSONString, ((MovieData) UpdateNotifierService.this.newMediaData));

                            } else {
                                detailsMediaData =
                                        SeriesDataParser.parseDetails(returnedJSONString,
                                                ((SeriesData) UpdateNotifierService.this.newMediaData));
                            }

                            String releaseStatus = detailsMediaData.getReleaseStatus();
                            newMediaData.setReleaseStatus(releaseStatus);
                            createReleaseNotifierPendingRelease(newMediaData);
                        }
                    });

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError, COOL_DOWN_REQUEST_TMDB_ID, newMediaData);

                    return;
                }

                //notify user of error
                Log.d(TAG, getString(R.string.details_error_message));
            }
        });
    }

    //create notifier if media release still pending
    private void createReleaseNotifierPendingRelease(MediaData newMediaData) {
        String releaseStatus = newMediaData.getReleaseStatus();

        //create notifier if media release still pending
        if (!releaseStatus.equals(MovieApiConstants.RELEASE_STATUS_RELEASED)
                && !releaseStatus.equals(SeriesApiConstants.SeriesTextReplacement.REPLACEMENT_RETURNING_SERIES)
                && !releaseStatus.equals(SeriesApiConstants.RELEASE_STATUS_PILOT)
                && !releaseStatus.equals(SeriesApiConstants.RELEASE_STATUS_ENDED)
                && !releaseStatus.equals(ApiConstants.RELEASE_STATUS_CANCELED)) {

            createReleaseNotifier(newMediaData, null);
        }
    }

    //creates new Media release notifier in database and returns notifier
    private MediaNotifier createReleaseNotifier(MediaData newMediaData, String dateString) {
        //create notifier and set alarm with release notification
        MediaNotifier releaseNotifier;

        if (newMediaData instanceof MovieData) {
            releaseNotifier =
                    new MovieNotifier(newMediaData.getId(), MediaNotifier.CONDITION_ON_RELEASE, dateString);
            mDatabase.movieNotifierDao().addMediaNotifier(((MovieNotifier) releaseNotifier));

        } else {
            releaseNotifier =
                    new SeriesNotifier(newMediaData.getId(), MediaNotifier.CONDITION_ON_RELEASE, dateString);
            mDatabase.seriesNotifierDao().addMediaNotifier(((SeriesNotifier) releaseNotifier));
        }

        return releaseNotifier;
    }

    //creates new episode notifier if new watch status is watching,
    //creates release notifier if status is to watch
    //otherwise delete notifier with this media id and cancel alarm
    private void updateNewEpisodeNotifier(SeriesData newMediaData) {
        int newWatchStatus = newMediaData.getWatchStatus();

        if (newWatchStatus == MediaData.WATCH_STATUS_TO_WATCH) {
            updateReleaseNotifier(newMediaData);

        } else if (newWatchStatus == MediaData.WATCH_STATUS_WATCHING) {
            getTraktNextEpisodeDetails(newMediaData);

        } else {
            cancelMediaAlarmIfExists(CONDITION_NEW_EPISODE);
        }
    }

    //get next episode details from trakt api
    //gets called twice: once to get matching trakt id, again to get trakt next episode details
    //if trakt id already exists, its called only once
    private void getTraktNextEpisodeDetails(final SeriesData newMediaData) {
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
                                    getTraktNextEpisodeDetails(newMediaData);
                                }

                            } else {
                                //parse trakt info
                                SeriesData detailsMediaData =
                                        SeriesDataParser.parseTraktNextEpisodeDetails(returnedJSONString, newMediaData);

                                //if there is a next episode and date, create notifier using date, otherwise try using tmdb details
                                Episode nextEpisode = detailsMediaData.getNextEpisodeToAir();
                                if (nextEpisode != null && nextEpisode.getBestAvailableDateString() != null) {
                                    setNewEpisodeNotifierThroughDateComparison(detailsMediaData);

                                } else {
                                    getTmdbNextEpisodeDetails(detailsMediaData);
                                }
                            }

                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() == TraktApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError, COOL_DOWN_REQUEST_TRAKT_ID, newMediaData);

                    return;
                }

                //notify user of error
                Log.d(TAG, getString(R.string.details_error_message));
            }
        });
    }

    //creates new episode notifier and notification alarm if release date exists and is in the future
    private void setNewEpisodeNotifierThroughDateComparison(SeriesData newMediaData) {
        ScheduledMedia scheduledMedia = newMediaData.getNextEpisodeToAir();

        //if release date is null or if release date has passed, return
        if (scheduledMedia.getBestLocalAirDate() == null || scheduledMedia.getBestLocalAirDate().before(new Date())) return;

        SeriesNotifier newEpisodeNotifier = createNewEpisodeNotifier(scheduledMedia.getBestAvailableDateString());

        NotificationHandler
                .scheduleNewEpisodeNotification(this, newMediaData, newEpisodeNotifier);
    }

    //Checks if media has been released by getting release status via media's details
    //if episode and air date available, create notification alarm using date
    //if no new episode and or episode date available, save notifier object without creating accompanying alarm notification.
    private void getTmdbNextEpisodeDetails(final SeriesData newMediaData) {
        //if release status exists create notifier and return
        //otherwise fetch release status from media details, then create notifier
        //NOTE: release status will be null when not accessing this activity via DetailsFragment, because details won't have been fetched
        if (newMediaData.getReleaseStatus() != null) {
            createNewEpisodeNotifierPendingRelease(newMediaData);
            return;
        }

        String[] detailUrls = getResources().getStringArray(R.array.details_urls);
        String detailUrl = null;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            detailUrl = detailUrls[MEDIA_TYPE_SERIES];
        }

        SearchPreferences searchPreferences =  new SearchPreferences();

        //build AN request
        ANRequest request = NetworkFunctions.agnosticDetailRequestById(detailUrl, newMediaData.getId(),
                searchPreferences, this);

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                try {
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            //get release status, set release status and create notifier
                            SeriesData detailsMediaData =
                                    SeriesDataParser.parseDetails(returnedJSONString,
                                            ((SeriesData) UpdateNotifierService.this.newMediaData));

                            String releaseStatus = detailsMediaData.getReleaseStatus();
                            newMediaData.setReleaseStatus(releaseStatus);


                            //if there is a next episode and date, create notifier using date, otherwise create notifier without alarm
                            Episode nextEpisode = detailsMediaData.getNextEpisodeToAir();
                            if (nextEpisode != null && nextEpisode.getBestAvailableDateString() != null) {
                                setNewEpisodeNotifierThroughDateComparison(newMediaData);

                            } else {
                                //NOTE: alarm will be created when media is updated and a release date becomes available
                                createNewEpisodeNotifierPendingRelease(newMediaData);
                            }
                        }
                    });

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError, COOL_DOWN_REQUEST_TRAKT_ID, newMediaData);

                    return;
                }

                //notify user of error
                Log.d(TAG, getString(R.string.details_error_message));
            }
        });
    }

    //create notifier if episodes still pending
    private void createNewEpisodeNotifierPendingRelease(SeriesData newMediaData) {
        String releaseStatus = newMediaData.getReleaseStatus();

        //create notifier if new episodes still pending
        if (releaseStatus.equals(SeriesApiConstants.SeriesTextReplacement.REPLACEMENT_RETURNING_SERIES)) {
            createNewEpisodeNotifier(null);
        }
    }

    private SeriesNotifier createNewEpisodeNotifier(String dateString) {
        SeriesNotifier newEpisodeNotifier =
                new SeriesNotifier(newMediaData.getId(), CONDITION_NEW_EPISODE, dateString);
        mDatabase.seriesNotifierDao().addMediaNotifier(newEpisodeNotifier);

        return newEpisodeNotifier;
    }

    //retry method if api returns too may requests error
    private void retryAfterCoolDOwn(ANError anError, final int coolDownRequestId,
                                    final MediaData newMediaData) {
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
                    if (coolDownRequestId == COOL_DOWN_REQUEST_TMDB_ID) {
                        setReleaseNotifierThroughReleaseStatus(newMediaData);

                    } else if (coolDownRequestId == COOL_DOWN_REQUEST_TRAKT_ID){
                        getTraktNextEpisodeDetails(((SeriesData) newMediaData));
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