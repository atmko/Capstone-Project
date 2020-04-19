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
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.NotificationIdlingResource;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
import com.atmko.onmywatch.utils.api_utils.SeriesDataParser;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker;

import org.parceler.Parcels;

import static com.atmko.onmywatch.Fragments.DetailsFragment.COOL_DOWN_REQUEST_TRAKT_ID;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;
import static com.atmko.onmywatch.utils.UpdateNotifierService.ASSUME_TRAKT_NEXT_EPISODE_NULL;
import static com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker.NEW_MEDIA_DATA_KEY;

public class NoNotifierService extends JobIntentService {
    private static final String TAG = NoNotifierService.class.getSimpleName();

    private static final String ACTION_TESTING = "testing";
    private static final String ACTION_SET = "set";

    private static final int JOB_ID = 20;

    private static final String sActionMode = ACTION_SET;

    private MediaData newMediaData;
    private int mMediaType;
    private AppDatabase mDatabase;

    public static void enqueueWork(Context appContext, Intent intent) {
        //set idle state to false
        if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
            NotificationIdlingResource.getNotificationIdlingResource().setIdleState(false);
        }

        enqueueWork(appContext, NoNotifierService.class, JOB_ID, intent);
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
        if (newMediaData instanceof SeriesData) {
            updateNewEpisodeNotifier();
        }
    }

    //creates new episode notifier if new watch status is watching,
    //creates release notifier if status is to watch
    //otherwise delete notifier with this media id and cancel alarm
    private void updateNewEpisodeNotifier() {
        int newWatchStatus = newMediaData.getWatchStatus();

        if (newWatchStatus != MediaData.WATCH_STATUS_WATCHING) {
            trackMedia(SeriesTracker.ACTION_DELETE);
        }

        if (newWatchStatus == MediaData.WATCH_STATUS_WATCHING) {
            getTraktNextEpisodeDetails();
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
                                    //save trakt id
                                    updateMedia(newMediaData);

                                    getTraktNextEpisodeDetails();
                                }

                            } else {
                                //parse trakt info
                                newMediaData =
                                        SeriesDataParser.parseTraktNextEpisodeDetails(returnedJSONString, ((SeriesData) newMediaData));

                                trackMedia(SeriesTracker.ACTION_SET);

                                //if ASSUME_TRAKT_NEXT_EPISODE_NULL is false, use production code
                                if (!ASSUME_TRAKT_NEXT_EPISODE_NULL) {
                                    //if there is a next episode and date, create notifier using date, otherwise try using tmdb details
                                    Episode nextEpisode = ((SeriesData) newMediaData).getNextEpisodeToAir();
                                    if (nextEpisode != null && nextEpisode.getBestAvailableDateString() != null) {
                                        //save next episode
                                        updateMedia(newMediaData);

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
                //save next episode
                updateMedia(newMediaData);
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
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

    private void updateMedia(MediaData mediaData) {
        if (mediaData instanceof MovieData) {
            mDatabase.movieDataDao().updateMovieData(((MovieData) mediaData));
        } else {
            mDatabase.seriesDataDao().updateSeriesData(((SeriesData) mediaData));
        }
    }

    private void trackMedia(String actionMode) {
        SeriesTracker.sActionMode = actionMode;
        Intent intent = new Intent(getApplicationContext(), SeriesTracker.class);
        intent.putExtra(NEW_MEDIA_DATA_KEY, Parcels.wrap(newMediaData));
        SeriesTracker.enqueueWork(getApplicationContext(), intent);
    }
}