/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.NetworkFunctions;

import org.parceler.Parcels;

import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;

public class UpdateMediaWorker extends Worker {
    private static final String TAG = UpdateMediaWorker.class.getSimpleName();

    public static final String NEW_MEDIA_DATA_KEY = "new_media_data";

    public static final int REQUEST_COOL_DOWN = 1000;

    public static String sMovieDetailsStringInject;
    public static String sSeriesDetailsStringInject;
    private final Context mContext;
    private final AppDatabase mDatabase;

    public UpdateMediaWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        mContext = context;
        mDatabase = AppDatabase.getInstance(mContext);
    }

    @NonNull
    @Override
    public Result doWork() {
        fetchSavedMovies();
        fetchSavedSeries();

        return Result.success();
    }

    private void fetchSavedMovies() {
        //get all saved movies
        List<MovieData> movieDataList = mDatabase.movieDataDao()
                .getAllMoviesAlt();

        List<String> notifierMediaIds = mDatabase.movieNotifierDao()
                .getAllMediaIdsAlt();

        //get movie detail url format
        String[] detailUrls = mContext.getResources().getStringArray(R.array.details_urls);
        String detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        //configure search preferences
        SearchPreferences searchPreferences = new SearchPreferences();

        //iterate through movie list with this watch status
        for (MovieData movieData: movieDataList) {
            SystemClock.sleep(REQUEST_COOL_DOWN);

            if (notifierMediaIds.contains(movieData.getId())) {
                updateSavedMedia(movieData, detailUrl, searchPreferences, true);

            } else {
                updateSavedMedia(movieData, detailUrl, searchPreferences, false);

            }
        }
    }

    private void fetchSavedSeries() {
        //get all saved series
        List<SeriesData> seriesDataList = mDatabase.seriesDataDao()
                .getAllSeriesAlt();

        List<String> notifierMediaIds = mDatabase.seriesNotifierDao()
                .getAllMediaIdsAlt();

        //get series detail url format
        String[] detailUrls = mContext.getResources().getStringArray(R.array.details_urls);
        String detailUrl = detailUrls[MEDIA_TYPE_SERIES];

        //configure search preferences
        SearchPreferences searchPreferences = new SearchPreferences();

        //iterate through series list with this watch status
        for (SeriesData seriesData: seriesDataList) {
            SystemClock.sleep(REQUEST_COOL_DOWN);

            if (notifierMediaIds.contains(seriesData.getId())) {
                updateSavedMedia(seriesData, detailUrl, searchPreferences, true);

            } else {
                updateSavedMedia(seriesData, detailUrl, searchPreferences, false);

            }
        }
    }

    private void updateSavedMedia(final MediaData oldMediaData, final String detailUrl,
                                  final SearchPreferences searchPreferences, final boolean hasNotifiers) {
        String id = oldMediaData.getId();

        //build AN request
        ANRequest request =
                NetworkFunctions
                        .agnosticDetailRequestById(detailUrl, id,  searchPreferences, mContext);

        if (sSeriesDetailsStringInject != null || sMovieDetailsStringInject != null) {
            String detailsInject =
                    oldMediaData instanceof MovieData ? sMovieDetailsStringInject : sSeriesDetailsStringInject;

            parseAndApplyUpdatedJsonString(oldMediaData, detailsInject, hasNotifiers);

            return;
        }

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        parseAndApplyUpdatedJsonString(oldMediaData, returnedJSONString, hasNotifiers);
                    }
                });
            }

            @Override
            public void onError(ANError anError) {
                Log.d(TAG, oldMediaData.getTitle() + " update data failed");

                //notify user of error
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError, oldMediaData, detailUrl, searchPreferences, hasNotifiers);
                }
            }
        });
    }

    private void parseAndApplyUpdatedJsonString(MediaData oldMediaData, String returnedJSONString, Boolean hasNotifiers) {
        MediaData newMediaData;

        //parse and populate retrieved data
        if (oldMediaData instanceof MovieData) {
            newMediaData =
                    MovieDataParser.parseDetails(returnedJSONString,
                            ((MovieData) oldMediaData));

            mDatabase.movieDataDao().updateMovieData(((MovieData) newMediaData));

        } else {
            newMediaData =
                    SeriesDataParser.parseDetails(returnedJSONString,
                            ((SeriesData) oldMediaData));

            mDatabase.seriesDataDao().updateSeriesData(((SeriesData) newMediaData));
        }

                        if (hasNotifiers) {
                            Intent intent = new Intent(getApplicationContext(), UpdateNotifierService.class);
                            intent.putExtra(NEW_MEDIA_DATA_KEY, Parcels.wrap(newMediaData));
                            UpdateNotifierService.enqueueWork(mContext, intent);
                        }

        Log.d(TAG, newMediaData.getTitle() + " data updated");
    }

    private void retryAfterCoolDOwn(ANError anError, final MediaData mediaData,
                                    final String detailUrl, final SearchPreferences searchPreferences,
                                    final boolean haNotifiers) {
        Log.d(TAG, mediaData.getTitle() + " retrying update");

        int coolDown = Integer.valueOf(anError.getResponse().header(ApiConstants.RETRY_AFTER_KEY));
        int coolDownInMilliSecs = coolDown * MILLISECOND_CONVERSION;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateSavedMedia(mediaData, detailUrl, searchPreferences, haNotifiers);

            }
        }, coolDownInMilliSecs);
    }
}
