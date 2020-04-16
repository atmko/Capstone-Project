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
import com.atmko.onmywatch.adapters.CastDataAdapter;
import com.atmko.onmywatch.adapters.CustomParams;
import com.atmko.onmywatch.models.CastData;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
import com.atmko.onmywatch.utils.api_utils.PersonDataParser;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;
import com.atmko.onmywatch.view_models.DetailsViewModel;
import com.google.android.material.snackbar.Snackbar;

import org.parceler.Parcels;

import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;

public class CastFragment extends Fragment
        implements CastDataAdapter.OnListItemClickListener{

    private static final String FRAGMENT_KEY = "search_results_fragment";

    // the fragment initialization parameters
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String MEDIA_ID_KEY = "media_id";
    private static final String SEARCH_PREFERENCES_KEY = "search_preferences";

    private int mMediaType;
    private String mMediaId;
    private String mCastUrl;

    private DetailsViewModel viewModel;

    private CastDataAdapter mDataAdapter;
    private SearchPreferences mSearchPreferences;

    public CastFragment() {
        // Required empty public constructor
    }

    public static CastFragment newInstance(int mediaType, String mediaId) {

        CastFragment fragment = new CastFragment();
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
            //todo: read form get resources once and then use index to get media type
            if (mMediaType == MEDIA_TYPE_MOVIE) {
                mCastUrl =
                        getResources().getStringArray(R.array.cast_urls)[MEDIA_TYPE_MOVIE];

            } else if (mMediaType == MEDIA_TYPE_SERIES) {
                mCastUrl =
                        getResources().getStringArray(R.array.cast_urls)[MEDIA_TYPE_SERIES];
            }

            mMediaId = getArguments().getString(MEDIA_ID_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_cast, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getParentFragment() == null) return;

        viewModel = ((DetailsFragment) getParentFragment()).getViewModel();
        defineViews();

        //not saving/restoring adapter data to to TransactionTooLargeException
        if (savedInstanceState == null) {
            startNewSearch();

        } else {
            //get saved adapter data list, if null, start new search, otherwise restore old search
            List mediaDataList = viewModel.getCast();
            if (mediaDataList != null) {
                restoreSearch(savedInstanceState, mediaDataList);

            } else {
                startNewSearch();
            }
        }
    }

    private void startNewSearch() {
        mSearchPreferences = new SearchPreferences();
        executeSearch();
    }

    private void restoreSearch(Bundle savedInstanceState, List<CastData> mediaDataList) {
        mSearchPreferences = Parcels.unwrap(savedInstanceState.getParcelable(SEARCH_PREFERENCES_KEY));
        mDataAdapter.addAdapterData(mediaDataList);

        checkIfEmptyAdapter();
    }

    private void defineViews() {
        if (getActivity() == null) return;
        if (getView() == null) return;
        if (getParentFragment() == null) return;
        if (getParentFragment().getView() == null) return;

        RecyclerView recyclerView = getView().findViewById(R.id.cast_recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());

        mDataAdapter = new CastDataAdapter(this, getActivity().getApplicationContext(),
                CustomParams.getDetailExtrasParams(this));

        recyclerView.setAdapter(mDataAdapter);
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.detail_extras_column_span));

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void executeSearch() {
        if (getParentFragment() ==null) return;
        if (getParentFragment().getActivity() == null) return;

        //build AN request
        ANRequest request =
                NetworkFunctions.agnosticDetailRequestById(mCastUrl, mMediaId,
                        mSearchPreferences, getParentFragment().getActivity());

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String returnedJSONString) {
                //parse and populate retrieved data
                List<CastData> dataList = PersonDataParser.parseCast(returnedJSONString);
                mDataAdapter.addAdapterData(dataList);

                checkIfEmptyAdapter();
            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError);

                    return;
                }

                if (getActivity() == null) return;
                //notify user of error
                Snackbar.make(getActivity().findViewById(R.id.top_layout),
                        getString(R.string.cast_fetch_error_message),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void retryAfterCoolDOwn(ANError anError) {
        Log.d(FRAGMENT_KEY, "retry fetching search results");

        int coolDown = Integer.valueOf(anError.getResponse().header(ApiConstants.RETRY_AFTER_KEY));
        int coolDownInMilliSecs = coolDown * MILLISECOND_CONVERSION;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                executeSearch();

            }
        }, coolDownInMilliSecs);
    }

    @Override
    public void onItemClick(int position) {
        CastData selectedData = mDataAdapter.getAdapterData().get(position);
        //do nothing if selecting stack placeholder
        if (selectedData.getId() == null) return;

        if ((getParentFragment() != null) && (getParentFragment().getActivity() != null)) {
            ((MasterActivity) getParentFragment().getActivity()).launchPeopleDetailsFragment(selectedData);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(SEARCH_PREFERENCES_KEY, Parcels.wrap(mSearchPreferences));

        //save adapter data into view model(will survive config changes but not process killing)
        viewModel.setCast(mDataAdapter.getAdapterData());
    }

    private void checkIfEmptyAdapter() {
        if (getView() != null) {
            if (mDataAdapter.getAdapterData().size() == 0) {
                getView().findViewById(R.id.no_data_text_view).setVisibility(View.VISIBLE);

            } else {
                getView().findViewById(R.id.no_data_text_view).setVisibility(View.GONE);
            }
        }
    }
}