/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.work.Constraints;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.atmko.onmywatch.Fragments.DetailsFragment;
import com.atmko.onmywatch.Fragments.HomeFragment;
import com.atmko.onmywatch.Fragments.ListResultsParentFragment;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.utils.SearchPreferences;
import com.atmko.onmywatch.utils.UpdateMediaWorker;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.parceler.Parcels;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MasterActivity extends AppCompatActivity {

    private static final String DEFAULT_MEDIA_KEY = "default_media";

    public static final int MEDIA_TYPE_SERIES = 0;
    public static final int MEDIA_TYPE_MOVIE = 1;
    public static final int MEDIA_TYPE_PEOPLE = 2;

    public static final String KEYBOARD_VISIBILITY_KEY = "keyboard_visibility";

    public static final String SEARCH_TEXT_KEY = "search_text";
    public static final String SEARCH_BAR_VISIBILITY_KEY = "visible_search_bar";

    public static final String ACTION_LAUNCH_DETAILS = "launch_details";

    public static final int REPEAT_INTERVAL = 2;
    public static final int INITIAL_DELAY = 15;

    //for restoring keyboard visibility upon configuration change
    private boolean mIsKeyboardVisible;
    private boolean mIsTabletLandscape;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        initializeAdMob();

        //set / restore values
        setValues(savedInstanceState);

        mIsTabletLandscape = getResources().getBoolean(R.bool.isTabletLandscape);

        // Obtain Analytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        if (savedInstanceState == null) {
            startHomeFragment();
            //start background work managers
            startWorkers();

        }

        if (getIntent()!= null) {
            Intent intent = getIntent();

            if (intent.getAction().equals(ACTION_LAUNCH_DETAILS)) {
                launchDetailsFromIntent(intent);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction().equals(ACTION_LAUNCH_DETAILS)) {
            launchDetailsFromIntent(intent);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //save keyboard visibility value
        outState.putBoolean(KEYBOARD_VISIBILITY_KEY, mIsKeyboardVisible);
    }

    private void initializeAdMob() {
        try {
            String adMobIdKey = getString(R.string.ad_mob_application_id_key);

            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(),
                    PackageManager.GET_META_DATA);
            Bundle applicationMetaData = applicationInfo.metaData;

            String adMobId = applicationMetaData.getString(adMobIdKey);

            MobileAds.initialize(this, adMobId);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setValues(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            //restore keyboard visibility value
            mIsKeyboardVisible =
                    savedInstanceState.getBoolean(KEYBOARD_VISIBILITY_KEY);
        }
    }

    //condition 1
    //hides master fragment if details fragment is loaded (only when two non two pane)
    //condition 2
    //hides fragment if its not active in master container
    public void onResumeMasterContainerFragment(Fragment fragment) {
        //condition 1
        //if not tablet landscape (not two pane)
        //&& there's a fragment in details container
        //hide fragment
        if (!mIsTabletLandscape && hasFragment(R.id.detail_fragments_container)) {
            //hide background fragment to reserve keyboard focus for newly loaded fragment
            fragment.getView().findViewById(R.id.top_layout).setVisibility(View.GONE);

        }

        //condition 2
        //compare master fragment name and resumed fragment name
        //if no match, hide fragment
        String masterContainerFragmentName =
                getSupportFragmentManager()
                        .findFragmentById(R.id.master_fragments_container)
                        .getClass().getSimpleName();

        String resumedFragmentName = fragment.getClass().getSimpleName();

        if (!masterContainerFragmentName.equals(resumedFragmentName)) {

            //hide background fragment to reserve keyboard focus for newly loaded fragment
            hideFragment(fragment);
        }
    }

    private void startHomeFragment() {
        HomeFragment homeFragment = HomeFragment.newInstance();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.master_fragments_container, homeFragment, HomeFragment.FRAGMENT_KEY)
                .commit();
    }

    private void startWorkers() {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build();

        PeriodicWorkRequest updateMediaDataRequest =
                new PeriodicWorkRequest.Builder(
                        UpdateMediaWorker.class, REPEAT_INTERVAL, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .setInitialDelay(INITIAL_DELAY, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueue(updateMediaDataRequest);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();

            return true;

        }else {
            return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onBackPressed() {
        Fragment detailFragment =
                getSupportFragmentManager().findFragmentById(R.id.detail_fragments_container);
        Fragment fragment =
                getSupportFragmentManager().findFragmentById(R.id.master_fragments_container);

        //condition for navigation
        //this removes details fragment because master container is behind detail container
        // (via frame layout) in non tablet landscape
        if (hasFragment(R.id.detail_fragments_container) && !mIsTabletLandscape) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                    .remove(detailFragment).commit();


        }else {
            //condition for exit animation transition
            if (fragment instanceof ListResultsParentFragment) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                        .remove(fragment).commit();


            //has fragment
            //&&fragment is home fragment
            } else if (hasFragment(R.id.master_fragments_container) && fragment instanceof HomeFragment) {
                finish();


            //condition for navigation
            } else if (hasFragment(R.id.master_fragments_container)) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_down_entry, R.anim.slide_up_exit)
                        .remove(fragment)
                        .commit();


            } else {
                super.onBackPressed();

            }
        }

        //finish above transaction to prevent null data
        getSupportFragmentManager().executePendingTransactions();
        fragment = getSupportFragmentManager().findFragmentById(R.id.master_fragments_container);

        //show hidden background fragment
        fragment.getView().findViewById(R.id.top_layout).setVisibility(View.VISIBLE);

        //set toolbar
        Toolbar toolbar = fragment.getView().findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //restore search bar visibility if title is hidden
        //exclude fragments that don't have search bar
        if (!(fragment instanceof HomeFragment)) {
            TextView titleTextView =
                    fragment.getView().findViewById(R.id.title_text_view);

            if (titleTextView.getVisibility() != View.VISIBLE) {
                SuperEditText searchTextView =
                        fragment.getView().findViewById(R.id.search_edit_text_view);
                showSearchBar(searchTextView);
            }
        }
    }

    public boolean isTabletLandscape(){
        return mIsTabletLandscape;
    }

    public boolean hasFragment(int containerId) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(containerId);

        return fragment != null;
    }

    public void launchDetailsFragment(Fragment fragment, MediaData selectedData) {
        //catch error from restoring fragments after configuration change
        try {
            getSupportFragmentManager().executePendingTransactions();

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        int mediaType = selectedData instanceof MovieData ? MEDIA_TYPE_MOVIE : MEDIA_TYPE_SERIES;

        String[] detailUrls = getResources().getStringArray(R.array.details_urls);
        String detailUrl = null;

        if (mediaType == MEDIA_TYPE_MOVIE) {
            detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        } else if (mediaType == MEDIA_TYPE_SERIES) {
            detailUrl = detailUrls[MEDIA_TYPE_SERIES];
        }

        Parcelable parceledData = Parcels.wrap(selectedData);
        SearchPreferences searchPreferences =  new SearchPreferences();
        Parcelable parceledSharedPreferences = Parcels.wrap(searchPreferences);

        DetailsFragment detailsFragment = DetailsFragment.newInstance(
                mediaType, detailUrl, parceledData, parceledSharedPreferences);

        Fragment detailContainerFragment = getSupportFragmentManager()
                .findFragmentById(R.id.detail_fragments_container);

        ///remove existing fragment
        if (detailContainerFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                    .remove(detailContainerFragment).commit();

        }

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                .add(R.id.detail_fragments_container, detailsFragment, DetailsFragment.FRAGMENT_KEY)
                .commit();
    }

    private void launchDetailsFromIntent(Intent intent) {
        MediaData mediaData =
                Parcels.unwrap(intent.getParcelableExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY));
        launchDetailsFragment(null, mediaData);
    }

    public void launchAddToListActivity(MediaData mediaData) {
        //fix for stack placeholder, do nothing if no id
        if (mediaData.getId() == null) return;

        int mediaType = mediaData instanceof MovieData ? MEDIA_TYPE_MOVIE : MEDIA_TYPE_SERIES;

        Intent intent = new Intent(getApplicationContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, mediaType);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(mediaData));

        startActivity(intent);
    }

    public void hideBackgroundFragment(Fragment fragment) {
        if (fragment.getView() != null) {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();

            int fragmentIndex = fragments.indexOf(fragment);
            int backgroundFragmentIndex = fragmentIndex - 1;

            Fragment backgroundFragment = fragments.get(backgroundFragmentIndex);

            if (backgroundFragment instanceof DetailsFragment) {
                backgroundFragmentIndex = backgroundFragmentIndex - 1;
                backgroundFragment = fragments.get(backgroundFragmentIndex);
            }

            Toast.makeText(this, backgroundFragment.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
            hideFragment(backgroundFragment);

            //hide search bar and dismiss keyboard
            //exclude fragments that don't have search bar
            if (!(backgroundFragment instanceof HomeFragment)) {
                SuperEditText searchTextView =
                backgroundFragment.getView().findViewById(R.id.search_edit_text_view);
                hideSearchBar(searchTextView);
            }
        }
    }

    private void hideFragment(Fragment fragment) {
        View backgroundFragmentParentView =
                fragment.getView().findViewById(R.id.top_layout);

        backgroundFragmentParentView.setVisibility(View.GONE);
    }

    public void onSearchButtonPressed(ImageButton searchButton,
                                      SuperEditText searchEditText, TextView toolbarTitle) {
        if (searchEditText.getVisibility() == View.VISIBLE) {
            searchEditText.setText("");
            searchButton.setImageResource(R.drawable.ic_manual_search);
            hideSearchBar(searchEditText);
            toolbarTitle.setVisibility(View.VISIBLE);

        } else {
            searchButton.setImageResource(R.drawable.ic_cancel_manual_search);
            searchEditText.setVisibility(View.VISIBLE);
            searchEditText.requestFocus();
            showSoftKeyboard(searchEditText);
            toolbarTitle.setVisibility(View.GONE);
        }
    }

    //search restore convenience method
    public void restoreSavedSearch(Fragment fragment, boolean firstInit, Bundle savedInstanceState,
                                   ImageButton searchButton, SuperEditText searchTextView) {

        //show keyboard if restore value is true
        //else hide it
        if (mIsKeyboardVisible) {
            showSoftKeyboard(searchTextView);

        } else {
            hideSoftKeyboard(searchTextView);
        }

        String savedSearchText;
        int savedBarVisibility;

        if (savedInstanceState != null && firstInit) {
            savedSearchText = savedInstanceState.getString(SEARCH_TEXT_KEY);
            savedBarVisibility = savedInstanceState.getInt(SEARCH_BAR_VISIBILITY_KEY);

        } else {
            savedSearchText = searchTextView.getText().toString();
            savedBarVisibility = searchTextView.getVisibility();
        }

        searchTextView.setText(savedSearchText);
        searchTextView.setVisibility(savedBarVisibility);

        if (savedBarVisibility == View.VISIBLE) {
            searchButton.setImageResource(R.drawable.ic_cancel_manual_search);

            fragment.getParentFragment()
                    .getView().findViewById(R.id.title_text_view).setVisibility(View.GONE);

        } else {
            searchButton.setImageResource(R.drawable.ic_manual_search);

        }
    }

    //hide soft keyboard and update keyboard visibility property
    public void hideSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            mIsKeyboardVisible = false;
        }
    }

    //show soft keyboard and update keyboard visibility property
    private void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);

            mIsKeyboardVisible = true;
        }
    }

    //convenience method for hiding search bar edit text
    public void hideSearchBar(SuperEditText searchEditText) {
        hideSoftKeyboard(searchEditText);
        searchEditText.setVisibility(View.GONE);
    }

    //convenience method for showing search bar edit text
    public void showSearchBar(SuperEditText searchEditText) {
        searchEditText.setVisibility(View.VISIBLE);
    }
}
