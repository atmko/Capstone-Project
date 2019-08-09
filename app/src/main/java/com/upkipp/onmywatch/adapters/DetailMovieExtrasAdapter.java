package com.upkipp.onmywatch.adapters;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.upkipp.onmywatch.Fragments.CastFragment;
import com.upkipp.onmywatch.Fragments.ReviewsFragment;
import com.upkipp.onmywatch.models.CastData;
import com.upkipp.onmywatch.models.MovieData;

import org.parceler.Parcels;

import java.util.ArrayList;

public class DetailMovieExtrasAdapter extends FragmentStatePagerAdapter {
    private static int TAB_COUNT = 2;

    private MovieData mMovieData;

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
                ArrayList<CastData> castList = mMovieData.getCast();
                Parcelable parceledCastList = Parcels.wrap(castList);
                return CastFragment.newInstance(parceledCastList);

            case 1:
                return ReviewsFragment.newInstance(null);

            default:
                return fragment;
        }
    }

    @Override
    public int getCount() {
        return TAB_COUNT;
    }
}
