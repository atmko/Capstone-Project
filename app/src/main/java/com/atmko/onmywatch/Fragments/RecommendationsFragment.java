/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.CustomParams;
import com.atmko.onmywatch.adapters.MediaDataAdapter;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.api_utils.MovieDataParser;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;
import com.atmko.onmywatch.utils.api_utils.SeriesDataParser;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.stack.Stack;
import com.google.android.material.snackbar.Snackbar;

import org.parceler.Parcels;

import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;
import static com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker.REQUEST_COOL_DOWN;

public class RecommendationsFragment extends Fragment
        implements MediaDataAdapter.OnListItemClickListener{

    private static final String FRAGMENT_KEY = "search_results_fragment";

    // the fragment initialization parameters
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String MEDIA_ID_KEY = "media_id";
    private static final String SEARCH_PREFERENCES_KEY = "search_preferences";

    private int mMediaType;
    private String mMediaId;
    private String mRecommendationUrl;

    private static final String ADAPTER_DATA_LIST_KEY = "adapter_data_list";
    private static final String PAGING_BLOCK_MAP_KEY = "paging_block_map";

    private MediaDataAdapter mDataAdapter;
    private Stack mStack;
    private SearchPreferences mSearchPreferences;

    public RecommendationsFragment() {
        // Required empty public constructor
    }

    public static RecommendationsFragment newInstance(int mediaType, String mediaId) {

        RecommendationsFragment fragment = new RecommendationsFragment();
        Bundle args = new Bundle();
        args.putInt(MEDIA_TYPE_KEY, mediaType);
        args.putString(MEDIA_ID_KEY, mediaId);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMediaType = getArguments().getInt(MEDIA_TYPE_KEY);
            if (mMediaType == MEDIA_TYPE_MOVIE) {
                mRecommendationUrl =
                        getResources().getStringArray(R.array.recommendation_urls)[MEDIA_TYPE_MOVIE];

            } else if (mMediaType == MEDIA_TYPE_SERIES) {
                mRecommendationUrl =
                        getResources().getStringArray(R.array.recommendation_urls)[MEDIA_TYPE_SERIES];
            }

            mMediaId = getArguments().getString(MEDIA_ID_KEY);
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
            mSearchPreferences = new SearchPreferences();
            loadSearch();

        } else {
            mSearchPreferences = Parcels.unwrap(savedInstanceState.getParcelable(SEARCH_PREFERENCES_KEY));

            //get saved adapter data list
            List mediaDataList = Parcels.unwrap(
                    savedInstanceState.getParcelable(ADAPTER_DATA_LIST_KEY));

            mDataAdapter.addAdapterData(mediaDataList);

            //get saved paging block map
            int[] pagingBlockRange = savedInstanceState.getIntArray(PAGING_BLOCK_MAP_KEY);
            mStack.restorePagingBlockStructure(pagingBlockRange, mediaDataList);

            //set total pages
            mStack.setTotalPages(mSearchPreferences.getTotalPages());
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

        mDataAdapter = new MediaDataAdapter(this, getActivity().getApplicationContext(),
                CustomParams.getDetailExtrasParams(this));

        Object preloadObject = null;
        if (mMediaType == MEDIA_TYPE_MOVIE) {
            preloadObject = new MovieData();

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            preloadObject = new SeriesData();

        }

        recyclerView.setAdapter(mDataAdapter);
        mStack = new Stack(false,getResources().getInteger(R.integer.stack_block_limit), pagingBlockTemplate,
                preloadObject, recyclerView, mDataAdapter, true);
        recyclerView.addOnScrollListener(mStack);
    }

    private void loadSearch() {
        mStack.initialize();
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.detail_extras_column_span));

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void executeSearch(final int blockNumber, final int targetPage, final int stackOperation) {
        if (getParentFragment().getActivity() == null) return;

        //build AN request
        ANRequest request =
                NetworkFunctions.agnosticDetailRequestById(mRecommendationUrl, mMediaId,
                        mSearchPreferences, getParentFragment().getActivity());

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

                }

                mStack.stackPage(blockNumber, targetPage, dataList, stackOperation);
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
                        getString(R.string.recommendations_fetch_error_message),
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

    @Override
    public void onItemClick(int position) {
        MediaData selectedData = mDataAdapter.getAdapterData().get(position);
        //do nothing if selecting stack placeholder
        if (selectedData.getId() == null) return;

        if (getParentFragment() != null && getParentFragment().getActivity() != null) {
            ((MasterActivity) getParentFragment().getActivity()).launchDetailsFragment(selectedData, null);
        }
    }

    @Override
    public void onAddButtonClick(final int position) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    ((MasterActivity) getActivity())
                            .launchAddToListActivity(mDataAdapter.getAdapterData().get(position));
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(SEARCH_PREFERENCES_KEY, Parcels.wrap(mSearchPreferences));

        outState.putParcelable(ADAPTER_DATA_LIST_KEY,
                Parcels.wrap(mDataAdapter.getAdapterData()));

        outState.putIntArray(PAGING_BLOCK_MAP_KEY, mStack.saveBlockStructure());
    }
}