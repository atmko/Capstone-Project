/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.content.Intent;
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

import com.atmko.onmywatch.CreateListActivity;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.adapters.ListsAdapter;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.UserListsAdapter;
import com.atmko.onmywatch.adapters.WatchListsAdapter;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.view_models.ListsWatchAndUserViewModel;

import java.util.List;

public class ListWatchAndUserFragment extends Fragment implements ListsAdapter.OnListItemClickListener,
        UserListsAdapter.OnSpinnerItemClickListener {

    public static String FRAGMENT_KEY = "list_watch_and_user_fragment";

    //fragment initialization parameters
    private static final String LIST_TYPE_KEY = "list_type";
    private int mListType;

    //check for restoring state
    private boolean mFirstInit = true;
    private Bundle mSavedInstanceState;
    private AppDatabase mDatabase;
    private ListsAdapter mAdapter;
    private RecyclerView mRecyclerView;

    private FloatingActionButton mFab;
    private SuperEditText mSearchTextView;


    public ListWatchAndUserFragment() {
        // Required empty public constructor
    }

    public static ListWatchAndUserFragment newInstance(int listType) {
        ListWatchAndUserFragment fragment = new ListWatchAndUserFragment();
        Bundle args = new Bundle();
        args.putInt(LIST_TYPE_KEY, listType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mListType = getArguments().getInt(LIST_TYPE_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_list_watch_and_user, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        defineViews();

        observeData();
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
        mDatabase = AppDatabase.getInstance(getContext());
        mRecyclerView = getView().findViewById(R.id.lists_recycler_view);
        mRecyclerView.setLayoutManager(configureLayoutManager());

        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            mAdapter = new WatchListsAdapter(this);

        } else if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_USER) {
            mAdapter = new UserListsAdapter(this);

        }

        mRecyclerView.setAdapter(mAdapter);

        mFab = getView().findViewById(R.id.new_list_fab);
        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            mFab.hide();

        } else {
            mFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchCreateListActivity(CreateListActivity.MODE_CREATE, "", 0);
                }
            });
        }

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
                onSearchTextChanged(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void observeData() {
        final ListsWatchAndUserViewModel viewModel =
                ViewModelProviders.of(getParentFragment()).get(ListsWatchAndUserViewModel.class);

        if (mAdapter instanceof WatchListsAdapter) {
            viewModel.getWatchLists().observe(getParentFragment(), new Observer<List<WatchListModel>>() {
                @Override
                public void onChanged(List<WatchListModel> watchListModels) {
                    mAdapter.getAdapterData().clear();
                    mAdapter.addAdapterData(watchListModels);

                    //restore search if it exists
                    final ImageButton searchImageButton = getParentFragment().
                            getView().findViewById(R.id.search_image_button);
                    MasterActivity masterActivity = ((MasterActivity) getActivity());
                    masterActivity.restoreSavedSearch(ListWatchAndUserFragment.this,
                            mFirstInit, mSavedInstanceState, searchImageButton, mSearchTextView);

                    mFirstInit = false;
                }
            });

        } else if (mAdapter instanceof UserListsAdapter) {
            viewModel.getUserLists().observe(getParentFragment(), new Observer<List<UserListModel>>() {
                @Override
                public void onChanged(List<UserListModel> userListModels) {
                    populateAndNotifyAdapter(userListModels);

                    //restore search if it exists
                    final ImageButton searchImageButton = getParentFragment().
                            getView().findViewById(R.id.search_image_button);
                    MasterActivity masterActivity = ((MasterActivity) getActivity());
                    masterActivity.restoreSavedSearch(ListWatchAndUserFragment.this,
                            mFirstInit, mSavedInstanceState, searchImageButton, mSearchTextView);

                    mFirstInit = false;
                }
            });
        }
    }

    private void populateAndNotifyAdapter(List listModels) {
        if (listModels.size() == 0) {
            mAdapter.setInPlaceholderMode(true);

        } else {
            mAdapter.setInPlaceholderMode(false);
            mAdapter.getAdapterData().clear();
            mAdapter.addAdapterData(listModels);
        }
    }

    private void onSearchTextChanged(CharSequence searchText) {
        String listName = searchText.toString();
        listName = "%" + listName + "%";

        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            //observe lists with searched name then remove observer
            final LiveData<List<WatchListModel>> listLiveData =
                    mDatabase.watchListsDao().getListsWithNameLike(listName);
            listLiveData.observe(getParentFragment(), new Observer<List<WatchListModel>>() {
                @Override
                public void onChanged(List<WatchListModel> watchListModels) {
                    listLiveData.removeObserver(this);
                    populateAndNotifyAdapter(watchListModels);
                }
            });
        }

        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_USER) {
            //observe lists with searched name then remove observer
            final LiveData<List<UserListModel>> listLiveData =
                    mDatabase.userListsDao().getListsWithNameLike(listName);
            listLiveData.observe(getParentFragment(), new Observer<List<UserListModel>>() {
                @Override
                public void onChanged(List<UserListModel> userListModels) {
                    listLiveData.removeObserver(this);
                    populateAndNotifyAdapter(userListModels);
                }
            });
        }
    }

    private void launchCreateListActivity(int mode, String listName, int itemCount) {
        Intent intent = new Intent(getActivity().getApplicationContext(), CreateListActivity.class);
        intent.putExtra(CreateListActivity.MODE_KEY, mode);
        intent.putExtra(CreateListActivity.LIST_NAME_KEY, listName);
        intent.putExtra(CreateListActivity.ITEM_COUNT_KEY, itemCount);

        startActivity(intent);
    }

    @Override
    public void onItemClick(int position) {
        if (mAdapter.inPlaceholderMode()) {
            launchCreateListActivity(CreateListActivity.MODE_CREATE, "", 0);

            return;
        }

        String listName = ((ListModel) mAdapter.getAdapterData().get(position)).getName();

        Fragment fragment = ListResultsParentFragment.newInstance(mListType, listName);

        getParentFragment().getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                .add(R.id.master_fragments_container, fragment, ListResultsParentFragment.FRAGMENT_KEY)
                .commit();
    }

    @Override
    public void onEditClick(ListModel userListModel) {
        launchCreateListActivity(CreateListActivity.MODE_EDIT,
                userListModel.getName(), userListModel.getItemCount());
    }

    @Override
    public void onDeleteClick(final ListModel userListModel) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                List<MovieData> moviesInList = mDatabase.movieDataRecordsDao()
                        .getAllMoviesInListAlt(userListModel.getName());

                List<SeriesData> seriesInList = mDatabase.seriesDataRecordsDao()
                        .getAllSeriesInListAlt(userListModel.getName());

                mDatabase.userListsDao().deleteList(new UserListModel(userListModel.getName()));

                maintainMoviesWatchListCountIntegrity(moviesInList);

                maintainSeriesWatchListCountIntegrity(seriesInList);
            }
        });
    }

    private void maintainMoviesWatchListCountIntegrity(List<MovieData> moviesInList) {
        for (MovieData movieData: moviesInList) {
            //delete if containing lists size = 0 and if watch status is none(0)
            List<UserListModel> containingLists =
                    mDatabase.movieDataRecordsDao()
                            .getAllListsContainingMediaAlt(movieData.getId());

            int watchStatus = movieData.getWatchStatus();

            if (containingLists.size() == 0 && movieData.getWatchStatus() == 0) {
                mDatabase.movieDataDao().deleteMovieData(movieData);

                if (getContext() == null) return;

                //subtract 1 from watch status list if deleted
                WatchListModel watchList = mDatabase.watchListsDao().getListByNameAlt(
                        MediaData.getWatchStatusTitle(watchStatus, getContext()));

                watchList.setItemCount(watchList.getItemCount() - 1);

                mDatabase.watchListsDao().updateListConfiguration(watchList);
            }
        }
    }

    private void maintainSeriesWatchListCountIntegrity(List<SeriesData> seriesInList) {
        for (SeriesData seriesData: seriesInList) {
            //delete if containing lists size = 0 and if watch status is none(0)
            List<UserListModel> containingLists =
                    mDatabase.seriesDataRecordsDao()
                            .getAllListsContainingMediaAlt(seriesData.getId());

            int watchStatus = seriesData.getWatchStatus();

            if (containingLists.size() == 0 && seriesData.getWatchStatus() == 0) {
                mDatabase.seriesDataDao().deleteSeriesData(seriesData);

                if (getContext() == null) return;

                //subtract 1 from watch status list if deleted
                WatchListModel watchList = mDatabase.watchListsDao().getListByNameAlt(
                        MediaData.getWatchStatusTitle(watchStatus, getContext()));

                watchList.setItemCount(watchList.getItemCount() - 1);

                mDatabase.watchListsDao().updateListConfiguration(watchList);
            }
        }
    }
}