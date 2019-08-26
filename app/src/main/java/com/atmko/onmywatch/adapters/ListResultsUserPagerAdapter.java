package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.atmko.onmywatch.Fragments.ListResultsFragment;
import com.atmko.onmywatch.R;

public class ListResultsUserPagerAdapter extends FragmentStatePagerAdapter {
    private int mListType;
    private String mListName;
    private int[] mMediaTypes;

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
        int mMediaType = position;

        return ListResultsFragment.newInstance(mListType, mMediaType, mListName);
    }

    @Override
    public int getCount() {
        return mMediaTypes.length;
    }

    @Nullable
    @Override
    public Parcelable saveState() {
        return super.saveState();
    }
}
