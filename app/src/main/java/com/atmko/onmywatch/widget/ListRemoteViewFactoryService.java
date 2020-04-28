/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.atmko.onmywatch.fragments.DetailsFragment;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaLog;
import com.atmko.onmywatch.models.MovieLog;
import com.atmko.onmywatch.models.SeriesLog;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
import com.bumptech.glide.request.FutureTarget;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.atmko.onmywatch.fragments.ListsWatchAndUserParentFragment.LIST_TYPE_AUTO;
import static com.atmko.onmywatch.fragments.ListsWatchAndUserParentFragment.LIST_TYPE_USER;
import static com.atmko.onmywatch.fragments.ListsWatchAndUserParentFragment.LIST_TYPE_WATCH;

public class ListRemoteViewFactoryService extends RemoteViewsService {
    private static final String TAG = "ListRemoteViewFactory";

    public static final String APP_WIDGET_ID_KEY = "app_widget_id";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Bundle intentExtras = intent.getExtras();

        Log.d(TAG, "creating new ListRemoteViewsFactory");

        int appWidgetId = 0;

        if (intentExtras != null) {

            if (intentExtras.containsKey(APP_WIDGET_ID_KEY)) {
                appWidgetId =
                        intent.getIntExtra(APP_WIDGET_ID_KEY, 0);

                Log.d(TAG, "appWidgetId : " + appWidgetId);

            } else {
                Log.d(TAG, "no appWidgetId given");

            }
        }

        return new ListRemoteViewsFactory(this.getApplicationContext(), appWidgetId);
    }
}

class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final int WIDGET_BACKDROP_WIDTH = 780;
    private static final int WIDGET_BACKDROP_HEIGHT = 439;

    private final Context mContext;
    private final AppDatabase mDatabase;
    private final int mAppWidgetId;
    private int mListType;
    private int mMediaType;
    private List<MovieData> mMovieDataList;
    private List<SeriesData> mSeriesDataList;
    private List<MovieLog> mMovieLogs;
    private List<SeriesLog> mSeriesLogs;

    ListRemoteViewsFactory(Context applicationContext, int appWidgetId) {
        mContext = applicationContext;
        mDatabase = AppDatabase.getInstance(mContext);
        mAppWidgetId = appWidgetId;
        mMovieDataList = new ArrayList<>();
        mSeriesDataList = new ArrayList<>();
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        getListData();
    }

    private void getListData() {
        final String[] watchStatusTitles =
                mContext.getResources().getStringArray(R.array.watch_status_titles);
        final List<String> titleList = Arrays.asList(watchStatusTitles);

        String mListName = ListWidgetProviderConfigureActivity.loadTitlePref(mContext, mAppWidgetId);
        mListType = ListWidgetProviderConfigureActivity.loadListTypePref(mContext, mAppWidgetId);
        mMediaType = ListWidgetProviderConfigureActivity.loadMediaTypePref(mContext, mAppWidgetId);

        if (mListType == LIST_TYPE_WATCH) {
            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                mMovieDataList = mDatabase.movieDataDao()
                        .getMoviesByWatchStatusAlt(titleList.indexOf(mListName));

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                mSeriesDataList = mDatabase.seriesDataDao()
                        .getSeriesByWatchStatusAlt(titleList.indexOf(mListName));
            }

        } else if (mListType == LIST_TYPE_USER) {
            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                mMovieDataList = mDatabase.movieDataRecordsDao()
                        .getAllMoviesInListAlt((mListName));

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                mSeriesDataList = mDatabase.seriesDataRecordsDao()
                        .getAllSeriesInListAlt(mListName);
            }

        } else if (mListType == LIST_TYPE_AUTO) {
            boolean isProMode =
                    mContext
                    .getSharedPreferences(mContext.getString(R.string.application_shared_prefs_key),
                            Context.MODE_PRIVATE)
                    .getBoolean(mContext.getString(R.string.is_pro_mode_key),false);

            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                if (isProMode) {
                    mMovieLogs = mDatabase.movieLogsDao().getUpcomingAlt();

                } else {
                    mMovieLogs = new ArrayList<>();
                }

            } else {
                if (isProMode) {
                    mSeriesLogs = mDatabase.seriesLogsDao().getUpcomingAlt();

                } else {
                    mSeriesLogs = new ArrayList<>();
                }
            }
        }
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            if (mListType == LIST_TYPE_AUTO) {
                if (mMovieLogs == null) return 0;
                return mMovieLogs.size();

            } else {
                if (mMovieDataList == null) return 0;
                return mMovieDataList.size();
            }

        } else {
            if (mListType == LIST_TYPE_AUTO) {
                if (mSeriesLogs == null) return 0;
                return mSeriesLogs.size();

            } else {
                if (mSeriesDataList == null) return 0;
                return mSeriesDataList.size();
            }
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        final RemoteViews remoteViews;
        if (mListType == LIST_TYPE_AUTO) {
            remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_log_object);

        } else {
            remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_object);
        }

        MediaData mediaData = null;
        MediaLog mediaLog = null;
        final String title;
        String countdown = null;
        String type = null;
        final String backdropUrl;

        if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            if (mListType == LIST_TYPE_AUTO) {
                mediaLog = mMovieLogs.get(position);
                title = mMovieLogs.get(position).title;
                countdown = mMovieLogs.get(position).getCountdown();
                backdropUrl = mMovieLogs.get(position).backdropPath;

            } else {
                mediaData = mMovieDataList.get(position);
                title = mMovieDataList.get(position).getTitle();
                backdropUrl = mMovieDataList.get(position).getBackdropPath();
            }

        } else {
            if (mListType == LIST_TYPE_AUTO) {
                mediaLog = mSeriesLogs.get(position);
                title = mSeriesLogs.get(position).title;
                countdown = mSeriesLogs.get(position).getCountdown();
                type = mSeriesLogs.get(position).getTypeString();
                backdropUrl = mSeriesLogs.get(position).backdropPath;

            } else {
                mediaData = mSeriesDataList.get(position);
                title = mSeriesDataList.get(position).getTitle();
                backdropUrl = mSeriesDataList.get(position).getBackdropPath();
            }
        }

        remoteViews.setTextViewText(R.id.title_text_view, title);
        if (mListType == LIST_TYPE_AUTO) {
            remoteViews.setTextViewText(R.id.count_down_text, countdown);
            remoteViews.setTextViewText(R.id.type_text_view, type);
        }

        loadImageForListItem(mContext, backdropUrl, remoteViews);

        //configure fill in intent
        Bundle detailsExtras = new Bundle();

        Intent detailsFillInIntent = new Intent();
        if (mListType == LIST_TYPE_AUTO) {
            detailsFillInIntent.putExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mediaLog));

        } else {
            detailsFillInIntent.putExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mediaData));
        }

        detailsFillInIntent.putExtras(detailsExtras);
        remoteViews.setOnClickFillInIntent(R.id.backdrop_image_view, detailsFillInIntent);

        //configure fill in intent
        Bundle shareExtras = new Bundle();
        shareExtras.putString(DetailsFragment.QUICK_ACTION_KEY, DetailsFragment.QUICK_ACTION_SHARE);

        Intent shareFillInIntent = new Intent();
        if (mListType == LIST_TYPE_AUTO) {
            shareFillInIntent.putExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mediaLog));

        } else {
            shareFillInIntent.putExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mediaData));
        }
        shareFillInIntent.putExtras(shareExtras);
        remoteViews.setOnClickFillInIntent(R.id.share_button, shareFillInIntent);

        //configure fill in intent
        Bundle rateExtras = new Bundle();
        rateExtras.putString(DetailsFragment.QUICK_ACTION_KEY, DetailsFragment.QUICK_ACTION_RATE);

        Intent rateInIntent = new Intent();
        if (mListType == LIST_TYPE_AUTO) {
            rateInIntent.putExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mediaLog));

        } else {
            rateInIntent.putExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mediaData));
        }
        rateInIntent.putExtras(rateExtras);
        remoteViews.setOnClickFillInIntent(R.id.rate_button, rateInIntent);

        return remoteViews;
    }

    private void loadImageForListItem(
            Context context, String pathName, RemoteViews remoteViews) {

        FutureTarget futureTarget =
                NetworkFunctions.loadWidgetImage(context, pathName, WIDGET_BACKDROP_WIDTH,
                        WIDGET_BACKDROP_HEIGHT);

        try {
            remoteViews.setImageViewBitmap(R.id.backdrop_image_view, (Bitmap) futureTarget.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}