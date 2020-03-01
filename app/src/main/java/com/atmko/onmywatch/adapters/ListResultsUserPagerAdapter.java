/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.atmko.onmywatch.Fragments.ListResultsFragment;
import com.atmko.onmywatch.R;

/*
 * pager adapter hosting ListResultsParentFragment's child fragments
 */

public class ListResultsUserPagerAdapter extends FragmentStatePagerAdapter {
    private final int mListType;
    private final String mListName;
    private final int[] mMediaTypes;

    public ListResultsUserPagerAdapter(@NonNull FragmentManager fm, int behavior,
                                       Context context, int listType, String listName) {

        super(fm, behavior);

        mListType = listType;
        mListName = listName;
        mMediaTypes = context.getResources().getIntArray(R.array.list_media_types);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        //equate media type to adapter position
        @SuppressWarnings("UnnecessaryLocalVariable")
        int mMediaType = position;

        return ListResultsFragment.newInstance(mListType, mMediaType, mListName);
    }

    @Override
    public int getCount() {
        return mMediaTypes.length;
    }
}
