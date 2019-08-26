package com.atmko.onmywatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.os.Bundle;

import com.atmko.onmywatch.Fragments.AddToListFragment;
import com.atmko.onmywatch.Fragments.CreateListFragment;
import com.atmko.onmywatch.Fragments.HomeFragment;
import com.atmko.onmywatch.Fragments.ListResultsParentFragment;
import com.atmko.onmywatch.utils.UpdateMediaWorker;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MasterActivity extends AppCompatActivity implements
        AddToListFragment.OnSavePressedActionListener,
        CreateListFragment.OnSavePressedActionListener {

    private static final String DEFAULT_MEDIA_KEY = "default_media";

    private static final String ADD_TO_LIST_VISIBILITY_KEY = "add_to_list_visibility";

    public static final int MEDIA_TYPE_SERIES = 0;
    public static final int MEDIA_TYPE_MOVIE = 1;
    public static final int MEDIA_TYPE_PEOPLE = 2;

    private boolean mIsTabletLandscape;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        mIsTabletLandscape = getResources().getBoolean(R.bool.isTabletLandscape);

        // Obtain Analytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        if (savedInstanceState == null) {
            startHomeFragment();
            //start background work managers
            startWorkers();

        } else {
            int addToListVisibility = savedInstanceState.getInt(ADD_TO_LIST_VISIBILITY_KEY);
            findViewById(R.id.popup_container).setVisibility(addToListVisibility);
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(ADD_TO_LIST_VISIBILITY_KEY,
                findViewById(R.id.popup_container).getVisibility());
    }

    @Override
    public void onBackPressed() {
        Fragment popupFragment = getSupportFragmentManager().findFragmentById(R.id.popup_container);
        Fragment detailFragment = getSupportFragmentManager().findFragmentById(R.id.detail_fragments_container);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.master_fragments_container);

        //condition for navigation
        if (hasFragment(R.id.popup_container)) {
            getSupportFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .remove(popupFragment).commit();


        //condition for navigation
        //this removes details fragment because master container is behind detail container
        // (via frame layout) in non tablet landscape
        } else if (hasFragment(R.id.detail_fragments_container) && !mIsTabletLandscape) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                    .remove(detailFragment).commit();


        }else {
            //condition for exit animation transition
            if (fragment instanceof ListResultsParentFragment) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                        .remove(fragment).commit();


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
    }

    public boolean isTabletLandscape(){
        return mIsTabletLandscape;
    }

    public boolean hasFragment(int containerId) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(containerId);

        return fragment != null;
    }

    @Override
    public void onSavePressed() {
        onBackPressed();
    }
}
