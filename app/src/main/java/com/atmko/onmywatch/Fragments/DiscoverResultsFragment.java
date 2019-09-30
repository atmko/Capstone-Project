/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.adapters.CastDataAdapter;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.stack.Stack;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.MediaDataAdapter;
import com.atmko.onmywatch.adapters.PeopleDataAdapter;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.PersonData;
import com.atmko.onmywatch.utils.MovieDataParser;
import com.atmko.onmywatch.utils.PersonDataParser;
import com.atmko.onmywatch.utils.SearchPreferences;
import com.atmko.onmywatch.utils.SeriesDataParser;
import com.atmko.onmywatch.utils.network_utils.NetworkFunctions;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.parceler.Parcels;

import java.util.Arrays;
import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_PEOPLE;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;
import static com.atmko.onmywatch.utils.UpdateMediaWorker.REQUEST_COOL_DOWN;

public class DiscoverResultsFragment extends Fragment implements
        MediaDataAdapter.OnListItemClickListener,
        PeopleDataAdapter.OnListItemClickListener{

    private static final String FRAGMENT_KEY = "search_results_fragment";

    // the fragment initialization parameters
    private static final String SEARCH_TYPE_KEY = "search_type";
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String SEARCH_URL_KEY = "search_url";
    private static final String SEARCH_PREFERENCES_KEY = "search_preferences";

    private String mSearchType;
    private int mMediaType;
    private String mSearchUrl;

    private static final String ADAPTER_DATA_LIST_KEY = "adapter_data_list";
    private static final String PAGING_BLOCK_MAP_KEY = "paging_block_map";

    //check for restoring state
    private boolean mIsFirstInit = true;
    private RecyclerView.Adapter mDataAdapter;
    private Stack mStack;
    private SearchPreferences mSearchPreferences;
    private SuperEditText mSearchTextView;


    public DiscoverResultsFragment() {
        // Required empty public constructor
    }

    public static DiscoverResultsFragment newInstance(String searchType,
                                                      int mediaType,
                                                      String searchUrl,
                                                      SearchPreferences searchPreferencesParcel) {

        DiscoverResultsFragment fragment = new DiscoverResultsFragment();
        Bundle args = new Bundle();
        args.putString(SEARCH_TYPE_KEY, searchType);
        args.putInt(MEDIA_TYPE_KEY, mediaType);
        args.putString(SEARCH_URL_KEY, searchUrl);
        args.putParcelable(SEARCH_PREFERENCES_KEY, Parcels.wrap(searchPreferencesParcel));

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSearchType = getArguments().getString(SEARCH_TYPE_KEY);
            mMediaType = getArguments().getInt(MEDIA_TYPE_KEY);
            mSearchUrl = getArguments().getString(SEARCH_URL_KEY);
            mSearchPreferences = Parcels.unwrap(getArguments().getParcelable(SEARCH_PREFERENCES_KEY));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_discover_results, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();

        if (savedInstanceState == null) {
            loadSearch();

        } else {
            //restore search if it exists
            final ImageButton searchImageButton = getParentFragment().
                    getView().findViewById(R.id.search_image_button);
            MasterActivity masterActivity = ((MasterActivity) getActivity());
            masterActivity.restoreSavedSearch(DiscoverResultsFragment.this,
                    mIsFirstInit, savedInstanceState, searchImageButton, mSearchTextView);

            mIsFirstInit = false;

            List mediaDataList;

            if (mMediaType == MEDIA_TYPE_PEOPLE) {
                //get saved adapter data list
                mediaDataList = Parcels.unwrap(
                        savedInstanceState.getParcelable(ADAPTER_DATA_LIST_KEY));

                ((PeopleDataAdapter) mDataAdapter).addAdapterData(mediaDataList);

            } else {
                //get saved adapter data list
                mediaDataList = Parcels.unwrap(
                        savedInstanceState.getParcelable(ADAPTER_DATA_LIST_KEY));

                ((MediaDataAdapter) mDataAdapter).addAdapterData(mediaDataList);
            }

            //get saved paging block map
            int[] pagingBlockRange = savedInstanceState.getIntArray(PAGING_BLOCK_MAP_KEY);
            mStack.restorePagingBlockStructure(pagingBlockRange, mediaDataList);

            //set total pages
            mStack.setTotalPages(mSearchPreferences.getTotalPages());

            loadDetailFragment();
        }
    }

    private void defineViews() {
        Stack.PagingBlockTemplate pagingBlockTemplate =
                new Stack.PagingBlockTemplate(new Stack.PagingBlockTemplate.OnCreatePageLoader() {
            @Override
            public void onPageEndReached(final int blockNumber, final int targetPage) {
                if (targetPage == mStack.getFirstPage()) {
                    mSearchPreferences.setTargetPage(targetPage);
                    executeSearch(blockNumber, targetPage, Stack.GO_DOWN_ONE_BLOCK);

                } else {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mSearchPreferences.setTargetPage(targetPage);
                            executeSearch(blockNumber, targetPage, Stack.GO_DOWN_ONE_BLOCK);
                        }
                    }, REQUEST_COOL_DOWN);
                }
            }

            @Override
            public void onPageStartReached(final int blockNumber, final int targetPage) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSearchPreferences.setTargetPage(targetPage);
                        executeSearch(blockNumber, targetPage, Stack.GO_UP_ONE_BLOCK);
                    }
                }, REQUEST_COOL_DOWN);
            }
        }, ApiConstants.RESULTS_PER_PAGE, getResources().getInteger(R.integer.stack_pages_per_block));

        RecyclerView recyclerView = getView().findViewById(R.id.discover_results_recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());

        if (mMediaType == MEDIA_TYPE_PEOPLE) {
            mDataAdapter = new PeopleDataAdapter(this,
                    getActivity().getApplicationContext());

        } else {
            mDataAdapter = new MediaDataAdapter(this,
                    getActivity().getApplicationContext());
        }

        Object preloadObject = null;
        if (mMediaType == MEDIA_TYPE_MOVIE) {
            preloadObject = new MovieData();

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            preloadObject = new SeriesData();


        } else if (mMediaType == MEDIA_TYPE_PEOPLE) {
            preloadObject = new PersonData();

        }

        recyclerView.setAdapter(mDataAdapter);
        mStack = new Stack(false,getResources().getInteger(R.integer.stack_block_limit), pagingBlockTemplate,
                preloadObject, recyclerView, mDataAdapter, true);
        recyclerView.addOnScrollListener(mStack);

        //get search bar from parent fragment
        mSearchTextView =
                getParentFragment().getView().findViewById(R.id.search_edit_text_view);
    }

    private void loadSearch() {
        mStack.initialize();
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.search_column_span));

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void executeSearch(final int blockNumber, final int targetPage, final int stackOperation) {
        if (getParentFragment().getActivity() == null) return;

        //build AN request
        ANRequest request = NetworkFunctions.agnosticSearchRequest(mSearchUrl, mSearchPreferences,
                getParentFragment().getActivity());

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String returnedJSONString) {
                mStack.setIsFrozen(false);

                //parse and populate retrieved data

                List dataList = null;

                if (mMediaType == MEDIA_TYPE_MOVIE) {
                    dataList =
                            MovieDataParser.parseData(returnedJSONString, mStack, mSearchPreferences);

                } else if (mMediaType == MEDIA_TYPE_SERIES) {
                    dataList =
                            SeriesDataParser.parseData(returnedJSONString, mStack, mSearchPreferences);

                } else if (mMediaType == MEDIA_TYPE_PEOPLE) {
                    dataList =
                            PersonDataParser.parseData(returnedJSONString, mStack, mSearchPreferences);

                }

                mStack.stackPage(blockNumber, targetPage, dataList, stackOperation);

                loadDetailFragment();

            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    mStack.setIsFrozen(true);
                    retryAfterCoolDOwn(anError, blockNumber, targetPage, stackOperation);

                    return;
                }

                //mStack page on failure to include empty results in recycler view,
                //also so mStack can keep counts for when to remove a block
                mStack.stackPage(blockNumber, targetPage, null, stackOperation);

                mStack.setIsFrozen(false);

                //notify user of error
                Snackbar.make(getActivity().findViewById(R.id.top_layout),
                        getString(R.string.discover_results_fetch_error_message),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void retryAfterCoolDOwn(ANError anError, final int blockNumber, final int targetPage,
                                    final int stackOperation) {
        Log.d(FRAGMENT_KEY, "retry fetching search results");

        int coolDown = Integer.valueOf(anError.getResponse().header(ApiConstants.RETRY_AFTER_KEY));
        int coolDownInMilliSecs = coolDown * MILLISECOND_CONVERSION;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                executeSearch(blockNumber, targetPage, stackOperation);

            }
        }, coolDownInMilliSecs);
    }

    //loads detail fragment:
    //if tablet is landscape
    // && detail fragment container has no fragment
    //&& this is currently selected tab
    // && stack is not currently waiting for more pages to load
    // && is containing fragment on top in fragment detail container
    private void loadDetailFragment() {
        MasterActivity masterActivity = ((MasterActivity) getParentFragment().getActivity());

        if (masterActivity == null) return;

        Fragment activeFragment =
                masterActivity.getSupportFragmentManager().findFragmentById(R.id.master_fragments_container);
        String activeClassName = activeFragment.getClass().getName();
        String parentClassName = getParentFragment().getClass().getName();

        boolean isParentActive = activeClassName.equals(parentClassName);

        if (masterActivity.isTabletLandscape()
                && !masterActivity.hasFragment(R.id.detail_fragments_container)
                && isCurrentTab()
                && mStack.isIdle()
                //TODO consider detaching fragments to disable background updates instead of "isParentActive"
                && isParentActive) {

            MediaData firstMediaData;

            if (mMediaType == MEDIA_TYPE_PEOPLE) {
                ((PeopleDataAdapter) mDataAdapter).getAdapterData().get(0);

            } else {
                firstMediaData =
                        ((MediaDataAdapter) mDataAdapter).getAdapterData().get(0);

                startDetailsFragment(firstMediaData);

            }
        }
    }

    //uses the saved selected tab position to get the corresponding url
    //if the corresponding url = this fragment's url(mSearchUrl), returns true
    //compares the url list of each tab to checks if this fragment is the currently selected tab
    private boolean isCurrentTab() {
        //if this is a manual search
        if (mSearchType.equals(DiscoverParentFragment.SEARCH_MODE_MANUAL)) {
            //get the list of manual search urls and compare to this fragment's mSearchUrl
            String[] manualUrls =
                    getParentFragment().getActivity().getResources()
                            .getStringArray(R.array.manual_discover_urls);

            List<String> manualUrlList = Arrays.asList(manualUrls);

            return manualUrlList.indexOf(mSearchUrl)
                    == ((DiscoverParentFragment) getParentFragment()).getCurrentTabPosition();

            //if this is a preset search
        } else {
            //get the list of preset search urls and compare it to the this fragment's mSearchUrl
            String[] presetUrls;

            if (mMediaType == MEDIA_TYPE_MOVIE) {
                presetUrls =
                        getParentFragment().getActivity().getResources()
                                .getStringArray(R.array.preset_movie_discover_urls);

            } else if (mMediaType == MEDIA_TYPE_SERIES) {
                presetUrls =
                        getParentFragment().getActivity().getResources()
                                .getStringArray(R.array.preset_series_discover_urls);

            } else {
                throw new Error("no specified media type");

            }

            List<String> presetUrlList = Arrays.asList(presetUrls);

            return presetUrlList.indexOf(mSearchUrl) ==
                    ((DiscoverParentFragment) getParentFragment()).getCurrentTabPosition();
        }
    }

    @Override
    public void onItemClick(int position) {
        Object selectedData;

        if (mDataAdapter instanceof MediaDataAdapter) {
            selectedData = ((MediaDataAdapter) mDataAdapter).getAdapterData().get(position);
            //do nothing if selecting stack placeholder
            if (((MediaData) selectedData).getId() == null) return;
            startDetailsFragment(((MediaData) selectedData));

        } else {
            selectedData = ((PeopleDataAdapter) mDataAdapter).getAdapterData().get(position);
            //do nothing if selecting stack placeholder
            if (((PersonData) selectedData).getId() == null) return;

        }
    }

    private void startDetailsFragment(MediaData selectedData) {
        ((MasterActivity) getActivity()).launchDetailsFragment(selectedData, null);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //update initialized search preferences
        getArguments().putParcelable(SEARCH_PREFERENCES_KEY, Parcels.wrap(mSearchPreferences));

        if (mMediaType == MEDIA_TYPE_PEOPLE) {
            outState.putParcelable(ADAPTER_DATA_LIST_KEY,
                    Parcels.wrap(((PeopleDataAdapter)mDataAdapter).getAdapterData()));

        } else {
            outState.putParcelable(ADAPTER_DATA_LIST_KEY,
                    Parcels.wrap(((MediaDataAdapter)mDataAdapter).getAdapterData()));
        }

        outState.putIntArray(PAGING_BLOCK_MAP_KEY, mStack.saveBlockStructure());

        //save search bar text
        outState.putString(MasterActivity.SEARCH_TEXT_KEY, mSearchTextView.getText().toString());

        //save search bar visibility
        outState.putInt(MasterActivity.SEARCH_BAR_VISIBILITY_KEY, mSearchTextView.getVisibility());

    }
}
