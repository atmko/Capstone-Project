/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.atmko.onmywatch.Fragments.DiscoverParentFragment;
import com.atmko.onmywatch.Fragments.DiscoverResultsFragment;
import com.atmko.onmywatch.utils.SearchPreferences;

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
        return DiscoverResultsFragment.newInstance(DiscoverParentFragment.SEARCH_MODE_PRESET,
                mMediaType,url,
                mSearchPreferences);
    }

    @Override
    public int getCount() {
        return mUrlList.length;
    }
}
