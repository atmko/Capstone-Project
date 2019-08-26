package com.atmko.onmywatch.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.Season;

import org.parceler.Parcels;

import java.util.ArrayList;

public class SeasonsFragment extends Fragment {
    public static String FRAGMENT_KEY = "seasons_fragment";

    private static final String SEASONS_KEY = "seasons";

    private ArrayList<Season> mSeasons;

    public SeasonsFragment() {
        // Required empty public constructor
    }

    public static SeasonsFragment newInstance(Parcelable seasons) {
        SeasonsFragment fragment = new SeasonsFragment();
        Bundle args = new Bundle();
        args.putParcelable(SEASONS_KEY, seasons);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSeasons = Parcels.unwrap(getArguments().getParcelable(SEASONS_KEY));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_seasons, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TabLayout seasonsTabLayout = getView().findViewById(R.id.seasons_tab_layout);
        seasonsTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        seasonsTabLayout.removeAllTabs();

        for (int index = 0; index < mSeasons.size(); index++) {
            String seasonNumber = mSeasons.get(index).mSeasonNumber;
            seasonsTabLayout.addTab(seasonsTabLayout.newTab().setText(seasonNumber));
        }
    }
}
