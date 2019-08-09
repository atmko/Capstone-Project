package com.upkipp.onmywatch.Fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.adapters.ListWatchAndUserAdapter;


public class ListsParentFragment extends Fragment {
    public static String FRAGMENT_KEY = "lists_parent_fragment";
    public static final int LIST_TYPE_WATCH = 0;
    public static final int LIST_TYPE_USER = 1;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


    // TODO: Rename and change types of parameters
    private int mListCategory;

    private OnFragmentInteractionListener mListener;

    ViewPager mViewPager;
    ListWatchAndUserAdapter mListWatchAndUserAdapter;

    public ListsParentFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static ListsParentFragment newInstance() {
        ListsParentFragment fragment = new ListsParentFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_lists_parent, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        defineViews();
    }

    private void defineViews() {
        mViewPager = getView().findViewById(R.id.lists_view_pager);
        mListWatchAndUserAdapter = new ListWatchAndUserAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mViewPager.setAdapter(mListWatchAndUserAdapter);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

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
