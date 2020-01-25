/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.CustomParams;
import com.atmko.onmywatch.adapters.MediaDataAdapter;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;
import com.atmko.onmywatch.view_models.ListResultsViewModelFactory;
import com.atmko.onmywatch.view_models.ListsResultsViewModel;

import java.util.Arrays;
import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;

public class ListResultsFragment extends Fragment
        implements MediaDataAdapter.OnListItemClickListener{
    public static final String FRAGMENT_KEY = "list_results_fragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String LIST_TYPE_KEY = "list_type";
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String LIST_NAME_KEY = "list_name";

    // TODO: Rename and change types of parameters
    //check for restoring state
    private boolean mFirstInit = true;
    private int mListType;
    private int mMediaType;
    private String mListName;

    private MediaDataAdapter mDataAdapter;
    private SearchPreferences mSearchPreferences;
    private SuperEditText mSearchTextView;

    public ListResultsFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static ListResultsFragment newInstance(int listType, int mediaType, String listName) {
        ListResultsFragment fragment = new ListResultsFragment();
        Bundle args = new Bundle();
        args.putInt(LIST_TYPE_KEY, listType);
        args.putInt(MEDIA_TYPE_KEY, mediaType);
        args.putString(LIST_NAME_KEY, listName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mListType = getArguments().getInt(LIST_TYPE_KEY);
            mMediaType = getArguments().getInt(MEDIA_TYPE_KEY);
            mListName = getArguments().getString(LIST_NAME_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_results, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();

        //state restoration done after ViewModel onChanged method
        observeData(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //save search bar text
        outState.putString(MasterActivity.SEARCH_TEXT_KEY, mSearchTextView.getText().toString());

        //save search bar visibility
        outState.putInt(MasterActivity.SEARCH_BAR_VISIBILITY_KEY, mSearchTextView.getVisibility());
    }

    private void defineViews() {
        RecyclerView mListResultsRecyclerView =
                getView().findViewById(R.id.list_results_recycler_view);

        mListResultsRecyclerView.setLayoutManager(configureLayoutManager());

        mDataAdapter = new MediaDataAdapter(this, getActivity().getApplicationContext(),
                CustomParams.getSearchParams(this));
        mListResultsRecyclerView.setAdapter(mDataAdapter);
        mSearchPreferences = new SearchPreferences();

        //get search bar from parent fragment
        mSearchTextView =
                getParentFragment().getView().findViewById(R.id.search_edit_text_view);
        //configure search bar
        mSearchTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //TODO: implement search for pro mode
                if (!MasterActivity.isProMode()) {
                    onSearchTextChanged(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void observeData(final Bundle savedInstanceState) {
        AppDatabase database = AppDatabase.getInstance(getContext());

        final String[] watchStatusMoviesTitles = getContext().getResources()
                .getStringArray(R.array.watch_status_movie_titles);
        List<String> titleList = Arrays.asList(watchStatusMoviesTitles);

        ListResultsViewModelFactory resultsViewModelFactory =
                new ListResultsViewModelFactory(database, mListType, mMediaType, titleList, mListName);

        final ListsResultsViewModel viewModel = ViewModelProviders.of(this, resultsViewModelFactory)
                .get(ListsResultsViewModel.class);
        LiveData<List<MovieData>> movieDataWatchListLiveData = viewModel.getAllMoviesInWatchList();
        LiveData<List<SeriesData>> seriesDataWatchListLiveData = viewModel.getAllSeriesInWatchList();
        LiveData<List<MovieData>>  movieDataUserListLiveData = viewModel.getAllMoviesInUserList();
        LiveData<List<SeriesData>> seriesDataUserListLiveData = viewModel.getAllSeriesInUserList();

        //if this is a watch list
        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                movieDataWatchListLiveData.observe(getParentFragment(),
                        new Observer<List<MovieData>>() {
                            @Override
                            public void onChanged(List<MovieData> mediaDataList) {
                                populateAndNotifyAdapter(mediaDataList);

                                //TODO: implement search for pro mode
                                if (!MasterActivity.isProMode()) {
                                    //restore search if it exists
                                    final ImageButton searchImageButton = getParentFragment().
                                            getView().findViewById(R.id.search_image_button);
                                    MasterActivity masterActivity = ((MasterActivity) getActivity());
                                    masterActivity.restoreSavedSearch(ListResultsFragment.this,
                                            mFirstInit, savedInstanceState, searchImageButton, mSearchTextView);

                                    mFirstInit = false;
                                }
                            }
                        });

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                seriesDataWatchListLiveData.observe(this, new Observer<List<SeriesData>>() {
                    @Override
                    public void onChanged(List<SeriesData> mediaDataList) {
                        populateAndNotifyAdapter(mediaDataList);

                        //TODO: implement search for pro mode
                        if (!MasterActivity.isProMode()) {
                            //restore search if it exists
                            final ImageButton searchImageButton = getParentFragment().
                                    getView().findViewById(R.id.search_image_button);
                            MasterActivity masterActivity = ((MasterActivity) getActivity());
                            masterActivity.restoreSavedSearch(ListResultsFragment.this,
                                    mFirstInit, savedInstanceState, searchImageButton, mSearchTextView);

                            mFirstInit = false;
                        }
                    }
                });
            }
        }

        //if this is a user list
        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_USER) {
            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                movieDataUserListLiveData.observe(this, new Observer<List<MovieData>>() {
                    @Override
                    public void onChanged(List<MovieData> mediaDataList) {
                        populateAndNotifyAdapter(mediaDataList);

                        //TODO: implement search for pro mode
                        if (!MasterActivity.isProMode()) {
                            //restore search if it exists
                            final ImageButton searchImageButton = getParentFragment().
                                    getView().findViewById(R.id.search_image_button);
                            MasterActivity masterActivity = ((MasterActivity) getActivity());
                            masterActivity.restoreSavedSearch(ListResultsFragment.this,
                                    mFirstInit, savedInstanceState, searchImageButton, mSearchTextView);

                            mFirstInit = false;
                        }
                    }
                });

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                seriesDataUserListLiveData.observe(this, new Observer<List<SeriesData>>() {
                    @Override
                    public void onChanged(List<SeriesData> mediaDataList) {
                        populateAndNotifyAdapter(mediaDataList);

                        //TODO: implement search for pro mode
                        if (!MasterActivity.isProMode()) {
                            //restore search if it exists
                            final ImageButton searchImageButton = getParentFragment().
                                    getView().findViewById(R.id.search_image_button);
                            MasterActivity masterActivity = ((MasterActivity) getActivity());
                            masterActivity.restoreSavedSearch(ListResultsFragment.this,
                                    mFirstInit, savedInstanceState, searchImageButton, mSearchTextView);

                            mFirstInit = false;
                        }
                    }
                });
            }
        }
    }

    private void onSearchTextChanged(CharSequence searchText) {
        final String[] watchStatusMoviesTitles = getContext().getResources()
                .getStringArray(R.array.watch_status_movie_titles);
        List<String> titleList = Arrays.asList(watchStatusMoviesTitles);

        final AppDatabase database = AppDatabase.getInstance(getContext());

        String mediaTitle = searchText.toString();
        mediaTitle = "%" + mediaTitle + "%";

        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                final LiveData<List<MovieData>> listLiveData = database.movieDataDao()
                        .getMoviesByWatchStatusLike(titleList.indexOf(mListName), mediaTitle);

                listLiveData.observe(getParentFragment(), new Observer<List<MovieData>>() {
                    @Override
                    public void onChanged(List<MovieData> movieDataList) {
                        listLiveData.removeObserver(this);
                        populateAndNotifyAdapter(movieDataList);
                    }
                });

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                final LiveData<List<SeriesData>> listLiveData = database.seriesDataDao()
                        .getSeriesByWatchStatusLike(titleList.indexOf(mListName), mediaTitle);

                listLiveData.observe(getParentFragment(), new Observer<List<SeriesData>>() {
                    @Override
                    public void onChanged(List<SeriesData> seriesDataList) {
                        listLiveData.removeObserver(this);
                        populateAndNotifyAdapter(seriesDataList);
                    }
                });
            }
        }

        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_USER) {
            if (mMediaType == MEDIA_TYPE_MOVIE) {
                //observe lists with searched name then remove observer
                final LiveData<List<MovieData>> listLiveData = database.movieDataRecordsDao()
                        .getMoviesWithNameLike(mListName, mediaTitle);
                listLiveData.observe(getParentFragment(),
                        new Observer<List<MovieData>>() {
                            @Override
                            public void onChanged(List<MovieData> movieDataList) {
                                listLiveData.removeObserver(this);
                                populateAndNotifyAdapter(movieDataList);
                            }
                        });

            } else {
                //observe lists with searched name then remove observer
                final LiveData<List<SeriesData>> listLiveData = database.seriesDataRecordsDao()
                        .getSeriesWithNameLike(mListName, mediaTitle);
                listLiveData.observe(getParentFragment(),
                        new Observer<List<SeriesData>>() {
                            @Override
                            public void onChanged(List<SeriesData> seriesDataList) {
                                listLiveData.removeObserver(this);
                                populateAndNotifyAdapter(seriesDataList);
                            }
                        });
            }
        }
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.search_column_span));

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void populateAndNotifyAdapter(List mediaDataList) {
        if (mediaDataList.size() == 0) {
            mDataAdapter.setInPlaceholderMode(true);

        } else {
            mDataAdapter.setInPlaceholderMode(false);
            mDataAdapter.getAdapterData().clear();
            mDataAdapter.addAdapterData(mediaDataList);

            if (mDataAdapter.getAdapterData().size() > 0 ) loadDetailFragment();
        }
    }

    //loads detail fragment:
    //if tablet is landscape
    // && detail fragment container has no fragment
    //&& this is currently selected tab
    // && is containing fragment on top in fragment detail container
    private void loadDetailFragment() {
        MasterActivity masterActivity = ((MasterActivity) getParentFragment().getActivity());

        boolean isCurrentTab =
                mMediaType == ((ListResultsParentFragment) getParentFragment()).getCurrentTabPosition();

        Fragment activeFragment = masterActivity.getSupportFragmentManager()
                .findFragmentById(R.id.master_fragments_container);
        String activeClassName = activeFragment.getClass().getName();
        String parentClassName = getParentFragment().getClass().getName();

        boolean isParentActive = activeClassName.equals(parentClassName);

        if (masterActivity.isTabletLandscape()
                && !masterActivity.hasFragment(R.id.detail_fragments_container)
                && isCurrentTab
                //TODO consider detaching fragments to disable background updates instead of "isParentActive"
                && isParentActive) {

            MediaData firstMediaData = mDataAdapter.getAdapterData().get(0);

            startDetailsFragment(firstMediaData);
        }
    }

    @Override
    public void onItemClick(int position) {
        if (mDataAdapter.inPlaceholderMode()) {
            DiscoverParentFragment discoverParentFragment = DiscoverParentFragment.newInstance();

            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_down_entry, android.R.animator.fade_out)
                    .add(R.id.master_fragments_container, discoverParentFragment,
                            DiscoverParentFragment.FRAGMENT_KEY)
                    .commit();
            return;
        }

        MediaData selectedData = mDataAdapter.getAdapterData().get(position);

        startDetailsFragment(selectedData);
    }

    private void startDetailsFragment(MediaData selectedData) {
        ((MasterActivity) getActivity()).launchDetailsFragment(selectedData, null);
    }
}