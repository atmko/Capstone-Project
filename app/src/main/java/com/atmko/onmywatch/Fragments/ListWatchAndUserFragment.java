/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidnetworking.core.MainThreadExecutor;
import com.atmko.onmywatch.ConfirmationActivity;
import com.atmko.onmywatch.CreateListActivity;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.ListsAdapter;
import com.atmko.onmywatch.adapters.TagAdapter;
import com.atmko.onmywatch.adapters.UserListsAdapter;
import com.atmko.onmywatch.adapters.WatchListsAdapter;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SearchListTag;
import com.atmko.onmywatch.models.SearchMediaTag;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SimpleIdlingResource;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.view_models.ListsWatchAndUserViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import static com.atmko.onmywatch.fragments.ListsWatchAndUserParentFragment.LIST_TYPE_AUTO;

public class ListWatchAndUserFragment extends Fragment implements ListsAdapter.OnListItemClickListener,
        UserListsAdapter.OnSpinnerItemClickListener {

    private static final String FRAGMENT_KEY = "list_watch_and_user_fragment";


    private static final int REQUEST_DELETE = 1;

    //fragment initialization parameters
    private static final String LIST_TYPE_KEY = "list_type";
    private static final String SHOW_FAB_KEY = "show_fab";
    private int mListType;
    private boolean mShowFab;

    private static ListWatchAndUserFragment.OnListModelClickListener sOnListModelClickListener;

    //check for restoring state
    private AppDatabase mDatabase;
    private ListsAdapter mAdapter;

    private SuperEditText mSearchTextView;
    private TagAdapter tagAdapter;

    public ListWatchAndUserFragment() {
        // Required empty public constructor
    }

    public static ListWatchAndUserFragment newInstance(int listType, boolean showFab) {
        ListWatchAndUserFragment fragment = new ListWatchAndUserFragment();
        Bundle args = new Bundle();
        args.putInt(LIST_TYPE_KEY, listType);
        args.putBoolean(SHOW_FAB_KEY, showFab);
        fragment.setArguments(args);
        return fragment;
    }

    public interface OnListModelClickListener {
        void onListModelClick(ListsAdapter adapter, Fragment childFragment, int listType,
                              ListModel listModel);
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof OnListModelClickListener)) {
            Log.d(FRAGMENT_KEY, OnListModelClickListener.class.getSimpleName() + " must be implemented");
        } else {
            sOnListModelClickListener = ((OnListModelClickListener) context);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mListType = getArguments().getInt(LIST_TYPE_KEY);
            mShowFab = getArguments().getBoolean(SHOW_FAB_KEY);
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

        defineViews();

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
        if (getView() == null) return;
        if (getParentFragment() == null) return;
        if (getParentFragment().getView() == null) return;

        mDatabase = AppDatabase.getInstance(getContext());
        RecyclerView mRecyclerView = getView().findViewById(R.id.results_recycler_view);
        mRecyclerView.setLayoutManager(configureLayoutManager());

        if (mListType != ListsWatchAndUserParentFragment.LIST_TYPE_USER) {
            mAdapter = new WatchListsAdapter(this);

        } else {
            mAdapter = new UserListsAdapter(this);
        }

        mRecyclerView.setAdapter(mAdapter);

        FloatingActionButton mFab = getView().findViewById(R.id.new_list_fab);
        if (mShowFab) {
            mFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getParentFragment() != null) {
                        MasterActivity.launchCreateListActivity(getParentFragment().getActivity());
                    }
                }
            });
        } else {
            mFab.hide();
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
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_USER) {
                            onSearchTextChanged();
                        }
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

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    @SuppressWarnings("unchecked")
    private void observeData(final Bundle savedInstanceState) {
        if (getParentFragment() == null) return;

        ListsWatchAndUserViewModel viewModel = ViewModelProviders.of(getParentFragment())
                .get(ListsWatchAndUserViewModel.class);
        LiveData listsLiveData;

        if (!(mAdapter instanceof UserListsAdapter)) {
            if (mListType == LIST_TYPE_AUTO) {
                listsLiveData = viewModel.getAutoLists();

            } else {
                listsLiveData = viewModel.getWatchLists();
            }

            listsLiveData.observe(getViewLifecycleOwner(), new Observer<List<WatchListModel>>() {
                @Override
                public void onChanged(List<WatchListModel> watchListModels) {
                    mAdapter.getAdapterData().clear();
                    mAdapter.addAdapterData(watchListModels);

                    //restore search if it exists
                    MasterActivity.restoreSearchIfAvailable(ListWatchAndUserFragment.this,
                            savedInstanceState);
                }
            });

        } else {
            listsLiveData = viewModel.getUserLists();
            listsLiveData.observe(getViewLifecycleOwner(), new Observer<List<UserListModel>>() {
                @Override
                public void onChanged(List<UserListModel> userListModels) {
                    populateAndNotifyAdapter(userListModels);

                    //restore search if it exists
                    MasterActivity.restoreSearchIfAvailable(ListWatchAndUserFragment.this,
                            savedInstanceState);
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

    private static final int TAG_COUNT_LIMIT = 7;
    private void onSearchTextChanged() {
        if (getContext() == null) return;

        String activeText = mSearchTextView.getActiveText();
        final List<String> searchTags = AppDatabase.getLocalDatabase(getContext()).searchListTagsDao()
                .getTagsLikeAlt(activeText);

        new MainThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                tagAdapter.clear();
                tagAdapter.addAll(searchTags);
                tagAdapter.notifyDataSetChanged();

                performFullSearchWithTags();
            }
        });
    }

    private void performFullSearchWithTags() {
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

        searchInUserList(formattedTags);
    }

    private void searchInUserList(List<String> formattedTags) {
        if (getParentFragment() == null) return;

        final LiveData<List<UserListModel>> listsLiveData = mDatabase.userListsDao()
                .getListsWithNameLike(formattedTags.get(0), formattedTags.get(1),
                        formattedTags.get(2), formattedTags.get(3), formattedTags.get(4),
                        formattedTags.get(5), formattedTags.get(6));

        listsLiveData.observe(getParentFragment(), new Observer<List<UserListModel>>() {
            @Override
            public void onChanged(List<UserListModel> userListModels) {
                listsLiveData.removeObserver(this);
                populateAndNotifyAdapter(userListModels);
            }
        });
    }

    private void launchCreateListActivity(UserListModel userListModel) {
        if (getActivity() == null) return;

        Intent intent = new Intent(getActivity().getApplicationContext(), CreateListActivity.class);
        intent.putExtra(CreateListActivity.MODE_KEY, CreateListActivity.MODE_EDIT);
        intent.putExtra(CreateListActivity.USER_LIST_KEY, Parcels.wrap(userListModel));

        startActivity(intent);
    }

    @Override
    public void onItemClick(ListModel listModel, AppCompatCheckBox checkBox) {
        sOnListModelClickListener.onListModelClick(mAdapter, this, mListType, listModel);
    }

    @Override
    public void onEditClick(ListModel userListModel) {
        launchCreateListActivity(((UserListModel) userListModel));
    }

    @Override
    public void onDeleteClick(final ListModel userListModel) {
        if (getParentFragment() != null) {
            MasterActivity.launchConfirmationActivity(getParentFragment(), userListModel,
                    REQUEST_DELETE, ConfirmationActivity.ACTION_DELETE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_DELETE && resultCode == Activity.RESULT_OK) {
            if (getIdlingResource() != null) {
                getIdlingResource().setIdleState(false);
            }

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    if (data == null) {
                        if (getActivity() == null) return;
                        //notify user of error
                        Snackbar.make(getActivity().findViewById(R.id.top_layout),
                                getString(R.string.confirmation_error_message), Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    mDatabase = AppDatabase.getInstance(getContext());

                    UserListModel userListModel = Parcels.unwrap(
                            data.getParcelableExtra(ConfirmationActivity.SELECTED_DATA_KEY));

                    if (userListModel != null) {
                        List<MovieData> moviesInList = mDatabase.movieDataRecordsDao()
                                .getAllMoviesInListAlt(userListModel.getName());

                        List<SeriesData> seriesInList = mDatabase.seriesDataRecordsDao()
                                .getAllSeriesInListAlt(userListModel.getName());

                        mDatabase.userListsDao().deleteList(userListModel);

                        if (moviesInList != null) maintainMoviesWatchListCountIntegrity(moviesInList);

                        if (seriesInList != null) maintainSeriesWatchListCountIntegrity(seriesInList);

                        deleteListTag(userListModel.getName());
                    }

                    if (getIdlingResource() != null) {
                        getIdlingResource().setIdleState(true);
                    }
                }
            });
        }
    }

    private SimpleIdlingResource getIdlingResource() {
        if (getActivity() == null) return null;

        return MasterActivity.sIdlingResource;
    }

    //checks if media tags are in use and deletes them if not
    private void deleteMediaTags(MediaData mediaData) {
        //delete media tags
        if (mediaData.searchTags == null) return;

        for (SearchMediaTag tag: mediaData.searchTags) {
            int tagUsage = mDatabase.movieDataDao().getAllMediaWithTagAlt(tag.mTag).size()
                    + mDatabase.movieDataDao().getAllMediaWithTagAlt(tag.mTag).size();

            if (tagUsage == 0) {
                mDatabase.searchMediaTagsDao().deleteTag(tag);
            }
        }
    }

    //checks if list tag exists and deletes it if so
    private void deleteListTag(String listName) {
        //delete list tag
        SearchListTag tagToDelete = mDatabase.searchListTagsDao().getTagAlt(listName.toLowerCase());
        if (tagToDelete !=  null) {
            mDatabase.searchListTagsDao().deleteTag(tagToDelete);
        }
    }

    private void maintainMoviesWatchListCountIntegrity(List<MovieData> moviesInList) {
        for (MovieData movieData: moviesInList) {
            //delete if containing lists size = 0 and if watch status is none(0)
            List<UserListModel> containingLists =
                    mDatabase.movieDataRecordsDao()
                            .getAllListsContainingMediaAlt(movieData.getId());

            int watchStatus = movieData.getWatchStatus();

            //TODO delete notifiers when item is unused
            if (containingLists.size() == 0 && movieData.getWatchStatus() == 0) {
                mDatabase.movieDataDao().deleteMovieData(movieData);
                deleteMediaTags(movieData);

                if (getContext() == null) continue;

                //subtract 1 from watch status list if deleted
                WatchListModel watchList = mDatabase.watchListsDao().getListByNameAlt(
                        MediaData.getWatchStatusTitle(watchStatus, getContext()));

                //ensure list has a value higher than zero
                if (watchList.getItemCount() <= 0) continue;

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

            //TODO delete notifiers when item is unused
            if (containingLists.size() == 0 && seriesData.getWatchStatus() == 0) {
                mDatabase.seriesDataDao().deleteSeriesData(seriesData);
                deleteMediaTags(seriesData);

                if (getContext() == null) continue;

                //subtract 1 from watch status list if deleted
                WatchListModel watchList = mDatabase.watchListsDao().getListByNameAlt(
                        MediaData.getWatchStatusTitle(watchStatus, getContext()));

                //ensure list has a value higher than zero
                if (watchList.getItemCount() <= 0) continue;

                watchList.setItemCount(watchList.getItemCount() - 1);

                mDatabase.watchListsDao().updateListConfiguration(watchList);
            }
        }
    }
}