/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.ReviewActivity;
import com.atmko.onmywatch.adapters.ReviewDataAdapter;
import com.atmko.onmywatch.models.Review;

import org.parceler.Parcels;

import java.util.ArrayList;

public class ReviewsFragment extends Fragment implements ReviewDataAdapter.OnListItemClickListener {
    public static String FRAGMENT_KEY = "reviews_fragment";

    private static final String REVIEWS_PARCELABLE_KEY = "reviews_parcelable";

    private ArrayList<Review> mReviewList;
    private ReviewDataAdapter mAdapter;

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
            mReviewList = Parcels.unwrap(getArguments().getParcelable(REVIEWS_PARCELABLE_KEY));
            if (mReviewList == null) mReviewList = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reviews, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        loadSearch();
    }

    private void loadSearch() {
        RecyclerView recyclerView = getView().findViewById(R.id.reviews_recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());

        mAdapter = new ReviewDataAdapter(this);
        recyclerView.setAdapter(mAdapter);

        populateAndNotifyAdapter(mReviewList);
    }

    private LinearLayoutManager configureLayoutManager() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    //TODO implement reviews details page launcher
    @Override
    public void onItemClick(int position) {
        Review selectedReview = mAdapter.getReviewData(position);
        if (getActivity() != null) {
            Intent reviewActivityIntent =
                    new Intent(getActivity().getApplicationContext(), ReviewActivity.class);
            reviewActivityIntent.putExtra(ReviewActivity.REVIEW_KEY, Parcels.wrap(selectedReview));
            getActivity().startActivity(reviewActivityIntent);
        }
    }

    private void populateAndNotifyAdapter(ArrayList<Review> reviews) {
        if (getView() != null) {
            if (reviews.size() == 0) {
                mAdapter.setInPlaceholderMode(true);
                if (getView() != null) {
                    getView().findViewById(R.id.no_data_text_view).setVisibility(View.VISIBLE);
                }

            } else {
                getView().findViewById(R.id.no_data_text_view).setVisibility(View.GONE);
                mAdapter.setInPlaceholderMode(false);
                mAdapter.getAdapterData().clear();
                mAdapter.addAdapterData(reviews);
            }
        }
    }
}