/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.WatchListsAdapter;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.GeneralUtils;
import com.atmko.onmywatch.view_models.ListsWatchAndUserViewModel;

import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

/**
 * The configuration screen for the {@link ListWidgetProvider NewAppWidget} AppWidget.
 */
public class ListWidgetProviderConfigureActivity extends AppCompatActivity
        implements WatchListsAdapter.OnListItemClickListener {
    private static final String TAG = "NewAppWidgetConfigureActivity";

    private static final String PREFS_NAME = "com.atmko.onmywatch.widget.NewAppWidget";

    private static final String PREF_PREFIX_KEY = "appwidget_";
    private static final String PREF_LIST_NAME_PREFIX_KEY = "list_name";
    private static final String PREF_MEDIA_TYPE_PREFIX_KEY = "media_type";

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private RecyclerView.Adapter mAdapter;
    private TextView mMediaTypeTextView;
    private RecyclerView mRecyclerView;
    private int mListType;
    private int mMediaType;

    public ListWidgetProviderConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget
    private static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId + PREF_LIST_NAME_PREFIX_KEY, text);
        prefs.apply();
    }

    static void saveMediaTypePref(Context context, int appWidgetId, int mediaType) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + PREF_MEDIA_TYPE_PREFIX_KEY, mediaType);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(
                PREF_PREFIX_KEY + appWidgetId + PREF_LIST_NAME_PREFIX_KEY, null);

        if (titleValue != null) {
            return titleValue;
        } else {
            return context.getString(R.string.appwidget_text);
        }
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static int loadMediaTypePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        Integer mediaTypeValue = prefs.getInt(
                PREF_PREFIX_KEY + appWidgetId + PREF_MEDIA_TYPE_PREFIX_KEY,
                MasterActivity.MEDIA_TYPE_SERIES);

        if (mediaTypeValue != null) {
            return mediaTypeValue;
        } else {
            //TODO return default media type
            return MasterActivity.MEDIA_TYPE_SERIES;
        }
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + PREF_LIST_NAME_PREFIX_KEY);
        prefs.apply();
    }

    static void deleteMediaTypePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + PREF_MEDIA_TYPE_PREFIX_KEY);
        prefs.apply();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.widget_list_provider_configure);

        //define views
        defineViews();

        //get watch lists
        observeData();

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            mMediaType = loadDefaultMediaType();
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
    }

    private void defineViews() {
        mMediaTypeTextView = findViewById(R.id.media_type_text_view);
        mMediaTypeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaType == (MEDIA_TYPE_SERIES)) {
                    mMediaType = MEDIA_TYPE_MOVIE;

                } else if (mMediaType == (MEDIA_TYPE_MOVIE)) {
                    mMediaType = MEDIA_TYPE_SERIES;
                }

                String mediaTitle = MediaData.getMediaTypeTitle(mMediaType,
                        ListWidgetProviderConfigureActivity.this);

                mMediaTypeTextView.setText(GeneralUtils.convertToDisplayText(mediaTitle));
            }
        });

        mRecyclerView = findViewById(R.id.lists_recycler_view);
        mRecyclerView.setLayoutManager(configureLayoutManager());

        mAdapter = new WatchListsAdapter(this);

        mRecyclerView.setAdapter(mAdapter);
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void observeData() {
        ListsWatchAndUserViewModel viewModel =
                ViewModelProviders.of(this).get(ListsWatchAndUserViewModel.class);

//        if (mAdapter instanceof WatchListsAdapter) {
        loadWatchLists(viewModel);

//        } else if (mAdapter instanceof UserListsAdapter) {
//            loadUserLists(viewModel);
//        }
    }

    private int loadDefaultMediaType() {
        int defaultMediaType = MasterActivity.MEDIA_TYPE_SERIES;

        //set initial media display text
        String mediaTypeText = MediaData.getMediaTypeTitle(defaultMediaType, this);

        mMediaTypeTextView.setText(GeneralUtils.convertToDisplayText(mediaTypeText));

        //return media type
        return defaultMediaType;
    }

    private void loadWatchLists(final ListsWatchAndUserViewModel viewModel) {
        viewModel.getWatchLists().observe(this, new Observer<List<WatchListModel>>() {
            @Override
            public void onChanged(List<WatchListModel> watchListModels) {
                ((WatchListsAdapter) mAdapter).getAdapterData().clear();
                ((WatchListsAdapter) mAdapter).addAdapterData(watchListModels);

                Log.d(TAG, "update watch lists");
            }
        });
    }

    @Override
    public void onItemClick(int position) {
        final Context context = ListWidgetProviderConfigureActivity.this;

        // When the button is clicked, store the string locally
        String listName = ((WatchListsAdapter) mAdapter).getAdapterData().get(position).getName();
        saveTitlePref(context, mAppWidgetId, listName);
        saveMediaTypePref(context, mAppWidgetId, mMediaType);

        // It is the responsibility of the configuration activity to update the app widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ListWidgetProvider.updateAppWidget(context, appWidgetManager, mAppWidgetId);

        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}

