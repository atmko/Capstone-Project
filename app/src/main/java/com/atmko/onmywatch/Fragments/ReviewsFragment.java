package com.atmko.onmywatch.Fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.network_utils.NetworkFunctions;

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
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //todo get reviews
    }
}