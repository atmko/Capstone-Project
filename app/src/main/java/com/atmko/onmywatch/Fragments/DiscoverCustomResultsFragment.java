/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

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
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.PersonData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.MovieDataParser;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
import com.atmko.onmywatch.utils.api_utils.PersonDataParser;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;
import com.atmko.onmywatch.utils.api_utils.SeriesDataParser;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.stack.Stack;
import com.google.android.material.snackbar.Snackbar;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_PEOPLE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;
import static com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker.REQUEST_COOL_DOWN;

public class DiscoverCustomResultsFragment extends Fragment implements
        MediaDataAdapter.OnListItemClickListener,
        AdapterView.OnItemSelectedListener {

    private static final String FRAGMENT_KEY = "discover_custom results_fragment";

    // the fragment initialization parameters
    private static final String SEARCH_TYPE_KEY = "search_type";
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String SEARCH_URL_KEY = "search_url";
    private static final String SEARCH_PREFERENCES_KEY = "search_preferences";

    private static final String IS_SEARCH_WINDOW_SHOWN_KEY = "is_search_window_shown";

    private String mSearchType;
    private int mMediaType;
    private String mSearchUrl;

    private static final String ADAPTER_DATA_LIST_KEY = "adapter_data_list";
    private static final String PAGING_BLOCK_MAP_KEY = "paging_block_map";
    private static final String SAVED_INDICES_KEY = "saved_indices";

    //check for restoring state
    private MediaDataAdapter mDataAdapter;
    private Stack mStack;
    private SearchPreferences mSearchPreferences;
    private SuperEditText mSearchTextView;

    private RecyclerView recyclerView;
    private ViewGroup customSearchLayout;
    private Button searchButton;
    private Map<Integer, Integer> selectionMap;
    private boolean mAvoidLoadingDetails;

    public DiscoverCustomResultsFragment() {
        // Required empty public constructor
    }

    public static DiscoverCustomResultsFragment newInstance(String searchType,
                                                            int mediaType,
                                                            String searchUrl,
                                                            SearchPreferences searchPreferencesParcel) {

        DiscoverCustomResultsFragment fragment = new DiscoverCustomResultsFragment();
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

        return inflater.inflate(R.layout.fragment_custom_discover_results, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();
        restoreCustomSpinnerValues(savedInstanceState);

        if (savedInstanceState != null) {
            boolean searchActive = savedInstanceState.getBoolean(IS_SEARCH_WINDOW_SHOWN_KEY);
            setSearchActive(searchActive);

            MasterActivity.restoreSearchIfAvailable(DiscoverCustomResultsFragment.this, savedInstanceState);

            //get saved adapter data list
            List<MediaData> mediaDataList = Parcels.unwrap(
                    savedInstanceState.getParcelable(ADAPTER_DATA_LIST_KEY));

            mDataAdapter.addAdapterData(mediaDataList);

            //get saved paging block map
            int[] pagingBlockRange = savedInstanceState.getIntArray(PAGING_BLOCK_MAP_KEY);
            if (pagingBlockRange != null) {
                mStack.restorePagingBlockStructure(pagingBlockRange, mediaDataList);
            }

            //set total pages
            mStack.setTotalPages(mSearchPreferences.getTotalPages());

            if (canLaunchDetailFragmentAlongside()) loadDetailFragmentIfCapable();
        }
    }

    private void defineViews() {
        if (getActivity() == null) return;
        if (getView() == null) return;
        if (getParentFragment() == null) return;
        if (getParentFragment().getView() == null) return;

        configureCustomSpinners();

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

        recyclerView = getView().findViewById(R.id.results_recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());

        mDataAdapter = new MediaDataAdapter(this, getActivity().getApplicationContext(),
                CustomParams.getSearchParams(this));

        Object preloadObject = null;
        if (mMediaType == MEDIA_TYPE_MOVIE) {
            preloadObject = new MovieData();

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            preloadObject = new SeriesData();


        } else if (mMediaType == MEDIA_TYPE_PEOPLE) {
            preloadObject = new PersonData();

        }

        recyclerView.setAdapter(mDataAdapter);
        mStack = new Stack(false,
                getResources().getInteger(R.integer.stack_block_limit), pagingBlockTemplate,
                preloadObject, recyclerView, mDataAdapter, true);
        recyclerView.addOnScrollListener(mStack);

        //get search bar from parent fragment
        mSearchTextView =
                getParentFragment().getView().findViewById(R.id.search_edit_text_view);
    }

    private void configureCustomSpinners() {
        if (getView() == null) return;
        if (getContext() == null) return;

        searchButton = getView().findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getContext() == null) return;
                //reset recycler view position on each search
                if (mDataAdapter.getItemCount() > 0) {
                    recyclerView.scrollToPosition(0);
                }

                Integer genre1Integer = selectionMap.get(R.id.genre1_search_item);
                int genre1Index = genre1Integer != null ? genre1Integer : 0;
                Integer genre2Integer = selectionMap.get(R.id.genre2_search_item);
                int genre2Index = genre2Integer != null ? genre2Integer : 0;

                int[] genreIndices = {genre1Index, genre2Index};
                mSearchPreferences.setGenres(getContext(), genreIndices);

                if (mMediaType == MEDIA_TYPE_SERIES) {
                    Integer networkInteger = selectionMap.get(R.id.network_search_item);
                    int networkIndex = networkInteger != null ? networkInteger : 0;

                    int[] indices = {networkIndex};
                    mSearchPreferences.setNetworks(getContext(), indices);
                }

                Integer sortInteger = selectionMap.get(R.id.sort_by_search_item);
                int sortByIndex = sortInteger != null ? sortInteger : 0;

                mSearchPreferences.setSortBy(getContext(), sortByIndex);

                activateSearch();

                loadSearch();
            }
        });

        String[] titleValues;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            customSearchLayout = getView().findViewById(R.id.items_custom_movie_search);
            customSearchLayout.setVisibility(View.VISIBLE);
            getView().findViewById(R.id.items_custom_series_search).setVisibility(View.GONE);

            titleValues = getResources().getStringArray(R.array.custom_movie_search_titles);

        } else {
            customSearchLayout = getView().findViewById(R.id.items_custom_series_search);
            customSearchLayout.setVisibility(View.VISIBLE);
            getView().findViewById(R.id.items_custom_movie_search).setVisibility(View.GONE);

            titleValues = getResources().getStringArray(R.array.custom_series_search_titles);
        }

        for (int i = 0; i < customSearchLayout.getChildCount(); i++) {
            View customSearchItem = customSearchLayout.findViewById(customSearchLayout.getChildAt(i).getId());

            TextView titleTextView = customSearchItem.findViewById(R.id.title_text_view);
            titleTextView.setText(titleValues[i]);

            Spinner spinner = customSearchItem.findViewById(R.id.spinner);

            int spinnerValues;
            if (customSearchItem.getId() == R.id.genre1_search_item
                    || customSearchItem.getId() == R.id.genre2_search_item) {
                spinnerValues = R.array.genre_id_value;

            } else if (customSearchItem.getId() == R.id.network_search_item) {
                spinnerValues = R.array.network_values;

            } else if (customSearchItem.getId() == R.id.sort_by_search_item) {
                spinnerValues = R.array.sort_values;
            } else {
                spinnerValues = 0;
            }

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                    spinnerValues, android.R.layout.simple_spinner_item);
            spinner.setAdapter(adapter);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }
    }

    private void restoreCustomSpinnerValues(Bundle savedInstanceState) {
        selectionMap = new HashMap<>();

        if (savedInstanceState == null) {
            for (int i = 0; i < customSearchLayout.getChildCount(); i++) {
                View customSearchItem =
                        customSearchLayout.findViewById(customSearchLayout.getChildAt(i).getId());
                selectionMap.put(customSearchItem.getId(), 0);
            }
        } else {
            ArrayList<Integer> savedIndices = savedInstanceState.getIntegerArrayList(SAVED_INDICES_KEY);
            if (savedIndices != null) {
                for (int i = 0; i < customSearchLayout.getChildCount(); i++) {
                    View customSearchItem =
                            customSearchLayout.findViewById(customSearchLayout.getChildAt(i).getId());
                    selectionMap.put(customSearchItem.getId(), savedIndices.get(i));
                }
            }
        }

        for (final int key : selectionMap.keySet()) {
            final Spinner spinner = customSearchLayout.findViewById(key).findViewById(R.id.spinner);
            spinner.post(new Runnable() {
                public void run() {
                    spinner.setOnItemSelectedListener(DiscoverCustomResultsFragment.this);

                    Integer indexObj = selectionMap.get(key);
                    int index = indexObj != null ? indexObj : 0;

                    spinner.setSelection(index);
                }
            });
        }
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
        if (getParentFragment() == null) return;
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
                            PersonDataParser.parseData(returnedJSONString, mStack, mSearchPreferences
                            );
                }

                mStack.stackPage(blockNumber, targetPage, dataList, stackOperation);

                if (canLaunchDetailFragmentAlongside()) loadDetailFragmentIfCapable();
            }

            @Override
            public void onError(ANError anError) {
                if (getActivity() == null) return;
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

    @SuppressLint("LongLogTag")
    private void retryAfterCoolDOwn(ANError anError, final int blockNumber, final int targetPage,
                                    final int stackOperation) {
        Log.d(FRAGMENT_KEY, "retry fetching search results");

        String coolDownString = anError.getResponse().header(ApiConstants.RETRY_AFTER_KEY);
        int coolDown = coolDownString != null ? Integer.parseInt(coolDownString) : 0;
        int coolDownInMilliSecs = coolDown * MILLISECOND_CONVERSION;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                executeSearch(blockNumber, targetPage, stackOperation);

            }
        }, coolDownInMilliSecs);
    }

    private boolean canLaunchDetailFragmentAlongside() {
        if (mAvoidLoadingDetails) return false;
        if (getActivity() == null) return false;
        boolean isTabletLandscape = ((MasterActivity) getActivity()).isTabletLandscape();
        boolean isAdapterEmpty = mDataAdapter.getAdapterData().size() > 0;

        return isAdapterEmpty && isTabletLandscape;
    }

    //loads detail fragment:
    //if tablet is landscape
    // && detail fragment container has no fragment
    //&& this is currently selected tab
    // && stack is not currently waiting for more pages to load
    // && is containing fragment on top in fragment detail container
    private void loadDetailFragmentIfCapable() {
        if (getParentFragment() == null) return;
        MasterActivity masterActivity = ((MasterActivity) getParentFragment().getActivity());

        if (masterActivity == null) return;
        Fragment activeFragment =
                masterActivity.getSupportFragmentManager().findFragmentById(R.id.master_fragments_container);

        if (activeFragment == null) return;
        String activeClassName = activeFragment.getClass().getName();
        String parentClassName = getParentFragment().getClass().getName();

        boolean isParentActive = activeClassName.equals(parentClassName);
        if (!masterActivity.hasFragment(R.id.detail_fragments_container)
                && isCurrentTab()
                && mStack.isIdle()
                //TODO consider detaching fragments to disable background updates instead of "isParentActive"
                && isParentActive) {

            MediaData firstMediaData = mDataAdapter.getAdapterData().get(0);
            masterActivity.launchDetailsFragment(firstMediaData, null);
        }
    }

    //uses the saved selected tab position to get the corresponding url
    //if the corresponding url = this fragment's url(mSearchUrl), returns true
    //compares the url list of each tab to checks if this fragment is the currently selected tab
    public boolean isCurrentTab() {
        if (getParentFragment() == null || getParentFragment().getActivity() == null) return false;
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

    public boolean isSearchActive() {
        return recyclerView.getVisibility() == View.VISIBLE;
    }

    public void setSearchActive(boolean searchActive) {
        if (searchActive) {
            activateSearch();

        } else {
            deactivateSearch();
        }
    }

    private void activateSearch() {
        searchButton.setVisibility(View.GONE);
        customSearchLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void deactivateSearch() {
        searchButton.setVisibility(View.VISIBLE);
        customSearchLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(int position) {
        if (getActivity() == null) return;

        MediaData mediaData = mDataAdapter.getAdapterData().get(position);
        //do nothing if selecting placeholder
        if (mediaData == null || mediaData.getId() == null || mediaData.getId().equals("")) return;
        ((MasterActivity) getActivity()).launchDetailsFragment(mediaData, null);
    }

    @Override
    public void onAddButtonClick(final int position) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    MediaData selectedData = mDataAdapter.getAdapterData().get(position);
                    if (selectedData != null && selectedData.getId() != null && !selectedData.getId().equals("")) {
                        ((MasterActivity) getActivity())
                                .launchAddToListActivity(mDataAdapter.getAdapterData().get(position));
                    }
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mAvoidLoadingDetails = true;

        //update initialized search preferences
        if (getArguments() != null) {
            getArguments().putParcelable(SEARCH_PREFERENCES_KEY, Parcels.wrap(mSearchPreferences));
        }

        outState.putParcelable(ADAPTER_DATA_LIST_KEY, Parcels.wrap(mDataAdapter.getAdapterData()));

        outState.putIntArray(PAGING_BLOCK_MAP_KEY, mStack.saveBlockStructure());

        //save search bar text
        outState.putString(MasterActivity.SEARCH_TEXT_KEY, mSearchTextView.getText().toString());

        //save search bar visibility
        outState.putInt(MasterActivity.SEARCH_BAR_VISIBILITY_KEY, mSearchTextView.getVisibility());

        outState.putBoolean(IS_SEARCH_WINDOW_SHOWN_KEY, isSearchActive());

        //save spinner indices
        ArrayList<Integer> savedIndices = new ArrayList<>();
        for (int i = 0; i < customSearchLayout.getChildCount(); i++) {
            View customSearchItem =
                    customSearchLayout.findViewById(customSearchLayout.getChildAt(i).getId());
            Spinner spinner = customSearchItem.findViewById(R.id.spinner);
            savedIndices.add(spinner.getSelectedItemPosition());
        }
        outState.putIntegerArrayList(SAVED_INDICES_KEY, savedIndices);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int newIndex, long l) {
        int spinnerLayoutId = ((ViewGroup) adapterView.getParent()).getId();
        Integer oldIndexInteger = selectionMap.get(spinnerLayoutId);
        int oldIndex = oldIndexInteger != null ? oldIndexInteger : 0;
        if (oldIndex != newIndex) selectionMap.put(spinnerLayoutId, newIndex);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
