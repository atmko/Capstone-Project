/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.atmko.onmywatch.Fragments.CastFragment;
import com.atmko.onmywatch.Fragments.RecommendationsFragment;
import com.atmko.onmywatch.Fragments.ReviewsFragment;
import com.atmko.onmywatch.models.CastData;
import com.atmko.onmywatch.models.MovieData;

import org.parceler.Parcels;

import java.util.ArrayList;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;

/*
 * pager adapter hosting DetailFragment's child fragments for the movie media type
 */

public class DetailMovieExtrasAdapter extends FragmentStatePagerAdapter {
    private static final int TAB_COUNT = 2;

    private final MovieData mMovieData;

    public DetailMovieExtrasAdapter(@NonNull FragmentManager fm, int behavior, MovieData movieData) {
        super(fm, behavior);

        this.mMovieData = movieData;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Fragment fragment = new Fragment();
        switch (position) {
            case 0:
                return RecommendationsFragment.newInstance(MEDIA_TYPE_MOVIE, mMovieData.getId());

            case 1:
                ArrayList<CastData> castList = mMovieData.getCast();
                Parcelable parceledCastList = Parcels.wrap(castList);
                return CastFragment.newInstance(parceledCastList);

            case 2:
                ArrayList reviewList = mMovieData.getReviews();
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
