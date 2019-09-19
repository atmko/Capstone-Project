package com.atmko.onmywatch.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.atmko.onmywatch.utils.network_utils.NetworkFunctions;
import com.bumptech.glide.request.FutureTarget;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ListRemoteViewFactoryService extends RemoteViewsService {
    private static final String TAG = "ListRemteViewFactorySer";

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
    private static final String TAG = "ListRemoteViewsFactory";

    Context mContext;
    AppDatabase mDatabase;
    int mAppWidgetId;
    String mListName;
    int mMediaType;
    List<MovieData> mMovieDataList;
    List<SeriesData> mSeriesDataList;

    public ListRemoteViewsFactory(Context applicationContext, int appWidgetId) {
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
        final String[] watchStatusMoviesTitles =
                mContext.getResources().getStringArray(R.array.watch_status_movie_titles);
        final List<String> titleList = Arrays.asList(watchStatusMoviesTitles);

        mListName = ListWidgetProviderConfigureActivity.loadTitlePref(mContext, mAppWidgetId);
        Log.d(TAG, "listName: " + mListName);

        mMediaType = ListWidgetProviderConfigureActivity.loadMediaTypePref(mContext, mAppWidgetId);
        Log.d(TAG, "media type: " + mMediaType);

        //if media data is movie
        if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            mMovieDataList = mDatabase.movieDataDao()
                    .getMoviesByWatchStatusAlt(titleList.indexOf(mListName));

            Log.d(TAG, "mMovieDataList size: " + mMovieDataList.size());

        //if media data is series
        } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            mSeriesDataList = mDatabase.seriesDataDao()
                    .getSeriesByWatchStatusAlt(titleList.indexOf(mListName));

            Log.d(TAG, "mSeriesDataList: " + mSeriesDataList.size());
        }
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            Log.d(TAG, "getCount: " + mMovieDataList.size());
            if (mMovieDataList == null) return 0;

            return mMovieDataList.size();

        } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES){
            Log.d(TAG, "getCount: " + mSeriesDataList.size());
            if (mSeriesDataList == null) return 0;

            return mSeriesDataList.size();

        } else {
            return 0;
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        final RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_object);

        final String title;
        final String backdropUrl;

        if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            title = mMovieDataList.get(position).getTitle();
            backdropUrl = mMovieDataList.get(position).getBackdropPath();

        } else {
            title = mSeriesDataList.get(position).getTitle();
            backdropUrl = mSeriesDataList.get(position).getBackdropPath();

        }

        Log.d(TAG, "title: " + title);
        Log.d(TAG, "backdropUrl: " + backdropUrl);

        remoteViews.setTextViewText(R.id.title_text_view, title);

        loadImageForListItem(mContext, backdropUrl, remoteViews);

        return remoteViews;
    }

    private void loadImageForListItem(
            Context context, String pathName, RemoteViews remoteViews) {

        int width  = 780;
        int height = 439;

        FutureTarget futureTarget =
                NetworkFunctions.loadWidgetImage(context, pathName, width, height);

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
        return 1;
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