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

import com.atmko.onmywatch.Fragments.SearchParentFragment;
import com.atmko.onmywatch.Fragments.SearchResultsFragment;
import com.atmko.onmywatch.utils.SearchPreferences;

/*
 * pager adapter for preset search results in DiscoveryFragment
 */

public class SearchPresetPagerAdapter extends FragmentStatePagerAdapter {
    private int mMediaType;
    private String[] mUrlList;
    private SearchPreferences mSearchPreferences;

    public SearchPresetPagerAdapter(@NonNull FragmentManager fm, int behavior, int mediaType,
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
        return SearchResultsFragment.newInstance(SearchParentFragment.SEARCH_MODE_PRESET,
                mMediaType,url,
                mSearchPreferences);
    }

    @Override
    public int getCount() {
        return mUrlList.length;
    }

    @Nullable
    @Override
    public Parcelable saveState() {
        return super.saveState();
    }
}
