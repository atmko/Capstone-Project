package com.upkipp.onmywatch.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.upkipp.onmywatch.Fragments.ListWatchAndUserFragment;
import com.upkipp.onmywatch.Fragments.ListsParentFragment;

public class ListWatchAndUserAdapter extends FragmentStatePagerAdapter {
    private static int TAB_COUNT = 2;

    public ListWatchAndUserAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {

        if (position == ListsParentFragment.LIST_TYPE_WATCH) {
            return ListWatchAndUserFragment.newInstance(ListsParentFragment.LIST_TYPE_WATCH);
        } else  {
            return ListWatchAndUserFragment.newInstance(ListsParentFragment.LIST_TYPE_USER);
        }
    }

    @Override
    public int getCount() {
        return TAB_COUNT;
    }
}
