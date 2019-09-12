package com.atmko.onmywatch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.atmko.onmywatch.Fragments.DetailsFragment;
import com.atmko.onmywatch.Fragments.HomeFragment;
import com.atmko.onmywatch.Fragments.ListResultsParentFragment;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.utils.SearchPreferences;
import com.atmko.onmywatch.utils.UpdateMediaWorker;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.parceler.Parcels;

import java.util.ArrayList;

public class MasterActivity extends AppCompatActivity {

    private static final String DEFAULT_MEDIA_KEY = "default_media";

    public static final int MEDIA_TYPE_SERIES = 0;
    public static final int MEDIA_TYPE_MOVIE = 1;
    public static final int MEDIA_TYPE_PEOPLE = 2;

    private boolean mIsTabletLandscape;
    private FirebaseAnalytics mFirebaseAnalytics;

    //list of focusable views to be saved for restore
    //each index resents an added fragment
    //nested index represents the fragment's views
    private ArrayList<ArrayList<View>> focusableViewList;
    //list of last focus before launching new fragment
    //each index represents an added fragment
    private ArrayList<View> previousFocusList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        //set / restore values
        setValues();

        mIsTabletLandscape = getResources().getBoolean(R.bool.isTabletLandscape);

        // Obtain Analytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        if (savedInstanceState == null) {
            startHomeFragment();
            //start background work managers
            startWorkers();

        }
    }

    private void setValues() {
        focusableViewList = new ArrayList<>();
        previousFocusList = new ArrayList<>();
    }

    //condition 1
    //hides master fragment if details fragment is visible (only when two non two pane)
    //condition 2
    //hides fragment if its not active in master container
    public void onResumeMasterContainerFragment(Fragment fragment) {
        //condition 1
        //if not tablet landscape (not two pane)
        //&& there's a fragment in details container
        //&& details fragment is visible
        //hide fragment
        if (!mIsTabletLandscape && hasFragment(R.id.detail_fragments_container)) {
            if (getSupportFragmentManager().findFragmentById(R.id.detail_fragments_container)
                    .isVisible()) {

                //hide background fragment to reserve keyboard focus for newly loaded fragment
                fragment.getView().findViewById(R.id.top_layout).setVisibility(View.GONE);
            }
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
            fragment.getView().findViewById(R.id.top_layout).setVisibility(View.GONE);
        }
    }

    private void startHomeFragment() {
        HomeFragment homeFragment = HomeFragment.newInstance();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.master_fragments_container, homeFragment, HomeFragment.FRAGMENT_KEY)
                .commit();
    }

    private void startWorkers() {
        Constraints constraints = new Constraints.Builder().build();

        OneTimeWorkRequest updateMediaDataRequest =
                new OneTimeWorkRequest.Builder(UpdateMediaWorker.class)
                        .setConstraints(constraints)
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
        Fragment detailFragment = getSupportFragmentManager().findFragmentById(R.id.detail_fragments_container);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.master_fragments_container);

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
        if (focusableViewList.size() != 0) {
            //restore fragment focus to allow keyboard focus
            onFragmentResume(fragment, fragment.getView().findViewById(R.id.top_layout));

        }

        //show hidden background fragment
        fragment.getView().findViewById(R.id.top_layout).setVisibility(View.VISIBLE);
    }

    public void onSearchButtonPressed(EditText searchEditText, TextView toolbarTitle) {
        if (searchEditText.getVisibility() == View.VISIBLE) {
            searchEditText.setVisibility(View.GONE);
            toolbarTitle.setVisibility(View.VISIBLE);

        } else {
            searchEditText.setVisibility(View.VISIBLE);
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            toolbarTitle.setVisibility(View.GONE);
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

        DetailsFragment detailsFragment =
                DetailsFragment.newInstance(mediaType, detailUrl, parceledData, parceledSharedPreferences);

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

        //if not tablet landscape (two pane)
        if (!mIsTabletLandscape) {
            //save focusable views and remove focus to reserve keyboard focus for newly loaded fragment
            View parentView = fragment.getParentFragment().getView().findViewById(R.id.top_layout);
            onFragmentPause(fragment, parentView);
        }
    }

    public void launchAddToListActivity(MediaData mediaData) {
        int mediaType = mediaData instanceof MovieData ? MEDIA_TYPE_MOVIE : MEDIA_TYPE_SERIES;

        Intent intent = new Intent(getApplicationContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, mediaType);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(mediaData));

        startActivity(intent);
    }

    //source: https://stackoverflow.com/questions/46034391/disable-focus-on-fragment
    //user:Veljko
    //date: Feb 7 '18
    public void onFragmentResume(Fragment fragment, View parentView) {
        int fragmentIndex = focusableViewList.size() - 1;

        //Enable focus
        if (fragment.getView() != null) {

            //Enable focus
            setEnableView((ViewGroup) parentView, true, fragmentIndex);

            //Clear focusable elements
            focusableViewList.remove(focusableViewList.get(fragmentIndex));
        }

        //Restore previous focus
        if (previousFocusList.get(fragmentIndex) != null) {
            previousFocusList.get(fragmentIndex).requestFocus();
            //clear previous focus
            previousFocusList.remove(previousFocusList.get(fragmentIndex));
        }
    }

    public void onFragmentPause(Fragment fragment, View parentView) {

        //Disable focus and store previously focused
        if (fragment.getView() != null) {

            //Store last focused element
            previousFocusList.add(fragment.getView().findFocus());

            //add new focusableViews
            focusableViewList.add(new ArrayList<View>());

            //Clear current focus
            fragment.getView().clearFocus();

            int fragmentIndex = focusableViewList.size() - 1;

            //Disable focus
            setEnableView((ViewGroup) parentView, false, fragmentIndex);
        }
    }

    //finds and save focusable views using parent view
    private void findFocusableViews(ViewGroup viewGroup, int fragmentIndex) {

        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            if (view.isFocusable()) {
                if (!focusableViewList.get(fragmentIndex).contains(view)) {
                    focusableViewList.get(fragmentIndex).add(view);
                }
            }
            if (view instanceof ViewGroup) {
                findFocusableViews((ViewGroup) view, fragmentIndex);
            }
        }
    }

    //set view enabled/focusable properties
    private void setEnableView(ViewGroup viewGroup, boolean isEnabled, int fragmentIndex) {

        //Find focusable elements
        findFocusableViews(viewGroup, fragmentIndex);

        for (View view : focusableViewList.get(fragmentIndex)) {
            view.setEnabled(isEnabled);
            view.setFocusable(isEnabled);
        }
    }
}
