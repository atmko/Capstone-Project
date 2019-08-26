package com.atmko.onmywatch.adapters;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.atmko.onmywatch.Fragments.CastFragment;
import com.atmko.onmywatch.Fragments.ReviewsFragment;
import com.atmko.onmywatch.Fragments.SeasonsFragment;
import com.atmko.onmywatch.models.CastData;
import com.atmko.onmywatch.models.Season;
import com.atmko.onmywatch.models.SeriesData;

import org.parceler.Parcels;

import java.util.ArrayList;

public class DetailSeriesExtrasAdapter extends FragmentStatePagerAdapter {
    private static final int TAB_COUNT = 3;

    private SeriesData mSeriesData;

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
                ArrayList<CastData> castList = mSeriesData.getCast();
                Parcelable parceledCastList = Parcels.wrap(castList);
                return CastFragment.newInstance(parceledCastList);

            case 1:
                ArrayList<Season> seasons = mSeriesData.getSeasons();
                Parcelable parceledSeasons = Parcels.wrap(seasons);
                return SeasonsFragment.newInstance(parceledSeasons);

            case 2:
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
