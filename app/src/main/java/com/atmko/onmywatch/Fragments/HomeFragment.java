/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.atmko.onmywatch.BillingActivity;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.SettingsActivity;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;
import com.atmko.onmywatch.view_models.MasterActivityViewModel;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class HomeFragment extends Fragment {
    public static final String FRAGMENT_KEY = "home_fragment";

    private static final String MEDIA_TYPE_KEY = "media_type";

    private int mMediaType;

    public HomeFragment() {
        // Required empty public constructor
    }

    //TODO @param defaultMedia doesn't need to be passed and can be retrieved within the fragment
    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();
        observeViewModel();

        //TODO replace MEDIA_TYPE_SERIES with default media shared preference
        if (savedInstanceState == null) {
            mMediaType = MEDIA_TYPE_SERIES;
            loadHomeFragment();

            //TODO replace MEDIA_TYPE_SERIES with default media shared preference
        } else {
            mMediaType = savedInstanceState.getInt(MEDIA_TYPE_KEY, MEDIA_TYPE_SERIES);
            loadMediaLabel();
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        ((MasterActivity) getActivity()).onResumeMasterContainerFragment(this);
    }

    private void defineViews() {
        setHasOptionsMenu(true);

        Toolbar toolbar = getView().findViewById(R.id.toolbar);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        TextView mediaTypeTextView = getView().findViewById(R.id.media_type_text_view);
        mediaTypeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaType == (MEDIA_TYPE_SERIES)) {
                    mMediaType = MEDIA_TYPE_MOVIE;

                } else if (mMediaType == (MEDIA_TYPE_MOVIE)) {
                    mMediaType = MEDIA_TYPE_SERIES;
                }

                loadHomeFragment();
            }
        });

        TextView listsTextView = getView().findViewById(R.id.lists_text_view);
        listsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListsWatchAndUserParentFragment listsParentFragment =
                        ListsWatchAndUserParentFragment.newInstance();

                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_down_entry, android.R.animator.fade_out)
                        .add(R.id.master_fragments_container, listsParentFragment,
                                ListsWatchAndUserParentFragment.FRAGMENT_KEY)
                        .commit();
            }
        });

        final ImageButton searchImageButton = getView().findViewById(R.id.search_image_button);
        searchImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DiscoverParentFragment discoverParentFragment = DiscoverParentFragment.newInstance();

                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_down_entry, android.R.animator.fade_out)
                        .add(R.id.master_fragments_container, discoverParentFragment,
                                DiscoverParentFragment.FRAGMENT_KEY)
                        .commit();
            }
        });
    }

    private void observeViewModel() {
        if (getActivity() == null) return;

        MasterActivityViewModel masterActivityViewModel =
                ViewModelProviders.of(this).get(MasterActivityViewModel.class);

        masterActivityViewModel.getIsProModeLiveData().observe(getActivity(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isProMode) {
                configureAds();
            }
        });
    }

    private void configureAds() {
        @SuppressWarnings("ConstantConditions")
        AdView mAdView = getView().findViewById(R.id.banner_ad);

        if (MasterActivity.sIsProMode) {
            mAdView.setVisibility(View.GONE);
            mAdView.destroy();

        } else {
            mAdView.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build();
            mAdView.loadAd(adRequest);
        }
    }

    private void loadMediaLabel() {
        final TextView mediaTypeTextView = getView().findViewById(R.id.media_type_text_view);

        if (mMediaType == MEDIA_TYPE_SERIES) {
            mediaTypeTextView.setText(getString(R.string.media_type_series_text));
            ((TextView) getView().findViewById(R.id.upcoming_container_heading)).setText(getString(R.string.upcoming_episodes));
            ((TextView) getView().findViewById(R.id.undated_container_heading)).setText(getString(R.string.no_release_date));
            ((TextView) getView().findViewById(R.id.ended_container_heading)).setText(getString(R.string.fully_released));

        } else if (mMediaType == MEDIA_TYPE_MOVIE) {
            mediaTypeTextView.setText(getString(R.string.media_type_movies_text));
            ((TextView) getView().findViewById(R.id.upcoming_container_heading)).setText(getString(R.string.upcoming_movies));
            ((TextView) getView().findViewById(R.id.undated_container_heading)).setText(getString(R.string.no_release_date));
            ((TextView) getView().findViewById(R.id.ended_container_heading)).setText(getString(R.string.already_released));
        }
    }

    private void loadHomeFragment() {
        loadMediaLabel();

        SearchPreferences searchPreferences =  new SearchPreferences();

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            String spotlightUrl =
                    getContext().getResources().getStringArray(R.array.spotlight_url)[MEDIA_TYPE_MOVIE];

            HomeSpotlightFragment spotLightDisplay = HomeSpotlightFragment
                    .newInstance(mMediaType, spotlightUrl, searchPreferences);

            HomeListDisplayFragment upcomingMoviesHomeList = HomeListDisplayFragment
                    .newInstance(HomeListDisplayFragment.UPCOMING_MOVIES, MEDIA_TYPE_MOVIE);

            HomeListDisplayFragment releasedMoviesHomeList = HomeListDisplayFragment
                    .newInstance(HomeListDisplayFragment.ALREADY_RELEASED_MOVIES, MEDIA_TYPE_MOVIE);

            HomeListDisplayFragment undatedMoviesHomeList = HomeListDisplayFragment
                    .newInstance(HomeListDisplayFragment.UNDATED_MOVIES, MEDIA_TYPE_MOVIE);

            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.spotlight_container, spotLightDisplay,
                            HomeSpotlightFragment.FRAGMENT_KEY)
                    .replace(R.id.upcoming_media_container, upcomingMoviesHomeList,
                            HomeListDisplayFragment.FRAGMENT_KEY)
                    .replace(R.id.ended_media_container, releasedMoviesHomeList,
                            HomeListDisplayFragment.FRAGMENT_KEY)
                    .replace(R.id.undated_media_container, undatedMoviesHomeList,
                            HomeListDisplayFragment.FRAGMENT_KEY)
                    .commit();

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            String spotlightUrl =
                    getContext().getResources().getStringArray(R.array.spotlight_url)[MEDIA_TYPE_SERIES];

            HomeSpotlightFragment spotLightDisplay = HomeSpotlightFragment
                    .newInstance(mMediaType, spotlightUrl, searchPreferences);

            HomeListDisplayFragment upcomingEpisodesHomeList = HomeListDisplayFragment
                    .newInstance(HomeListDisplayFragment.UPCOMING_EPISODES, MEDIA_TYPE_SERIES);

            HomeListDisplayFragment endedSeriesHomeList = HomeListDisplayFragment
                    .newInstance(HomeListDisplayFragment.ENDED_SERIES, MEDIA_TYPE_SERIES);

            HomeListDisplayFragment undatedSeriesHomeList = HomeListDisplayFragment
                    .newInstance(HomeListDisplayFragment.UNDATED_SERIES, MEDIA_TYPE_SERIES);

            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.spotlight_container, spotLightDisplay,
                            HomeSpotlightFragment.FRAGMENT_KEY)
                    .replace(R.id.upcoming_media_container, upcomingEpisodesHomeList,
                            HomeListDisplayFragment.FRAGMENT_KEY)
                    .replace(R.id.ended_media_container, endedSeriesHomeList,
                            HomeListDisplayFragment.FRAGMENT_KEY)
                    .replace(R.id.undated_media_container, undatedSeriesHomeList,
                            HomeListDisplayFragment.FRAGMENT_KEY)
                    .commit();
        }
    }

    //configures width, height and margins of home list display container
    private void configureListContainerParams(int containerId) {
        final FrameLayout fragmentContainer = getView().findViewById(containerId);

        DisplayMetrics displayDimensions = Resources.getSystem().getDisplayMetrics();

        int masterRatio;
        int detailRatio;

        int imageColumnSpan = getResources().getInteger(R.integer.search_column_span);

        //get layout weights
        masterRatio = getResources().getInteger(R.integer.master_fragment_layout_weight);
        detailRatio = getResources().getInteger(R.integer.detail_fragment_layout_weight);

        //get weight total
        int weightTotal = masterRatio + detailRatio;

        int weightedWidth;

        //get total weightedWidth
        if (((MasterActivity) getActivity()).isTabletLandscape()) {
            weightedWidth = displayDimensions.widthPixels * masterRatio/weightTotal;

        } else {
            weightedWidth = displayDimensions.widthPixels;

        }

        //get single image pixel width: (searchFragmentPixelWidth/num of columns)
        int singleImgPixelWidth =
                weightedWidth / imageColumnSpan;

        //convert spacing between images to pixels
        int imageSpacing = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                getResources().getInteger(R.integer.search_image_spacing),
                getResources().getDisplayMetrics());

        //new image width now that spacing is applied
        int adjustedViewWidth = singleImgPixelWidth - imageSpacing;

        //get poster height
        Long posterHeight = Math.round(adjustedViewWidth * ApiConstants.POSTER_ASPECT_RATIO);

        //set layout params
        LinearLayout.LayoutParams parentDictatingParams =
                new LinearLayout.LayoutParams(weightedWidth, posterHeight.intValue());

        fragmentContainer.setLayoutParams(parentDictatingParams);

        //set layout margins
        Float dimenToPixels = displayDimensions.density *
                getResources().getInteger(R.integer.x1_standard_layout_margin_unscaled);
        ViewGroup.MarginLayoutParams margins =
                (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();

        margins.setMargins(0, dimenToPixels.intValue(), 0, 0);
    }


    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        super.onCreateOptionsMenu(menu, inflater);

        //get menu item
        MenuItem settingsItem = menu.findItem(R.id.settings);
        MenuItem purchasesItem = menu.findItem(R.id.purchases);
        MenuItem logOutItem = menu.findItem(R.id.log_out);

        //set click listener
        settingsItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //launch settings activity
                Intent settingsIntent = new Intent(getContext(), SettingsActivity.class);
                startActivity(settingsIntent);

                return true;
            }
        });

        //set click listener
        purchasesItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //launch settings activity
                Intent billingIntent = new Intent(getActivity(), BillingActivity.class);
                startActivity(billingIntent);

                return true;
            }
        });

        //set click listener
        logOutItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //launch settings activity
                if (getActivity() != null) {
                    MasterActivity.startLogOutBackupService(getActivity());
                }

                return true;
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(MEDIA_TYPE_KEY, mMediaType);
    }
}
