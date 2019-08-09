package com.upkipp.onmywatch.Fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.models.SeriesData;
import com.upkipp.onmywatch.utils.network_utils.NetworkFunctions;

import org.parceler.Parcels;

public class ReviewsFragment extends Fragment {
    public static String FRAGMENT_KEY = "reviews_fragment";

    public static String REVIEWS_PARCELABLE_KEY = "reviews_parcelable";

    private SeriesData mReviews;

    public ReviewsFragment() {
        // Required empty public constructor
    }

    public static ReviewsFragment newInstance(Parcelable reviewsParcel) {
        ReviewsFragment fragment = new ReviewsFragment();
        Bundle args = new Bundle();
        args.putParcelable(REVIEWS_PARCELABLE_KEY, reviewsParcel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mReviews = Parcels.unwrap(getArguments().getParcelable(REVIEWS_PARCELABLE_KEY));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_reviews, container, false);

//        final TabLayout tabLayout = rootView.findViewById(R.id.detail_extras_tab_layout);
////        tabLayout.addTab(tabLayout.newTab().setText(CAST_TAB_TITLE));
////        tabLayout.addTab(tabLayout.newTab().setText(SEASONS_TAB_TITLE));
////        tabLayout.addTab(tabLayout.newTab().setText(REVIEWS_TAB_TITLE));
//        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
//
//        final ViewPager viewPager = rootView.findViewById(R.id.details_extra_view_pager);
//        PagerAdapter pagerAdapter = new DetailExtrasAdapter(getFragmentManager(),
//                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
//        viewPager.setAdapter(pagerAdapter);
//        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
//
//        tabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
//            @Override
//            public void onTabSelected(TabLayout.Tab tab) {
//                viewPager.setCurrentItem(tab.getPosition());
//            }
//
//            @Override
//            public void onTabUnselected(TabLayout.Tab tab) {
//
//            }
//
//            @Override
//            public void onTabReselected(TabLayout.Tab tab) {
//
//            }
//        });

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        defineViews();
//        setViewValues();

    }

    private void defineViews() {

    }

    private void setViewValues() {
        NetworkFunctions.loadImage(getContext(), mReviews.getBackdropPath(),
                ((ImageView) getView().findViewById(R.id.backdrop_image_view)));

//        ((TextView) getView().findViewById(R.id.title);
    }

}