package com.atmko.onmywatch.Fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.ListWatchAndUserAdapter;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;


public class ListsParentFragment extends Fragment {
    public static String FRAGMENT_KEY = "lists_parent_fragment";
    public static final int LIST_TYPE_WATCH = 0;
    public static final int LIST_TYPE_USER = 1;

    private Bundle mSavedInstanceState;
    private ViewPager mViewPager;
    private ListWatchAndUserAdapter mListWatchAndUserAdapter;

    public ListsParentFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static ListsParentFragment newInstance() {
        ListsParentFragment fragment = new ListsParentFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_lists_parent, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //save saveInstanceState value for onCreateAnimator to check if this is the first instance
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
        mViewPager = getView().findViewById(R.id.lists_view_pager);
        mListWatchAndUserAdapter = new ListWatchAndUserAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mViewPager.setAdapter(mListWatchAndUserAdapter);
    }
}
