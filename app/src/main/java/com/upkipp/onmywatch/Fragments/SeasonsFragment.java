package com.upkipp.onmywatch.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.models.Season;

import org.parceler.Parcels;

import java.util.ArrayList;

public class SeasonsFragment extends Fragment {
    public static String FRAGMENT_KEY = "seasons_fragment";

    private static final String SEASONS_KEY = "seasons";

    private ArrayList<Season> mSeasons;

    private OnFragmentInteractionListener mListener;

    public SeasonsFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
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
        };
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
