package com.upkipp.onmywatch.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.material.tabs.TabLayout;
import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.adapters.ListResultsUserPagerAdapter;

public class ListResultsParentFragment extends Fragment {
    public static String FRAGMENT_KEY = "list_results_parent_fragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters
    private static final String LIST_TYPE_KEY = "list_type";
    private static final String LIST_NAME_KEY = "list_name";

    // TODO: Rename and change types of parameters
    private int mListType;
    private String mListName;

    private OnFragmentInteractionListener mListener;

    public ListResultsParentFragment() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters
    public static ListResultsParentFragment newInstance(int listType, String listName) {
        ListResultsParentFragment fragment = new ListResultsParentFragment();
        Bundle args = new Bundle();
        args.putInt(LIST_TYPE_KEY, listType);
        args.putString(LIST_NAME_KEY, listName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mListType = getArguments().getInt(LIST_TYPE_KEY);
            mListName = getArguments().getString(LIST_NAME_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_results_parent, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        EditText mSearchEditTextView = getView().findViewById(R.id.search_edit_text_view);
        defineViews();
    }

    private void defineViews() {
        TabLayout mMediaTypeTabLayout = getView().findViewById(R.id.media_type_tab_layout);
        final ViewPager mListsViewPager = getView().findViewById(R.id.lists_view_pager);

        //remove old tabs
        mMediaTypeTabLayout.removeAllTabs();

        //add new tabs
        String[] listMediaTypes = getContext().getResources().getStringArray(R.array.list_media_types);

        for (String type : listMediaTypes) {
            mMediaTypeTabLayout.addTab(mMediaTypeTabLayout.newTab().setText(type));
        }

        mListsViewPager.setOffscreenPageLimit(listMediaTypes.length - 1);

        FragmentStatePagerAdapter resultsAdapter = new ListResultsUserPagerAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,getContext(),
                mListType, mListName);

        mListsViewPager.setAdapter(resultsAdapter);

        //configure listeners
        mListsViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mMediaTypeTabLayout));
        mMediaTypeTabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mListsViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
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
