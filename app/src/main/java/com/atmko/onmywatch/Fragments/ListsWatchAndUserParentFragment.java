package com.atmko.onmywatch.Fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.ListWatchAndUserAdapter;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.google.android.material.tabs.TabLayout;


public class ListsWatchAndUserParentFragment extends Fragment
        implements SuperEditText.OnKeyBoardDismissListener {
    public static String FRAGMENT_KEY = "lists_watch_and_user_parent_fragment";
    public static final int LIST_TYPE_WATCH = 0;
    public static final int LIST_TYPE_USER = 1;

    private Bundle mSavedInstanceState;

    public ListsWatchAndUserParentFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static ListsWatchAndUserParentFragment newInstance() {
        ListsWatchAndUserParentFragment fragment = new ListsWatchAndUserParentFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_lists_watch_and_user_parent, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        Toolbar toolbar = getView().findViewById(R.id.toolbar);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //save saveInstanceState value for onCreateAnimator to check if this is the first instance
        mSavedInstanceState = savedInstanceState;

        if (savedInstanceState == null) {
            //startup code moved to onCreateAnimator

        } else {
            defineViews();

        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ((MasterActivity) getActivity()).onResumeMasterContainerFragment(this);

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
                    ((MasterActivity) getActivity())
                            .hideBackgroundFragment(ListsWatchAndUserParentFragment.this);

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
        final TextView titleText = getView().findViewById(R.id.title_text_view);
        titleText.setText(getString(R.string.lists_text_literal));

        final SuperEditText searchEditText = getView().findViewById(R.id.search_edit_text_view);
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                ((MasterActivity) getActivity()).hideSoftKeyboard(searchEditText);

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
                ((MasterActivity) getActivity()).onSearchButtonPressed(searchImageButton,
                        searchEditText, titleText);
            }
        });


        TabLayout mMediaTypeTabLayout = getView().findViewById(R.id.list_type_tab_layout);
        final ViewPager mListsViewPager = getView().findViewById(R.id.lists_view_pager);

        //remove old tabs
        mMediaTypeTabLayout.removeAllTabs();

        //add new tabs
        String[] listTypeNames = getContext().getResources().getStringArray(R.array.list_type_titles);

        for (String type : listTypeNames) {
            mMediaTypeTabLayout.addTab(mMediaTypeTabLayout.newTab().setText(type));
        }

        mListsViewPager.setOffscreenPageLimit(listTypeNames.length - 1);

        ListWatchAndUserAdapter mListWatchAndUserAdapter = new ListWatchAndUserAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        mListsViewPager.setAdapter(mListWatchAndUserAdapter);

        //configure listeners
        mListsViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mMediaTypeTabLayout));
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
    }

    @Override
    public void onKeyBoardDismiss() {
        //set focus to top layout when keyboard dismissed
        getView().findViewById(R.id.top_layout).requestFocus();

    }
}
