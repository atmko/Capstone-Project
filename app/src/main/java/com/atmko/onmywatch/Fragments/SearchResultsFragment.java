package com.atmko.onmywatch.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.view_models.SearchViewModel;
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

public class SearchResultsFragment extends Fragment implements
        MediaDataAdapter.OnListItemClickListener,
        PeopleDataAdapter.OnListItemClickListener{

    public static String FRAGMENT_KEY = "search_results_fragment";

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

    private RecyclerView.Adapter mDataAdapter;
    private Stack stack;
    private SearchPreferences mSearchPreferences;

    private SearchViewModel searchViewModel;

    public SearchResultsFragment() {
        // Required empty public constructor
    }

    public static SearchResultsFragment newInstance(String searchType, int mediaType, String searchUrl,
                                                    SearchPreferences searchPreferencesParcel) {

        SearchResultsFragment fragment = new SearchResultsFragment();
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

        return inflater.inflate(R.layout.fragment_search_results, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        searchViewModel = ViewModelProviders.of(getParentFragment().getActivity()).get(SearchViewModel.class);

        defineViews();

        if (savedInstanceState == null) {
            loadSearch();

        } else {
            if (mMediaType == MEDIA_TYPE_PEOPLE) {
                //get saved adapter data list
                List<PersonData> mediaDataList = Parcels.unwrap(
                        savedInstanceState.getParcelable(ADAPTER_DATA_LIST_KEY));

                ((PeopleDataAdapter) mDataAdapter).addAdapterData(mediaDataList);

            } else {
                //get saved adapter data list
                List<MediaData> mediaDataList = Parcels.unwrap(
                        savedInstanceState.getParcelable(ADAPTER_DATA_LIST_KEY));

                ((MediaDataAdapter) mDataAdapter).addAdapterData(mediaDataList);
            }

            //get saved paging block map
            int[] pagingBlockRange = savedInstanceState.getIntArray(PAGING_BLOCK_MAP_KEY);
            stack.restorePagingBlockStructure(pagingBlockRange);

            //set total pages
            stack.setTotalPages(mSearchPreferences.getTotalPages());

            loadDetailFragment();
        }
    }

    private void defineViews() {
        Stack.PagingBlockTemplate pagingBlockTemplate = new Stack.PagingBlockTemplate(new Stack.PagingBlockTemplate.OnCreatePageLoader() {
            @Override
            public void onPageEndReached(int blockNumber, int targetPage) {
                mSearchPreferences.setTargetPage(targetPage);
                executeSearch(blockNumber, targetPage, Stack.GO_DOWN_ONE_BLOCK);
            }

            @Override
            public void onPageStartReached(int blockNumber, int targetPage) {
                mSearchPreferences.setTargetPage(targetPage);
                executeSearch(blockNumber, targetPage, Stack.GO_UP_ONE_BLOCK);
            }
        }, ApiConstants.RESULTS_PER_PAGE, getResources().getInteger(R.integer.stack_pages_per_block));

        RecyclerView recyclerView = getView().findViewById(R.id.search_results_recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());

        if (mMediaType == MEDIA_TYPE_PEOPLE) {
            mDataAdapter = new PeopleDataAdapter(this);

        } else {
            mDataAdapter = new MediaDataAdapter(this);
        }

        recyclerView.setAdapter(mDataAdapter);
        stack = new Stack(false,getResources().getInteger(R.integer.stack_block_limit),
                pagingBlockTemplate, recyclerView, mDataAdapter);
        recyclerView.addOnScrollListener(stack);
    }

    private void loadSearch() {
        stack.initialize();
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.search_column_span));

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void executeSearch(final int blockNumber, final int targetPage, final int stackOperation) {
        //build AN request
        ANRequest request = NetworkFunctions.agnosticSearchRequest(mSearchUrl, mSearchPreferences,
                getParentFragment().getActivity());

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String returnedJSONString) {
                try {
                    //parse and populate retrieved data

                    List dataList = null;

                    if (mMediaType == MEDIA_TYPE_MOVIE) {
                        dataList =
                                MovieDataParser.parseData(returnedJSONString, stack, mSearchPreferences);

                    } else if (mMediaType == MEDIA_TYPE_SERIES) {
                        dataList =
                                SeriesDataParser.parseData(returnedJSONString, stack, mSearchPreferences);

                    } else if (mMediaType == MEDIA_TYPE_PEOPLE) {
                        dataList =
                                PersonDataParser.parseData(returnedJSONString, stack, mSearchPreferences);

                    }

                    stack.stackPage(blockNumber, targetPage, dataList, stackOperation);

                    loadDetailFragment();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ANError anError) {
                //prepareNotification error
                Snackbar.make(getActivity().findViewById(R.id.top_layout),
                        getString(R.string.search_results_fetch_error_message),
                        Snackbar.LENGTH_LONG).show();

            }
        });
    }

    //loads detail fragment:
    //if tablet is landscape
    // && detail fragment container has no fragment
    //&& this is currently selected tab
    // && stack is not currently waiting for more pages to load
    // && is containing fragment on top in fragment detail container
    private void loadDetailFragment() {
        MasterActivity masterActivity = ((MasterActivity) getParentFragment().getActivity());

        Fragment activeFragment = masterActivity.getSupportFragmentManager().findFragmentById(R.id.master_fragments_container);
        String activeClassName = activeFragment.getClass().getName();
        String parentClassName = getParentFragment().getClass().getName();

        boolean isParentActive = activeClassName.equals(parentClassName);

        if (masterActivity.isTabletLandscape()
                && !masterActivity.hasFragment(R.id.detail_fragments_container)
                && isCurrentTab()
                && stack.isIdle()
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
        if (mSearchType.equals(SearchParentFragment.SEARCH_MODE_MANUAL)) {
            //get the list of manual search urls and compare to this fragment's mSearchUrl
            String[] manualUrls =
                    getParentFragment().getActivity().getResources()
                            .getStringArray(R.array.manual_search_urls);

            List<String> manualUrlList = Arrays.asList(manualUrls);

            return manualUrlList.indexOf(mSearchUrl) == searchViewModel.getCurrentTabPosition();

        //if this is a preset search
        } else {
            //get the list of preset search urls and compare it to the this fragment's mSearchUrl
            String[] presetUrls;

            if (mMediaType == MEDIA_TYPE_MOVIE) {
                presetUrls =
                        getParentFragment().getActivity().getResources()
                                .getStringArray(R.array.preset_movie_search_urls);

            } else if (mMediaType == MEDIA_TYPE_SERIES) {
                presetUrls =
                        getParentFragment().getActivity().getResources()
                                .getStringArray(R.array.preset_series_search_urls);

            } else {
                throw new Error("no specified media type");

            }

            List<String> presetUrlList = Arrays.asList(presetUrls);

            return presetUrlList.indexOf(mSearchUrl) == searchViewModel.getCurrentTabPosition();
        }
    }

    @Override
    public void onItemClick(int position) {
        MediaData selectedData = ((MediaDataAdapter) mDataAdapter).getAdapterData().get(position);

        startDetailsFragment(selectedData);
    }

    private void startDetailsFragment(MediaData selectedData) {
        String[] detailUrls = getContext().getResources().getStringArray(R.array.details_urls);
        String detailUrl = null;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            detailUrl = detailUrls[MEDIA_TYPE_SERIES];
        }

        Parcelable parceledData = Parcels.wrap(selectedData);
        Parcelable parceledSharedPreferences = Parcels.wrap(mSearchPreferences);

        DetailsFragment detailsFragment =
                DetailsFragment.newInstance(mMediaType, detailUrl, parceledData, parceledSharedPreferences);

        Fragment detailContainerFragment =
                getParentFragment().getActivity().getSupportFragmentManager().
                        findFragmentById(R.id.detail_fragments_container);

        ///remove existing fragment
        if (detailContainerFragment != null) {
            getParentFragment().getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                    .remove(detailContainerFragment).commit();

        }

        //launch search fragment
        getParentFragment().getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                .add(R.id.detail_fragments_container, detailsFragment, DetailsFragment.FRAGMENT_KEY)
                .commit();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //update initialized search preferences
        getArguments().putParcelable(SEARCH_PREFERENCES_KEY, Parcels.wrap(mSearchPreferences));

        if (mMediaType == MEDIA_TYPE_PEOPLE) {
            outState.putParcelable(ADAPTER_DATA_LIST_KEY, Parcels.wrap(((PeopleDataAdapter)mDataAdapter).getAdapterData()));

        } else {
            outState.putParcelable(ADAPTER_DATA_LIST_KEY, Parcels.wrap(((MediaDataAdapter)mDataAdapter).getAdapterData()));
        }

        outState.putIntArray(PAGING_BLOCK_MAP_KEY, stack.saveBlockStructure());
    }
}
