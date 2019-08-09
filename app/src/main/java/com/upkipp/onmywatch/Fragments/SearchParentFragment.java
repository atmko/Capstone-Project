package com.upkipp.onmywatch.Fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.adapters.SearchManualPagerAdapter;
import com.upkipp.onmywatch.adapters.SearchPresetPagerAdapter;
import com.upkipp.onmywatch.utils.SearchPreferences;

import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class SearchParentFragment extends Fragment {
    public static String FRAGMENT_KEY = "search_parent_fragment";

    public static final String SEARCH_MODE_PRESET = "preset_search";
    public static final String SEARCH_MODE_MANUAL = "manual_search";

    public static final String SEARCH_MODE_KEY = "search_mode";
    public static final String MEDIA_TYPE_KEY = "media_type";
    private static final String FRAGMENT_PAGER_ADAPTER_KEY = "fragment_pager_adapter";

    private String searchMode;
    private int mediaType;
    private FragmentStatePagerAdapter resultsAdapter;

    private TextView searchEditTextView;
    private TabLayout mediaTypeTabLayout;
    private FrameLayout searchPresetsTopLayout;
    private TabLayout searchPresetsTabLayout;
    private FloatingActionButton manualSearchFab;
    private ViewPager searchResultsViewPager;

//    private OnFragmentInteractionListener mListener;

    public SearchParentFragment() {
        // Required empty public constructor
    }

    public static SearchParentFragment newInstance() {
        SearchParentFragment fragment = new SearchParentFragment();
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

        return inflater.inflate(R.layout.fragment_search_parent, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();

        if (savedInstanceState == null) {
            searchMode = SEARCH_MODE_PRESET;
            //TODO replace MEDIA_TYPE_SERIES with default media shared preference
            mediaType = MEDIA_TYPE_SERIES;

            //load default ui
            loadPresetSearchUi();

        } else {
            searchMode = savedInstanceState.getString(SEARCH_MODE_KEY);
            mediaType = savedInstanceState.getInt(MEDIA_TYPE_KEY);

            if (searchMode.equals(SEARCH_MODE_PRESET)) {
                loadPresetSearchUi();

            } else if (searchMode.equals(SEARCH_MODE_MANUAL)){
                loadManualSearchUi();
            }
        }
    }

    private void defineViews() {
        mediaTypeTabLayout = getView().findViewById(R.id.media_type_tab_layout);
        searchPresetsTopLayout = getView().findViewById(R.id.search_presets_top_layout);
        searchPresetsTabLayout = getView().findViewById(R.id.search_presets_tab_layout);
        searchResultsViewPager = getView().findViewById(R.id.search_results_view_pager);

        configureUpNavigationButton();
        configureSearchBox();
        configureFab();
    }

    private void configureUpNavigationButton() {
        getView().findViewById(R.id.up_navigation_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                fragmentManager.popBackStack();
            }
        });
    }

    private void configureSearchBox() {
        searchEditTextView = getView().findViewById(R.id.search_edit_text_view);
        //configure search box action event
        searchEditTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                //set sort parameter
                //note: setting parameter here is preferred instead of "on show search button" clicked

                hideSoftKeyboard(searchEditTextView);

                //set focus to top layout(away from search box)
                getView().findViewById(R.id.top_layout).requestFocus();


                //execute search
                loadManualSearchUi();

                return true;
            }
        });
    }


    private void configureFab() {
        manualSearchFab = getView().findViewById(R.id.manual_search_fab);

        manualSearchFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchMode.equals(SEARCH_MODE_PRESET)) {
                    if (searchEditTextView.getVisibility() == View.VISIBLE) {
                        searchEditTextView.setVisibility(View.GONE);

                    } else {
                        searchEditTextView.setVisibility(View.VISIBLE);

                    }

                } else if (searchMode.equals(SEARCH_MODE_MANUAL)) {
                    loadPresetSearchUi();
                    searchEditTextView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void loadPresetSearchUi() {
        searchPresetsTopLayout.setVisibility(View.VISIBLE);

        searchMode = SEARCH_MODE_PRESET;
        manualSearchFab.setImageResource(R.drawable.ic_manual_search);

//        getView().findViewById(R.id.search_edit_text_view).setVisibility(View.GONE);
//        manualSearchFab.setImageResource(R.drawable.ic_manual_search);

        configurePresetMediaTypeTabLayout();
        loadPresetTabAndPager();
    }

    private void configurePresetMediaTypeTabLayout() {
        //remove old tabs
        mediaTypeTabLayout.removeAllTabs();

        //add new tabs
        String[] searchTypesList = getContext().getResources().getStringArray(R.array.search_preset_media_types);

        for (String type : searchTypesList) {
            mediaTypeTabLayout.addTab(mediaTypeTabLayout.newTab().setText(type));
        }

        mediaTypeTabLayout.getTabAt(mediaType).select();

        //clear old listeners to avoid conflicts
        mediaTypeTabLayout.clearOnTabSelectedListeners();

        //configure new listener
        mediaTypeTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mediaType = tab.getPosition();
                loadPresetTabAndPager();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void loadPresetTabAndPager() {
        //remove old tabs
        searchPresetsTabLayout.removeAllTabs();

        //add new tabs
        String[] titleList = null;
        //configure urls
        String[] urlList = null;

        if (mediaType == MEDIA_TYPE_MOVIE) {
            titleList = getContext().getResources().getStringArray(R.array.preset_movie_search_titles);
            urlList = getContext().getResources().getStringArray(R.array.preset_movie_search_urls);

        } else if (mediaType == MEDIA_TYPE_SERIES){
            titleList = getContext().getResources().getStringArray(R.array.preset_tv_search_titles);
            urlList = getContext().getResources().getStringArray(R.array.preset_tv_search_urls);
        }

        for (String title : titleList) {
            searchPresetsTabLayout.addTab(searchPresetsTabLayout.newTab().setText(title));
        }

        searchResultsViewPager.setOffscreenPageLimit(titleList.length - 1);

        //configure search preferences
        SearchPreferences searchPreferences = new SearchPreferences();

        resultsAdapter = new SearchPresetPagerAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, mediaType, urlList,
                searchPreferences);

        searchResultsViewPager.setAdapter(resultsAdapter);

        //clear old listeners to avoid conflicts
        searchResultsViewPager.clearOnPageChangeListeners();

        //configure new listeners
        searchResultsViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(searchPresetsTabLayout));
        searchPresetsTabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                searchResultsViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

//    private void toggleSearchBoxVisibility() {
////        getView().findViewById(R.id.search_edit_text_view).setVisibility(View.VISIBLE);
//        manualSearchFab.setImageResource(R.drawable.ic_cancel_manual_search);
//    }

    private void loadManualSearchUi() {
        //hide presets tab
        searchPresetsTopLayout.setVisibility(View.GONE);

        searchMode = SEARCH_MODE_MANUAL;
        manualSearchFab.setImageResource(R.drawable.ic_cancel_manual_search);

        configureManualMediaTypeTabLayout();
        loadManualPager(searchEditTextView.getText().toString());
    }

    private void configureManualMediaTypeTabLayout() {
        //remove old tabs
        mediaTypeTabLayout.removeAllTabs();

        //add new tabs
        String[] searchTypesList = getContext().getResources().getStringArray(R.array.search_manual_media_types);

        for (String type : searchTypesList) {
            mediaTypeTabLayout.addTab(mediaTypeTabLayout.newTab().setText(type));
        }

        //clear old listeners to avoid conflicts
        mediaTypeTabLayout.clearOnTabSelectedListeners();

        //configure new listener
        mediaTypeTabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mediaType = tab.getPosition();
                searchResultsViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void loadManualPager(String query) {
        //clear old listeners to avoid conflicts
        searchResultsViewPager.clearOnPageChangeListeners();

        //configure new listener
        searchResultsViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mediaTypeTabLayout));

        //configure view pager adapter

        //configure urls
        String[] urlList = getContext().getResources().getStringArray(R.array.manual_search_urls);

        //configure search preferences
        SearchPreferences searchPreferences = new SearchPreferences();
        searchPreferences.setQuery(query);

        resultsAdapter = new SearchManualPagerAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, urlList,
                searchPreferences);

        searchResultsViewPager.setAdapter(resultsAdapter);
    }

    public void hideSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SEARCH_MODE_KEY, searchMode);
        outState.putInt(MEDIA_TYPE_KEY, mediaType);
        outState.putParcelable(FRAGMENT_PAGER_ADAPTER_KEY, resultsAdapter.saveState()); }


    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }

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
//        mListener = null;
        Log.d(FRAGMENT_KEY, "detaching");

    }

//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
//    }
}
