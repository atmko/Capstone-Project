/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.atmko.onmywatch.fragments.DiscoverCustomResultsFragment;
import com.atmko.onmywatch.fragments.DiscoverParentFragment;
import com.atmko.onmywatch.fragments.DiscoverResultsFragment;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;

/*
 * pager adapter for preset search results in DiscoveryFragment
 */

public class DiscoverPresetPagerAdapter extends FragmentStatePagerAdapter {
    private final int mMediaType;
    private final String[] mUrlList;
    private final SearchPreferences mSearchPreferences;

    public DiscoverPresetPagerAdapter(@NonNull FragmentManager fm, int behavior, int mediaType,
                                      String[] urlList, SearchPreferences searchPreferences) {
        super(fm, behavior);

        mUrlList = urlList;
        mMediaType = mediaType;
        mSearchPreferences = searchPreferences;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        String url = mUrlList[position];
        if (position == 0) {
            return DiscoverCustomResultsFragment.newInstance(DiscoverParentFragment.SEARCH_MODE_PRESET,
                    mMediaType,url,
                    mSearchPreferences);
        } else {
            return DiscoverResultsFragment.newInstance(DiscoverParentFragment.SEARCH_MODE_PRESET,
                    mMediaType,url,
                    mSearchPreferences);
        }
    }

    @Override
    public int getCount() {
        return mUrlList.length;
    }
}
