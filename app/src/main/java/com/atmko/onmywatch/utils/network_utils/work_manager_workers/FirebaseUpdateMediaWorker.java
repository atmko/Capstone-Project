/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils.work_manager_workers;

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
import com.atmko.onmywatch.database.daos.FirebaseMovieDataDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.api_utils.MovieDataParser;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;
import com.atmko.onmywatch.utils.api_utils.SeriesDataParser;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;

/*
 * class for updating media saved through firebase
 */

public class FirebaseUpdateMediaWorker extends Worker {
    private static final String TAG = FirebaseUpdateMediaWorker.class.getSimpleName();
    private static final int REQUEST_COOL_DOWN = 1000;

    private final Context mContext;

    public FirebaseUpdateMediaWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        mContext = context;
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
        FirebaseMovieDataDao.getAllMovies()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        //TODO: make message when error occurs
                        if (task.getException() != null) return;
                        if (task.getResult() == null) return;

                        List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();

                        for (DocumentSnapshot documentSnapShot: documentSnapshots) {
                            if (documentSnapShot.getData() == null) return;

                            MovieData movieData =
                                    MovieData.parseDataMapToMediaData(documentSnapShot.getData());

                            //get movie detail url format
                            String[] detailUrls = mContext.getResources().getStringArray(R.array.details_urls);
                            String detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

                            //configure search preferences
                            SearchPreferences searchPreferences = new SearchPreferences();

                            //iterate through movie list with this watch status

                            SystemClock.sleep(REQUEST_COOL_DOWN);
                            updateSavedMedia(movieData, detailUrl, documentSnapShot.getId(), searchPreferences);
                        }
                    }
                });
    }

    private void fetchSavedSeries() {
        //get all saved series
        FirebaseSeriesDataDao.getAllSeries()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        //TODO: make message when error occurs
                        if (task.getException() != null) return;
                        if (task.getResult() == null) return;

                        List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();

                        for (DocumentSnapshot documentSnapShot: documentSnapshots) {
                            if (documentSnapShot.getData() == null) return;

                            SeriesData seriesData =
                                    SeriesData.parseDataMapToMediaData(documentSnapShot.getData());

                            //get series detail url format
                            String[] detailUrls = mContext.getResources().getStringArray(R.array.details_urls);
                            String detailUrl = detailUrls[MEDIA_TYPE_SERIES];

                            //configure search preferences
                            SearchPreferences searchPreferences = new SearchPreferences();

                            //iterate through series list with this watch status

                            SystemClock.sleep(REQUEST_COOL_DOWN);
                            updateSavedMedia(seriesData, detailUrl, documentSnapShot.getId(), searchPreferences);
                        }
                    }
                });
    }

    private void updateSavedMedia(final MediaData oldMediaData, final String detailUrl,
                                  final String documentId, final SearchPreferences searchPreferences) {
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
                        final MediaData newMediaData;
                        Task<Void> mediaUpdateTask;
                        Map<String, Object> mediaDataMap;

                        if (oldMediaData instanceof MovieData) {
                            newMediaData = MovieDataParser.parseDetails(returnedJSONString,
                                    ((MovieData) oldMediaData));

                            mediaDataMap = ((MovieData) newMediaData).parseMediaDataToDataMap();

                            mediaUpdateTask = FirebaseMovieDataDao
                                    .updateMovieData(documentId, mediaDataMap);

                        } else {
                            newMediaData = SeriesDataParser.parseDetails(returnedJSONString,
                                    ((SeriesData) oldMediaData));

                            mediaDataMap = ((SeriesData) newMediaData).parseMediaDataToDataMap();

                            mediaUpdateTask = FirebaseSeriesDataDao
                                    .updateSeriesData(documentId, mediaDataMap);
                        }

                        mediaUpdateTask.addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.getException() != null) {
                                    notifyUpdateFailure(oldMediaData, task.getException(),
                                            detailUrl, documentId, searchPreferences);

                                } else {
                                    Log.d(TAG, newMediaData.getTitle() + " data updated");

                                }
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(ANError anError) {
                notifyUpdateFailure(oldMediaData, anError, detailUrl, documentId, searchPreferences);
            }
        });
    }

    //helper method to notify of error in updating media
    private void notifyUpdateFailure(MediaData oldMediaData, Exception exception,
                                     String detailUrl, String documentId,
                                     SearchPreferences searchPreferences) {
        Log.d(TAG, oldMediaData.getTitle() + " update data failed");

        //notify user of error
        if (exception instanceof  ANError) {
            if (((ANError) exception).getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                retryAfterCoolDOwn(((ANError) exception), oldMediaData,
                        detailUrl, documentId, searchPreferences);
            }
        }
    }

    private void retryAfterCoolDOwn(ANError anError, final MediaData mediaData,
                                    final String detailUrl, final String documentId,
                                    final SearchPreferences searchPreferences) {
        Log.d(TAG, mediaData.getTitle() + " retrying update");

        int coolDown = Integer.valueOf(anError.getResponse().header(ApiConstants.RETRY_AFTER_KEY));
        int coolDownInMilliSecs = coolDown * MILLISECOND_CONVERSION;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateSavedMedia(mediaData, detailUrl, documentId, searchPreferences);

            }
        }, coolDownInMilliSecs);
    }
}