package com.atmko.onmywatch.Fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.ListResultsUserPagerAdapter;

public class ListResultsParentFragment extends Fragment {
    public static String FRAGMENT_KEY = "list_results_parent_fragment";

    // the fragment initialization parameters
    private static final String LIST_TYPE_KEY = "list_type";
    private static final String LIST_NAME_KEY = "list_name";

    private int mListType;
    private String mListName;

    private Bundle mSavedInstanceState;

    public ListResultsParentFragment() {
        // Required empty public constructor
    }

    public static ListResultsParentFragment newInstance(int listType, String listName) {
        ListResultsParentFragment fragment = new ListResultsParentFragment();
        Bundle args = new Bundle();
        args.putInt(LIST_TYPE_KEY, listType);
        args.putString(LIST_NAME_KEY, listName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mListType = getArguments().getInt(LIST_TYPE_KEY);
            mListName = getArguments().getString(LIST_NAME_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_results_parent, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //save save instanceState value for onCreateAnimator to check if this is the first instance
        mSavedInstanceState = savedInstanceState;

        if (savedInstanceState == null) {
            //startup code moved to onCreateAnimator

        } else {
            defineViews();

        }
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
        TabLayout mMediaTypeTabLayout = getView().findViewById(R.id.media_type_tab_layout);
        final ViewPager mListsViewPager = getView().findViewById(R.id.lists_view_pager);

        //remove old tabs
        mMediaTypeTabLayout.removeAllTabs();

        //add new tabs
        String[] listMediaTypes = getContext().getResources().getStringArray(R.array.list_media_types);

        for (String type : listMediaTypes) {
            mMediaTypeTabLayout.addTab(mMediaTypeTabLayout.newTab().setText(type));
        }

        mListsViewPager.setOffscreenPageLimit(listMediaTypes.length - 1);

        FragmentStatePagerAdapter resultsAdapter = new ListResultsUserPagerAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,getContext(),
                mListType, mListName);

        mListsViewPager.setAdapter(resultsAdapter);

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
}
