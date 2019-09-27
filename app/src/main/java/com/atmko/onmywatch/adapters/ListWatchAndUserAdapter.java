/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.atmko.onmywatch.Fragments.ListWatchAndUserFragment;
import com.atmko.onmywatch.Fragments.ListsWatchAndUserParentFragment;

/*
 * pager adapter hosting ListsWatchAndUserParentFragment's child fragments
 */

public class ListWatchAndUserAdapter extends FragmentStatePagerAdapter {
    private static final int TAB_COUNT = 2;

    public ListWatchAndUserAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {

        if (position == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            return ListWatchAndUserFragment
                    .newInstance(ListsWatchAndUserParentFragment.LIST_TYPE_WATCH);
        } else  {
            return ListWatchAndUserFragment
                    .newInstance(ListsWatchAndUserParentFragment.LIST_TYPE_USER);
        }
    }

    @Override
    public int getCount() {
        return TAB_COUNT;
    }
}
