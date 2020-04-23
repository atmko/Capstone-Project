/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.atmko.onmywatch.fragments.KnownForFragment;
import com.atmko.onmywatch.models.PersonData;

import org.parceler.Parcels;

/*
 * pager adapter hosting DetailFragment's child fragments for the series media type
 */

public class DetailPeopleExtrasAdapter extends FragmentStatePagerAdapter {
    private static final int TAB_COUNT = 2;

    private final PersonData mPersonData;

    public DetailPeopleExtrasAdapter(@NonNull FragmentManager fm, int behavior, PersonData personData) {
        super(fm, behavior);

        this.mPersonData = personData;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Fragment fragment = new Fragment();
        switch (position) {
            case 0:
                return KnownForFragment.newInstance(Parcels.wrap(mPersonData.getKnownForSeries()));

            case 1:
                return KnownForFragment.newInstance(Parcels.wrap(mPersonData.getKnownForMovies()));

            default:
                return fragment;
        }
    }

    @Override
    public int getCount() {
        return TAB_COUNT;
    }
}
