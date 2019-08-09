package com.upkipp.onmywatch.adapters;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.upkipp.onmywatch.Fragments.SearchResultsFragment;
import com.upkipp.onmywatch.utils.SearchPreferences;

public class SearchManualPagerAdapter extends FragmentStatePagerAdapter {
    private String[] mUrlList;
    private SearchPreferences mSearchPreferences;

    public SearchManualPagerAdapter(@NonNull FragmentManager fm, int behavior,
                                    String[] urlList, SearchPreferences searchPreferences) {
        super(fm, behavior);

        mUrlList = urlList;
        mSearchPreferences = searchPreferences;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        String url = mUrlList[position];
        return SearchResultsFragment.newInstance(position, url, mSearchPreferences);
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
