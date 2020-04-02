/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

/*
 * pager adapter hosting ListsWatchAndUserParentFragment's child fragments
 */

public class ListWatchAndUserAdapter extends FragmentStatePagerAdapter {
    private int mCount;
    private LogicImplementation mLogicImplementation;

    public ListWatchAndUserAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    public interface LogicImplementation {
        Fragment launchFragment(int position);
    }

    public void setLogicImplementation(LogicImplementation logicImplementation) {
        mLogicImplementation = logicImplementation;
    }

    public void setTabCount(int count) {
        mCount = count;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return mLogicImplementation.launchFragment(position);
    }

    @Override
    public int getCount() {
        return mCount;
    }
}
