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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidnetworking.core.MainThreadExecutor;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.CustomParams;
import com.atmko.onmywatch.adapters.MediaDataAdapter;
import com.atmko.onmywatch.adapters.TagAdapter;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.view_models.ListResultsViewModelFactory;
import com.atmko.onmywatch.view_models.ListsResultsViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.atmko.onmywatch.MasterActivity.SEARCH_TEXT_KEY;

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
    private int mListType;
    private int mMediaType;
    private String mListName;

    private AppDatabase database;
    private MediaDataAdapter mDataAdapter;
    private SearchPreferences mSearchPreferences;
    private SuperEditText mSearchTextView;
    private TagAdapter tagAdapter;

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
        return inflater.inflate(R.layout.fragment_recycler_results, container, false);
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
        outState.putString(SEARCH_TEXT_KEY, mSearchTextView.getText().toString());

        //save search bar visibility
        outState.putInt(MasterActivity.SEARCH_BAR_VISIBILITY_KEY, mSearchTextView.getVisibility());
    }

    private void defineViews() {
        if (getActivity() == null) return;
        if (getView() == null) return;
        if (getParentFragment() == null) return;
        if (getParentFragment().getView() == null) return;

        RecyclerView mListResultsRecyclerView =
                getView().findViewById(R.id.results_recycler_view);

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
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        onSearchTextChanged();
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        tagAdapter = new TagAdapter(
                getParentFragment().getContext(),
                R.layout.fragment_list_results_parent,
                R.id.search_edit_text_view,
                new ArrayList<String>()
        );

        mSearchTextView.setAdapter(tagAdapter);
        mSearchTextView.setThreshold(1);
    }

    private void observeData(final Bundle savedInstanceState) {
        if (getParentFragment() == null) return;

        database = AppDatabase.getInstance(getContext());

        final String[] watchStatusMoviesTitles = getResources()
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
                movieDataWatchListLiveData.observe(getParentFragment(), new Observer<List<MovieData>>() {
                    @Override
                    public void onChanged(List<MovieData> mediaDataList) {
                        populateAndNotifyAdapter(mediaDataList);

                        //restore search if it exists
                        MasterActivity.restoreSearchIfAvailable(ListResultsFragment.this, savedInstanceState);
                    }
                });

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                seriesDataWatchListLiveData.observe(getParentFragment(), new Observer<List<SeriesData>>() {
                    @Override
                    public void onChanged(List<SeriesData> mediaDataList) {
                        populateAndNotifyAdapter(mediaDataList);

                        //restore search if it exists
                        MasterActivity.restoreSearchIfAvailable(ListResultsFragment.this, savedInstanceState);
                    }
                });
            }
        }

        //if this is a user list
        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_USER) {
            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                movieDataUserListLiveData.observe(getParentFragment(), new Observer<List<MovieData>>() {
                    @Override
                    public void onChanged(List<MovieData> mediaDataList) {
                        populateAndNotifyAdapter(mediaDataList);

                        //restore search if it exists
                        MasterActivity.restoreSearchIfAvailable(ListResultsFragment.this, savedInstanceState);
                    }
                });

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                seriesDataUserListLiveData.observe(getParentFragment(),
                        new Observer<List<SeriesData>>() {
                            @Override
                            public void onChanged(List<SeriesData> mediaDataList) {
                                populateAndNotifyAdapter(mediaDataList);

                        //restore search if it exists
                        MasterActivity.restoreSearchIfAvailable(ListResultsFragment.this, savedInstanceState);
                    }
                });
            }
        }
    }

    private static final int TAG_COUNT_LIMIT = 7;
    private void onSearchTextChanged() {
        if (getContext() == null) return;

        //define list id
        final Object listId;

        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            final String[] watchStatusMoviesTitles = getContext().getResources()
                    .getStringArray(R.array.watch_status_movie_titles);
            listId = Arrays.asList(watchStatusMoviesTitles).indexOf(mListName);

        } else {
            listId = mListName;
        }

        //get tag from db like text currently touching cursor
        String activeText = mSearchTextView.getActiveText();
        final List<String> searchTags = AppDatabase.getLocalDatabase(getContext()).searchMediaTagsDao()
                .getTagsLikeAlt(activeText);

        new MainThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                tagAdapter.clear();
                tagAdapter.addAll(searchTags);
                tagAdapter.notifyDataSetChanged();

                performFullSearchWithTags(listId);
            }
        });
    }

    private void performFullSearchWithTags(Object listId) {
        String searchBoxStrings = mSearchTextView.getText().toString();
        String[] terms = searchBoxStrings.split(" ");
        final List<String> formattedTags = new ArrayList<>();

        for (int i = 0; i < TAG_COUNT_LIMIT; i++) {
            if (!(i > terms.length - 1)) {
                formattedTags.add(terms[i]);

            } else {
                formattedTags.add("");
            }
        }

        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            searchInWatchList(formattedTags, ((int) listId));

        } else {
            searchInUserList(formattedTags, ((String) listId));
        }
    }

    private void searchInWatchList(List<String> formattedTags, int listId) {
        if (getParentFragment() == null) return;

        //if media data is movie
        if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            final LiveData<List<MovieData>> listLiveData = database.movieDataDao()
                    .getAllMediaWithWatchStatusAndTags(listId, formattedTags.get(0),
                            formattedTags.get(1), formattedTags.get(2), formattedTags.get(3),
                            formattedTags.get(4), formattedTags.get(5), formattedTags.get(6));

            listLiveData.observe(getParentFragment(), new Observer<List<MovieData>>() {
                @Override
                public void onChanged(List<MovieData> seriesDataList) {
                    listLiveData.removeObserver(this);
                    populateAndNotifyAdapter(seriesDataList);
                }
            });
        } else {
            final LiveData<List<SeriesData>> listLiveData = database.seriesDataDao()
                    .getAllMediaWithWatchStatusAndTags(listId, formattedTags.get(0),
                            formattedTags.get(1), formattedTags.get(2), formattedTags.get(3),
                            formattedTags.get(4), formattedTags.get(5), formattedTags.get(6));

            listLiveData.observe(getParentFragment(), new Observer<List<SeriesData>>() {
                @Override
                public void onChanged(List<SeriesData> seriesDataList) {
                    listLiveData.removeObserver(this);
                    populateAndNotifyAdapter(seriesDataList);
                }
            });
        }
    }

    private void searchInUserList(List<String> formattedTags, String listId) {
        if (getParentFragment() == null) return;

        //if media data is movie
        if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            final LiveData<List<MovieData>> listLiveData = database.movieDataRecordsDao()
                    .getMediaInListLike(listId, formattedTags.get(0),
                            formattedTags.get(1), formattedTags.get(2), formattedTags.get(3),
                            formattedTags.get(4), formattedTags.get(5), formattedTags.get(6));

            listLiveData.observe(getParentFragment(), new Observer<List<MovieData>>() {
                @Override
                public void onChanged(List<MovieData> seriesDataList) {
                    listLiveData.removeObserver(this);
                    populateAndNotifyAdapter(seriesDataList);
                }
            });
        } else {
            final LiveData<List<SeriesData>> listLiveData = database.seriesDataRecordsDao()
                    .getMediaInListLike(listId, formattedTags.get(0),
                            formattedTags.get(1), formattedTags.get(2), formattedTags.get(3),
                            formattedTags.get(4), formattedTags.get(5), formattedTags.get(6));

            listLiveData.observe(getParentFragment(), new Observer<List<SeriesData>>() {
                @Override
                public void onChanged(List<SeriesData> seriesDataList) {
                    listLiveData.removeObserver(this);
                    populateAndNotifyAdapter(seriesDataList);
                }
            });
        }
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.search_column_span));

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void populateAndNotifyAdapter(List mediaDataList) {
        mDataAdapter.getAdapterData().clear();
        mDataAdapter.addAdapterData(mediaDataList);
        mDataAdapter.setPlaceholders();

        if (canLaunchDetailFragmentAlongside()) loadDetailFragmentIfCapable();
    }

    private boolean canLaunchDetailFragmentAlongside() {
        if (getActivity() == null) return false;
        boolean isAdapterEmpty = mDataAdapter.getAdapterData().size() == 0;
        boolean isTabletLandscape = ((MasterActivity) getActivity()).isTabletLandscape();
        return isAdapterEmpty && isTabletLandscape;
    }

    //loads detail fragment:
    //if tablet is landscape
    // && detail fragment container has no fragment
    //&& this is currently selected tab
    // && is containing fragment on top in fragment detail container
    private void loadDetailFragmentIfCapable() {
        if (getParentFragment() == null) return;
        MasterActivity masterActivity = ((MasterActivity) getParentFragment().getActivity());

        if (masterActivity == null) return;
        Fragment activeFragment = masterActivity.getSupportFragmentManager()
                .findFragmentById(R.id.master_fragments_container);

        if (activeFragment == null) return;
        String activeClassName = activeFragment.getClass().getName();
        String parentClassName = getParentFragment().getClass().getName();

        boolean isCurrentTab =
                mMediaType == ((ListResultsParentFragment) getParentFragment()).getCurrentTabPosition();

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
        if (getActivity() == null) return;
        if (mDataAdapter.inPlaceholderMode(position)) {
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

    private void startDetailsFragment(MediaData selectedData) {
        if (getActivity() == null) return;
        ((MasterActivity) getActivity()).launchDetailsFragment(selectedData, null);
    }
}