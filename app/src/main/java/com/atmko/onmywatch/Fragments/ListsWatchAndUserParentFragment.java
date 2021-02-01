/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.ListWatchAndUserAdapter;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

public class ListsWatchAndUserParentFragment extends Fragment
        implements SuperEditText.OnKeyBoardDismissListener {
    private static final String TAG = ListsWatchAndUserParentFragment.class.getSimpleName();
    public static final String FRAGMENT_KEY = "lists_watch_and_user_parent_fragment";
    public static final int LIST_TYPE_WATCH = 0;
    public static final int LIST_TYPE_USER = 1;
    public static final int LIST_TYPE_AUTO = 2;

    private static final String LIST_TYPE_NAMES_KEY = "list_type_names";
    private static final String IS_UP_ENABLED_KEY = "is_up_enabled";

    private Bundle mSavedInstanceState;
    private static ListFragmentImplementation sFragmentImplementation;
    private String[] mListTypeNames;
    private boolean mIsUpEnabled;

    public ListsWatchAndUserParentFragment() {
        // Required empty public constructor
    }

    public static ListsWatchAndUserParentFragment newInstance(String[] listTypeNames,
                                                              boolean isUpEnabled) {
        ListsWatchAndUserParentFragment fragment = new ListsWatchAndUserParentFragment();
        Bundle args = new Bundle();
        args.putStringArray(LIST_TYPE_NAMES_KEY, listTypeNames);
        args.putBoolean(IS_UP_ENABLED_KEY, isUpEnabled);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof ListFragmentImplementation)) {
            Log.d(TAG, ListFragmentImplementation.class.getSimpleName() + " must be implemented");
        } else {
            sFragmentImplementation = ((ListFragmentImplementation) context);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mListTypeNames = getArguments().getStringArray(LIST_TYPE_NAMES_KEY);
            mIsUpEnabled = getArguments().getBoolean(IS_UP_ENABLED_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_lists_watch_and_user_parent, container, false);
    }

    public interface ListFragmentImplementation {
        void onListFragmentResume(Fragment fragment);
        void onAnimationEnd(Fragment fragment);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (getView() == null) return;
        if (getActivity() == null) return;

        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        Toolbar toolbar = getView().findViewById(R.id.toolbar);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(mIsUpEnabled);
        }

        //save saveInstanceState value for onCreateAnimator to check if this is the first instance
        mSavedInstanceState = savedInstanceState;

        //noinspection StatementWithEmptyBody
        if (savedInstanceState == null) {
            //startup code moved to onCreateAnimator

        } else {
            defineViews();

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sFragmentImplementation.onListFragmentResume(this);
    }

    @Nullable
    @Override
    public Animator onCreateAnimator(int transit, final boolean enter, int nextAnim) {
        //if this is entry animation
        //&& if this is the first instance
        if (enter && mSavedInstanceState == null) {
            //create entry animator
            Animator animator = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
            //add animator listener
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    //run code after entry animation is complete
                    defineViews();

                    //reserve focus by hiding background fragment
                    sFragmentImplementation
                            .onAnimationEnd(ListsWatchAndUserParentFragment.this);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });

            return animator;

        }

        //return super method
        return super.onCreateAnimator(transit, enter, nextAnim);
    }

    private void defineViews() {
        if (getView() == null) return;

        final TextView titleText = getView().findViewById(R.id.title_text_view);
        titleText.setText(getString(R.string.lists_text_literal));

        final SuperEditText searchEditText = getView().findViewById(R.id.search_edit_text_view);
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (getView() == null) return true;

                MasterActivity.hideSoftKeyboard(searchEditText);

                //set focus to top layout(away from search box)
                getView().findViewById(R.id.top_layout).requestFocus();

                return true;
            }
        });
        searchEditText.setKeyBoardDismissListener(this);
        final ImageButton searchImageButton = getView().findViewById(R.id.search_image_button);
        searchImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MasterActivity.onSearchButtonPressed(searchImageButton, searchEditText, titleText);
            }
        });

        TabLayout mMediaTypeTabLayout = getView().findViewById(R.id.tab_layout_1);
        final ViewPager mListsViewPager = getView().findViewById(R.id.lists_view_pager);

        //remove old tabs
        mMediaTypeTabLayout.removeAllTabs();

        //add new tabs
        for (String type : mListTypeNames) {
            mMediaTypeTabLayout.addTab(mMediaTypeTabLayout.newTab().setText(type));
        }

        mListsViewPager.setOffscreenPageLimit(mListTypeNames.length - 1);

        ListWatchAndUserAdapter mListWatchAndUserAdapter =
                new ListWatchAndUserAdapter(getChildFragmentManager(),
                        FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        mListWatchAndUserAdapter
                .setLogicImplementation((ListWatchAndUserAdapter.LogicImplementation) getActivity());
        mListWatchAndUserAdapter.setTabCount(mListTypeNames.length);

        mListsViewPager.setAdapter(mListWatchAndUserAdapter);

        //configure listeners
        mListsViewPager.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(mMediaTypeTabLayout));
        mMediaTypeTabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mListsViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        FloatingActionButton mFab = requireView().findViewById(R.id.new_list_fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MasterActivity.launchCreateListActivity(getActivity());
            }
        });
    }

    void setFabViewState(boolean showFab) {
        FloatingActionButton mFab = requireView().findViewById(R.id.new_list_fab);
        if (showFab) {
            mFab.show();
        } else {
            mFab.hide();
        }
    }

    @Override
    public void onKeyBoardDismiss() {
        if (getView() == null) return;
        //set focus to top layout when keyboard dismissed
        getView().findViewById(R.id.top_layout).requestFocus();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (getView() != null) {
            final ViewPager mListsViewPager = getView().findViewById(R.id.lists_view_pager);
            ListWatchAndUserAdapter listWatchAndUserAdapter =
                    ((ListWatchAndUserAdapter) mListsViewPager.getAdapter());
            if (listWatchAndUserAdapter != null) {
                Fragment fragment = listWatchAndUserAdapter.getItem(mListsViewPager.getCurrentItem());
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}
