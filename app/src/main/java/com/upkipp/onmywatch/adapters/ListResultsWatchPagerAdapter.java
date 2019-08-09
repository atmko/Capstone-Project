//package com.upkipp.onmywatch.adapters;
//
//import android.content.Context;
//import android.os.Parcelable;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentStatePagerAdapter;
//
//import com.upkipp.onmywatch.Fragments.ListResultsFragment;
//import com.upkipp.onmywatch.R;
//
//public class ListResultsWatchPagerAdapter extends FragmentStatePagerAdapter {
//    String mListName;
//    private int[] mMediaTypes;
//
//    public ListResultsWatchPagerAdapter(@NonNull FragmentManager fm, int behavior,
//                                        Context context, int listType, String listName) {
//
//        super(fm, behavior);
//
//        mListName = listName;
//        mMediaTypes = context.getResources().getIntArray(R.array.list_media_types);
//    }
//
//    @NonNull
//    @Override
//    public Fragment getItem(int position) {
//        int mMediaType = position;
//        return ListResultsFragment.newInstance(mMediaType, mListName);
//    }
//
//    @Override
//    public int getCount() {
//        return mMediaTypes.length;
//    }
//
//    @Nullable
//    @Override
//    public Parcelable saveState() {
//        return super.saveState();
//    }
//}
