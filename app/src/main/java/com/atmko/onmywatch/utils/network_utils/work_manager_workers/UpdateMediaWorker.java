/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils.work_manager_workers;

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
import com.atmko.onmywatch.utils.NoNotifierService;
import com.atmko.onmywatch.utils.UpdateNotifierService;
import com.atmko.onmywatch.utils.api_utils.MovieDataParser;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;
import com.atmko.onmywatch.utils.api_utils.SeriesDataParser;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;

public class UpdateMediaWorker extends Worker {
    private static final String TAG = UpdateMediaWorker.class.getSimpleName();

    public static final String NEW_MEDIA_DATA_KEY = "new_media_data";

    private static final long UPDATE_INTERVAL = TimeUnit.HOURS.toMillis(18);

    public static final int REQUEST_COOL_DOWN = 1000;

    public static String sMovieDetailsStringInject;
    public static String sSeriesDetailsStringInject;
    private final Context mContext;
    private final AppDatabase mDatabase;
    private final long mUpdateThreshold;

    public UpdateMediaWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        mContext = context;
        mDatabase = AppDatabase.getInstance(mContext);
        mUpdateThreshold = new Date().getTime() - UPDATE_INTERVAL;
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
        List<MovieData> expiredMediaList =
                mDatabase.movieDataDao().getMediaAtOrBeforeThreshold(mUpdateThreshold);

        //weed out media that don't support notifiers
        List<MovieData> mediaToUpdateList = new ArrayList<>();
        for (MovieData movieData: expiredMediaList) {
            if (movieData.supportsNotifiers()) mediaToUpdateList.add(movieData);
        }

        //get movie detail url format
        String[] detailUrls = mContext.getResources().getStringArray(R.array.details_urls);
        String detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        //configure search preferences
        SearchPreferences searchPreferences = new SearchPreferences();

        //iterate through update list
        for (MovieData movieData: mediaToUpdateList) {
            SystemClock.sleep(REQUEST_COOL_DOWN);

            updateSavedMedia(movieData, detailUrl, searchPreferences);
        }
    }

    private void fetchSavedSeries() {
        //get all saved series
        List<SeriesData> expiredMediaList
                = mDatabase.seriesDataDao().getMediaAtOrBeforeThreshold(mUpdateThreshold);

        //weed out media that don't support notifiers
        List<SeriesData> mediaToUpdateList = new ArrayList<>();
        for (SeriesData seriesData: expiredMediaList) {
            if (seriesData.supportsNotifiers()) mediaToUpdateList.add(seriesData);
        }

        //get series detail url format
        String[] detailUrls = mContext.getResources().getStringArray(R.array.details_urls);
        String detailUrl = detailUrls[MEDIA_TYPE_SERIES];

        //configure search preferences
        SearchPreferences searchPreferences = new SearchPreferences();

        //iterate through update list
        for (SeriesData seriesData: mediaToUpdateList) {
            SystemClock.sleep(REQUEST_COOL_DOWN);

            updateSavedMedia(seriesData, detailUrl, searchPreferences);
        }
    }

    private void updateSavedMedia(final MediaData oldMediaData, final String detailUrl,
                                  final SearchPreferences searchPreferences) {
        String id = oldMediaData.getId();

        //build AN request
        ANRequest request =
                NetworkFunctions
                        .agnosticDetailRequestById(detailUrl, id,  searchPreferences, mContext);

        if (sSeriesDetailsStringInject != null || sMovieDetailsStringInject != null) {
            String detailsInject =
                    oldMediaData instanceof MovieData ? sMovieDetailsStringInject : sSeriesDetailsStringInject;

            parseAndApplyUpdatedJsonString(oldMediaData, detailsInject);

            return;
        }

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        parseAndApplyUpdatedJsonString(oldMediaData, returnedJSONString);
                    }
                });
            }

            @Override
            public void onError(final ANError anError) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, oldMediaData.getTitle() + " update data failed");

                        //notify user of error
                        if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                            retryAfterCoolDOwn(anError, oldMediaData, detailUrl, searchPreferences);
                        }
                    }
                });
            }
        });
    }

    private void parseAndApplyUpdatedJsonString(MediaData oldMediaData, String returnedJSONString) {
        MediaData newMediaData;

        //parse and populate retrieved data
        if (oldMediaData instanceof MovieData) {
            newMediaData = MovieDataParser.parseDetails(returnedJSONString, ((MovieData) oldMediaData));
            newMediaData.setLastUpdated(new Date().getTime());

            mDatabase.movieDataDao().updateMovieData(((MovieData) newMediaData));

        } else {
            newMediaData = SeriesDataParser.parseDetails(returnedJSONString, ((SeriesData) oldMediaData));
            newMediaData.setLastUpdated(new Date().getTime());

            mDatabase.seriesDataDao().updateSeriesData(((SeriesData) newMediaData));
        }

        boolean isProMode =
                mContext
                .getSharedPreferences(mContext.getString(R.string.application_shared_prefs_key),
                        Context.MODE_PRIVATE)
                        .getBoolean(mContext.getString(R.string.is_pro_mode_key),false);
        //if watch status supports notifiers and requires updates, launch update notifier service
        Intent intent;
        if (isProMode) {
            intent = new Intent(getApplicationContext(), UpdateNotifierService.class);
            intent.putExtra(NEW_MEDIA_DATA_KEY, Parcels.wrap(newMediaData));
            UpdateNotifierService.enqueueWork(mContext, intent);

        } else {
            intent = new Intent(getApplicationContext(), NoNotifierService.class);
            intent.putExtra(NEW_MEDIA_DATA_KEY, Parcels.wrap(newMediaData));
            NoNotifierService.enqueueWork(mContext, intent);
        }

        Log.d(TAG, newMediaData.getTitle() + " data updated");
    }

    private void retryAfterCoolDOwn(ANError anError, final MediaData mediaData,
                                    final String detailUrl, final SearchPreferences searchPreferences) {
        Log.d(TAG, mediaData.getTitle() + " retrying update");

        String coolDownString = anError.getResponse().header(ApiConstants.RETRY_AFTER_KEY);
        int coolDown = coolDownString != null ? Integer.parseInt(coolDownString) : 0;
        int coolDownInMilliSecs = coolDown * MILLISECOND_CONVERSION;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateSavedMedia(mediaData, detailUrl, searchPreferences);

            }
        }, coolDownInMilliSecs);
    }
}
