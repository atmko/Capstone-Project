package com.atmko.onmywatch.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.atmko.onmywatch.adapters.HomeSpotlightAdapter;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.utils.MovieDataParser;
import com.atmko.onmywatch.utils.SearchPreferences;
import com.atmko.onmywatch.utils.SeriesDataParser;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.NetworkFunctions;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.parceler.Parcels;

import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;

public class HomeSpotlightFragment extends Fragment implements
        HomeSpotlightAdapter.OnListItemClickListener {

    public static String FRAGMENT_KEY = "search_results_fragment";

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String SEARCH_URL_KEY = "search_url";
    private static final String SEARCH_PREFERENCES_KEY = "search_preferences";

    private int mMediaType;
    private String mSearchUrl;

    private static final String ADAPTER_DATA_LIST_KEY = "adapter_data_list";
    private static final String PAGING_BLOCK_MAP_KEY = "paging_block_map";

    private HomeSpotlightAdapter mDataAdapter;
    private SearchPreferences mSearchPreferences;

    public HomeSpotlightFragment() {
        // Required empty public constructor
    }

    public static HomeSpotlightFragment newInstance(int mediaType, String searchUrl,
                                                    SearchPreferences searchPreferencesParcel) {
        HomeSpotlightFragment fragment = new HomeSpotlightFragment();
        Bundle args = new Bundle();
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
            mMediaType = getArguments().getInt(MEDIA_TYPE_KEY);
            mSearchUrl = getArguments().getString(SEARCH_URL_KEY);
            mSearchPreferences = Parcels.unwrap(getArguments().getParcelable(SEARCH_PREFERENCES_KEY));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_search_results, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();

        if (savedInstanceState == null) {
            mSearchPreferences.setTargetPage(1);
            executeSearch();

        } else {
            //get saved adapter data list
            List<MediaData> mediaDataList = Parcels.unwrap(
                    savedInstanceState.getParcelable(ADAPTER_DATA_LIST_KEY));

            mDataAdapter.addAdapterData(mediaDataList);

            loadDetailFragment();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mDataAdapter.getAdapterData().size() == 0) {
            executeSearch();
        }
    }

    private void defineViews() {
        RecyclerView recyclerView = getView().findViewById(R.id.search_results_recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());

        mDataAdapter = new HomeSpotlightAdapter(this,
                getActivity().getApplicationContext());

        recyclerView.setAdapter(mDataAdapter);

    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),1);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        return layoutManager;
    }

    private void executeSearch() {
        //build AN request
        ANRequest request = NetworkFunctions.agnosticSearchRequest(mSearchUrl,
                mSearchPreferences, getParentFragment().getActivity());

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String returnedJSONString) {
                try {
                    //parse and populate retrieved data
                    List dataList = null;

                    if (mMediaType == MEDIA_TYPE_MOVIE) {
                        dataList =
                                MovieDataParser.parseData(returnedJSONString,
                                        null, mSearchPreferences);

                    } else if (mMediaType == MEDIA_TYPE_SERIES) {
                        dataList =
                                SeriesDataParser.parseData(returnedJSONString,
                                        null, mSearchPreferences);

                    }

                    //refresh adapter data
                    mDataAdapter.getAdapterData().clear();
                    mDataAdapter.addAdapterData(dataList);

                    loadDetailFragment();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError);

                    return;
                }

                //prepareNotification error
                Snackbar.make(getActivity().findViewById(R.id.top_layout),
                        getString(R.string.spotlight_fetch_error_message), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void retryAfterCoolDOwn(ANError anError) {
        Log.d(FRAGMENT_KEY, "retrying spotlight fetch");

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

    //loads detail fragment:
    //if tablet is landscape
    // && detail fragment container has no fragment
    // && is containing fragment on top in fragment detail container
    private void loadDetailFragment() {
        MasterActivity masterActivity = ((MasterActivity) getParentFragment().getActivity());

        if (masterActivity == null) return;

        Fragment activeFragment = masterActivity.getSupportFragmentManager()
                .findFragmentById(R.id.master_fragments_container);
        String activeClassName = activeFragment.getClass().getName();
        String parentClassName = getParentFragment().getClass().getName();

        boolean isParentActive = activeClassName.equals(parentClassName);

        if (masterActivity.isTabletLandscape()
                && !masterActivity.hasFragment(R.id.detail_fragments_container)
                //TODO consider detaching fragments to disable background updates instead of "isParentActive"//fixes
                //fixes bug where media data of bottom fragments get loaded into details container
                // instead of topmost fragment
                && isParentActive) {

            MediaData firstMediaData = mDataAdapter.getAdapterData().get(0);

            startDetailsFragment(firstMediaData);
        }
    }

    private void startDetailsFragment(MediaData selectedData) {
        ((MasterActivity) getActivity())
                .launchDetailsFragment(HomeSpotlightFragment.this, selectedData);
    }

    @Override
    public void onItemClick(int position) {
        MediaData selectedData = mDataAdapter.getAdapterData().get(position);

        startDetailsFragment(selectedData);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //update initialized search preferences
        getArguments().putParcelable(SEARCH_PREFERENCES_KEY, Parcels.wrap(mSearchPreferences));

        outState.putParcelable(ADAPTER_DATA_LIST_KEY, Parcels.wrap(mDataAdapter.getAdapterData()));
    }
}
