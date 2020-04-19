/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.atmko.onmywatch.Fragments.ListWatchAndUserFragment;
import com.atmko.onmywatch.Fragments.ListsWatchAndUserParentFragment;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.ListWatchAndUserAdapter;
import com.atmko.onmywatch.adapters.ListsAdapter;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.utils.GeneralUtils;
import com.atmko.onmywatch.view_models.MasterActivityViewModel;

import java.util.List;

import static com.atmko.onmywatch.Fragments.ListsWatchAndUserParentFragment.LIST_TYPE_WATCH;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.MasterActivity.getCurrentUser;

/**
 * The configuration screen for the {@link ListWidgetProvider NewAppWidget} AppWidget.
 */
public class ListWidgetProviderConfigureActivity extends AppCompatActivity
        implements ListsWatchAndUserParentFragment.ListFragmentImplementation,
        ListWatchAndUserAdapter.LogicImplementation,
        ListWatchAndUserFragment.OnListModelClickListener {

    private static final String PREFS_NAME = "com.atmko.onmywatch.widget.NewAppWidget";

    private static final String PREF_PREFIX_KEY = "appwidget_";
    private static final String PREF_LIST_NAME_PREFIX_KEY = "list_name";
    private static final String PREF_LIST_TYPE_PREFIX_KEY = "list_type";
    private static final String PREF_MEDIA_TYPE_PREFIX_KEY = "media_type";

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private TextView mMediaTypeTextView;
    private int mMediaType;
    private boolean mIsProMode;
    private String[] mListTypeNames;

    public ListWidgetProviderConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget
    private static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId + PREF_LIST_NAME_PREFIX_KEY, text);
        prefs.apply();
    }

    private static void saveListTypePref(Context context, int appWidgetId, int listType) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + PREF_LIST_TYPE_PREFIX_KEY, listType);
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
    static int loadListTypePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);

        return prefs.getInt(
                PREF_PREFIX_KEY + appWidgetId + PREF_LIST_TYPE_PREFIX_KEY, LIST_TYPE_WATCH);
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

    public static void deleteListTypePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + PREF_LIST_TYPE_PREFIX_KEY);
        prefs.apply();
    }

    static void deleteMediaTypePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + PREF_MEDIA_TYPE_PREFIX_KEY);
        prefs.apply();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.widget_list_provider_configure);

        //if current user is null start login
        if (getCurrentUser() == null) {
            Toast.makeText(this, "You're not logged in", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //define views
        defineViews();

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

        } else {
            observeData(savedInstanceState);
        }
    }

    private void loadUi(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            ListsWatchAndUserParentFragment listsParentFragment =
                    ListsWatchAndUserParentFragment.newInstance(mListTypeNames, false);

            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_down_entry, android.R.animator.fade_out)
                    .add(R.id.master_fragments_container, listsParentFragment,
                            ListsWatchAndUserParentFragment.FRAGMENT_KEY)
                    .commit();
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
    }

    private void observeData(final Bundle savedInstanceState) {
        MasterActivityViewModel masterActivityViewModel =
                ViewModelProviders.of(this).get(MasterActivityViewModel.class);

        masterActivityViewModel.getIsProModeLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isProMode) {
                if (isProMode == null) return;

                mIsProMode = isProMode;
                if (!mIsProMode) {
                    mListTypeNames = getResources().getStringArray(R.array.list_type_titles);

                } else {
                    mListTypeNames = getResources().getStringArray(R.array.list_type_titles_pro);
                }

                List<Fragment> fragments = getSupportFragmentManager().getFragments();
                if (fragments.size() != 0) {
                    getSupportFragmentManager().beginTransaction().remove(fragments.get(0));
                }

                loadUi(savedInstanceState);
            }
        });
    }

    private int loadDefaultMediaType() {
        int defaultMediaType = MasterActivity.MEDIA_TYPE_SERIES;

        //set initial media display text
        String mediaTypeText = MediaData.getMediaTypeTitle(defaultMediaType, this);

        mMediaTypeTextView.setText(GeneralUtils.convertToDisplayText(mediaTypeText));

        //return media type
        return defaultMediaType;
    }

    @Override
    public void onListFragmentResume(Fragment fragment) {

    }

    @Override
    public void onAnimationEnd(Fragment fragment) {

    }

    @Override
    public Fragment launchFragment(int position) {
        ListWatchAndUserFragment fragment = null;
        if (position == LIST_TYPE_WATCH) {
            fragment = ListWatchAndUserFragment.newInstance(LIST_TYPE_WATCH, false);

        } else if (position == ListsWatchAndUserParentFragment.LIST_TYPE_USER) {
            fragment = ListWatchAndUserFragment
                    .newInstance(ListsWatchAndUserParentFragment.LIST_TYPE_USER, false);

        } else if (position == ListsWatchAndUserParentFragment.LIST_TYPE_AUTO) {
            fragment = ListWatchAndUserFragment
                    .newInstance(ListsWatchAndUserParentFragment.LIST_TYPE_AUTO, false);
        }

        return fragment;
    }

    @Override
    public void onListModelClick(ListsAdapter adapter, Fragment childFragment, int listType,
                                 ListModel listModel) {
        if (adapter.inPlaceholderMode()) {
            if (childFragment.getParentFragment() != null) {
                MasterActivity.launchCreateListActivity(this);

                return;
            }
        }

        final Context context = ListWidgetProviderConfigureActivity.this;

        // When the button is clicked, store the string locally
        saveTitlePref(context, mAppWidgetId, listModel.getName());
        saveListTypePref(context, mAppWidgetId, listType);
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

