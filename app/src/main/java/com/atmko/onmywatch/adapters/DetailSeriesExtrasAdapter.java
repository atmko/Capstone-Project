/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.atmko.onmywatch.fragments.CastFragment;
import com.atmko.onmywatch.fragments.RecommendationsFragment;
import com.atmko.onmywatch.fragments.ReviewsFragment;
import com.atmko.onmywatch.models.SeriesData;

import org.parceler.Parcels;

import java.util.ArrayList;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

/*
 * pager adapter hosting DetailFragment's child fragments for the series media type
 */

public class DetailSeriesExtrasAdapter extends FragmentStatePagerAdapter {
    private static final int TAB_COUNT = 3;

    private final SeriesData mSeriesData;

    public DetailSeriesExtrasAdapter(@NonNull FragmentManager fm, int behavior, SeriesData seriesData) {
        super(fm, behavior);

        this.mSeriesData = seriesData;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Fragment fragment = new Fragment();
        switch (position) {
            case 0:
                return RecommendationsFragment.newInstance(MEDIA_TYPE_SERIES, mSeriesData.getId());

            case 1:
                return CastFragment.newInstance(MEDIA_TYPE_SERIES, mSeriesData.getId());

            case 2:
                ArrayList reviewList = mSeriesData.getReviews();
                Parcelable parceledReviewList = Parcels.wrap(reviewList);
                return ReviewsFragment.newInstance(parceledReviewList);

            default:
                return fragment;
        }
    }

    @Override
    public int getCount() {
        return TAB_COUNT;
    }
}
