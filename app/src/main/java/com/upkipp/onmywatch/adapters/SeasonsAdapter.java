//package com.upkipp.onmywatch.adapters;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentPagerAdapter;
//
//import com.upkipp.onmywatch.Fragments.SeasonsFragment;
//import com.upkipp.onmywatch.models.Season;
//
//import java.util.ArrayList;
//
//public class SeasonsAdapter extends FragmentPagerAdapter {
//    private static int TAB_COUNT = 3;
//
//    private ArrayList<Season> mSeasons;
//
//    public SeasonsAdapter(@NonNull FragmentManager fm, int behavior, ArrayList<Season> seasons) {
//        super(fm, behavior);
//
//        this.mSeasons = seasons;
//    }
//
//
//    @NonNull
//    @Override
//    public Fragment getItem(int position) {
//
//        Season selectedSeason = mSeasons.get(position);
//        return new SeasonsFragment.newInstance("","");
//    }
//
//    @Override
//    public int getCount() {
//        return TAB_COUNT;
//    }
//}
