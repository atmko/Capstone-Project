package com.atmko.onmywatch.utils;

import android.content.Context;
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

import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;

public class UpdateMediaWorker extends Worker {
    private static final String TAG = UpdateMediaWorker.class.getSimpleName();
    private static final int REQUEST_COOL_DOWN = 1000;

    Context mContext;
    AppDatabase mDatabase;

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

        //get movie detail url format
        String[] detailUrls = mContext.getResources().getStringArray(R.array.details_urls);
        String detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        //configure search preferences
        SearchPreferences searchPreferences = new SearchPreferences();

        //iterate through movie list with this watch status
        for (MovieData movieData: movieDataList) {
            SystemClock.sleep(REQUEST_COOL_DOWN);
            updateSavedMedia(movieData, detailUrl, searchPreferences);
        }
    }

    private void fetchSavedSeries() {
        //get all saved series
        List<SeriesData> seriesDataList = mDatabase.seriesDataDao()
                .getAllLSeriesAlt();

        //get series detail url format
        String[] detailUrls = mContext.getResources().getStringArray(R.array.details_urls);
        String detailUrl = detailUrls[MEDIA_TYPE_SERIES];

        //configure search preferences
        SearchPreferences searchPreferences = new SearchPreferences();

        //iterate through series list with this watch status
        for (SeriesData seriesData: seriesDataList) {
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

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        MediaData newMediaData;

                        //parse and populate retrieved data
                        if (oldMediaData instanceof MovieData) {
                            newMediaData =
                                    MovieDataParser.parseDetails(returnedJSONString, ((MovieData) oldMediaData));

                            //preserve the overwritten watch status and user rating
                            newMediaData.setWatchStatus(oldMediaData.getWatchStatus());
                            newMediaData.setUserRating(oldMediaData.getUserRating());

                            mDatabase.movieDataDao().updateMovieData(((MovieData) newMediaData));

                        } else {
                            newMediaData =
                                    SeriesDataParser
                                            .parseDetails(returnedJSONString, ((SeriesData) oldMediaData), mContext);

                            //preserve the over written watch status
                            newMediaData.setWatchStatus(oldMediaData.getWatchStatus());
                            newMediaData.setUserRating(oldMediaData.getUserRating());

                            mDatabase.seriesDataDao().updateSeriesData(((SeriesData) newMediaData));

                        }

                        Log.d(TAG, newMediaData.getTitle() + " data updated");

                    }
                });
            }

            @Override
            public void onError(ANError anError) {
                Log.d(TAG, oldMediaData.getTitle() + " update data failed");

                //notify user of error
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError, oldMediaData, detailUrl, searchPreferences);
                }
            }
        });
    }

    private void retryAfterCoolDOwn(ANError anError, final MediaData mediaData,
                                    final String detailUrl, final SearchPreferences searchPreferences) {
        Log.d(TAG, mediaData.getTitle() + " retrying update");

        int coolDown = Integer.valueOf(anError.getResponse().header(ApiConstants.RETRY_AFTER_KEY));
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
