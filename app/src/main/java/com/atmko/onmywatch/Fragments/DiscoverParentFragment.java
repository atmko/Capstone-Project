/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.google.android.material.tabs.TabLayout;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.DiscoverManualPagerAdapter;
import com.atmko.onmywatch.adapters.DiscoverPresetPagerAdapter;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class DiscoverParentFragment extends Fragment
        implements SuperEditText.OnKeyBoardDismissListener {
    public static final String FRAGMENT_KEY = "search_parent_fragment";

    public static final String SEARCH_MODE_PRESET = "preset_search";
    public static final String SEARCH_MODE_MANUAL = "manual_search";

    private static final String SEARCH_MODE_KEY = "search_mode";
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String FRAGMENT_PAGER_ADAPTER_KEY = "fragment_pager_adapter";

    private String mSearchMode;
    private int mMediaType;
    private FragmentStatePagerAdapter mResultsAdapter;

    private Bundle mSavedInstanceState;
    private SuperEditText mSearchEditTextView;
    private TabLayout mMediaTypeTabLayout;
    private FrameLayout mSearchPresetsTopLayout;
    private TabLayout mSearchPresetsTabLayout;
    private ImageButton mSearchImageButton;
    private ViewPager mSearchResultsViewPager;

    private int mCurrentTabPosition;

    public DiscoverParentFragment() {
        // Required empty public constructor
    }

    public static DiscoverParentFragment newInstance() {
        DiscoverParentFragment fragment = new DiscoverParentFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    int getCurrentTabPosition() {
        return mCurrentTabPosition;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_discover_parent, container, false);
    }

    //TODO: NullPointerException handled
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        try {
            Toolbar toolbar = getView().findViewById(R.id.toolbar);

            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            //save savedInstanceState value for onCreateAnimator to check if this is the first instance
            mSavedInstanceState = savedInstanceState;

            defineViews();

            if (savedInstanceState == null) {
                //run code after entry animation is complete
                mSearchMode = SEARCH_MODE_PRESET;
                //TODO replace MEDIA_TYPE_SERIES with default media shared preference
                mMediaType = MEDIA_TYPE_SERIES;

                //startup code moved to onCreateAnimator

            } else {
                mSearchMode = savedInstanceState.getString(SEARCH_MODE_KEY);
                mMediaType = savedInstanceState.getInt(MEDIA_TYPE_KEY);

                if (mSearchMode.equals(SEARCH_MODE_PRESET)) {
                    loadPresetSearchUi();

                } else if (mSearchMode.equals(SEARCH_MODE_MANUAL)){
                    loadManualSearchUi();
                }
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    //TODO: NullPointerException handled
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResume() {
        super.onResume();

        try {
            ((MasterActivity) getActivity()).onResumeMasterContainerFragment(this);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // TODO: NullPointerException handled
    @SuppressWarnings("ConstantConditions")@Nullable
    @Override
    public Animator onCreateAnimator(int transit, final boolean enter, int nextAnim) {
        //if this is entry animation
        //&& if this is the first instance
        if (enter && mSavedInstanceState == null) {
            Animator animator = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    //load default ui
                    loadPresetSearchUi();

                    try {
                        //reserve focus by hiding background fragment
                        ((MasterActivity) getActivity())
                                .hideBackgroundFragment(DiscoverParentFragment.this);

                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });

            return animator;

        } else {
            //return super method
            return super.onCreateAnimator(transit, enter, nextAnim);
        }
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void defineViews() throws NullPointerException {
        final TextView titleText = getView().findViewById(R.id.title_text_view);
        titleText.setText(getString(R.string.discover_text_literal));

        final SuperEditText searchEditText = getView().findViewById(R.id.search_edit_text_view);
        mSearchImageButton = getView().findViewById(R.id.search_image_button);
        mSearchImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MasterActivity) getActivity()).onSearchButtonPressed(mSearchImageButton,
                        searchEditText, titleText);

                if (mSearchMode.equals(SEARCH_MODE_PRESET)) {

                } else if (mSearchMode.equals(SEARCH_MODE_MANUAL)) {
                    loadPresetSearchUi();
                }
            }
        });

        mMediaTypeTabLayout = getView().findViewById(R.id.tab_layout_1);
        mSearchPresetsTopLayout = getView().findViewById(R.id.tab_degree_2_frame_layout);
        mSearchPresetsTabLayout = getView().findViewById(R.id.tab_layout_2);
        mSearchResultsViewPager = getView().findViewById(R.id.discover_results_view_pager);

        try {
            configureSearchBox();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void configureSearchBox() throws NullPointerException {
        mSearchEditTextView = getView().findViewById(R.id.search_edit_text_view);
        mSearchEditTextView.setKeyBoardDismissListener(this);
        //configure search box action event
        mSearchEditTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                //validate user input
                if (mSearchEditTextView.getText().toString().equals("")) {
                    return true;
                }

                ((MasterActivity) getActivity()).hideSoftKeyboard(mSearchEditTextView);

                //set focus to top layout(away from search box)
                getView().findViewById(R.id.top_layout).requestFocus();


                //execute search
                loadManualSearchUi();

                return true;
            }
        });
    }

    private void loadPresetSearchUi() {
        mSearchPresetsTopLayout.setVisibility(View.VISIBLE);

        mSearchMode = SEARCH_MODE_PRESET;

        try {
            configurePresetMediaTypeTabLayout();
            loadPresetTabAndPager();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void configurePresetMediaTypeTabLayout() throws NullPointerException {
        //remove old tabs
        mMediaTypeTabLayout.removeAllTabs();

        //add new tabs
        String[] searchTypesList =
                getContext().getResources().getStringArray(R.array.discover_preset_media_types);

        for (String type : searchTypesList) {
            mMediaTypeTabLayout.addTab(mMediaTypeTabLayout.newTab().setText(type));
        }

        mMediaTypeTabLayout.getTabAt(mMediaType).select();

        //clear old listeners to avoid conflicts
        mMediaTypeTabLayout.clearOnTabSelectedListeners();

        //configure new listener
        mMediaTypeTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mMediaType = tab.getPosition();
                loadPresetTabAndPager();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void loadPresetTabAndPager() throws NullPointerException {
        //remove old tabs
        mSearchPresetsTabLayout.removeAllTabs();

        //add new tabs
        String[] titleList = null;
        //configure urls
        String[] urlList = null;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            titleList = getContext().getResources().getStringArray(R.array.preset_movie_discover_titles);
            urlList = getContext().getResources().getStringArray(R.array.preset_movie_discover_urls);

        } else if (mMediaType == MEDIA_TYPE_SERIES){
            titleList = getContext().getResources().getStringArray(R.array.preset_series_discover_titles);
            urlList = getContext().getResources().getStringArray(R.array.preset_series_discover_urls);
        }

        for (String title : titleList) {
            mSearchPresetsTabLayout.addTab(mSearchPresetsTabLayout.newTab().setText(title));
        }

        mSearchResultsViewPager.setOffscreenPageLimit(titleList.length - 1);

        //configure search preferences
        SearchPreferences searchPreferences = new SearchPreferences();

        mResultsAdapter = new DiscoverPresetPagerAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, mMediaType, urlList,
                searchPreferences);

        mSearchResultsViewPager.setAdapter(mResultsAdapter);

        //clear old listeners to avoid conflicts
        mSearchResultsViewPager.clearOnPageChangeListeners();

        //configure new listeners
        mSearchResultsViewPager.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(mSearchPresetsTabLayout));
        mSearchPresetsTabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mSearchResultsViewPager.setCurrentItem(tab.getPosition());
                mCurrentTabPosition = tab.getPosition();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void loadManualSearchUi() {
        //hide presets tab
        mSearchPresetsTopLayout.setVisibility(View.GONE);

        mSearchMode = SEARCH_MODE_MANUAL;

        try {
            configureManualMediaTypeTabLayout();
            loadManualPager(mSearchEditTextView.getText().toString());

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void configureManualMediaTypeTabLayout() {
        //remove old tabs
        mMediaTypeTabLayout.removeAllTabs();

        //add new tabs
        String[] searchTypesList =
                getContext().getResources().getStringArray(R.array.discover_manual_media_types);

        for (String type : searchTypesList) {
            mMediaTypeTabLayout.addTab(mMediaTypeTabLayout.newTab().setText(type));
        }

        //clear old listeners to avoid conflicts
        mMediaTypeTabLayout.clearOnTabSelectedListeners();

        //configure new listener
        mMediaTypeTabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mMediaType = tab.getPosition();
                mSearchResultsViewPager.setCurrentItem(tab.getPosition());
                mCurrentTabPosition = tab.getPosition();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void loadManualPager(String query) throws NullPointerException {
        //clear old listeners to avoid conflicts
        mSearchResultsViewPager.clearOnPageChangeListeners();

        //configure new listener
        mSearchResultsViewPager.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(mMediaTypeTabLayout));

        //configure view pager adapter

        //configure urls
        String[] urlList = getContext().getResources().getStringArray(R.array.manual_discover_urls);

        //configure search preferences
        SearchPreferences searchPreferences = new SearchPreferences();
        searchPreferences.setQuery(query);

        mResultsAdapter = new DiscoverManualPagerAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, urlList,
                searchPreferences);

        mSearchResultsViewPager.setAdapter(mResultsAdapter);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SEARCH_MODE_KEY, mSearchMode);
        outState.putInt(MEDIA_TYPE_KEY, mMediaType);
        outState.putParcelable(FRAGMENT_PAGER_ADAPTER_KEY, mResultsAdapter.saveState());
    }

    // TODO: NullPointerException handled
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onKeyBoardDismiss() {
        try {
            //set focus to top layout when keyboard dismissed
            getView().findViewById(R.id.top_layout).requestFocus();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
