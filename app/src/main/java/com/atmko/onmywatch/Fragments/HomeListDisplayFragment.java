/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.MediaDataAdapter;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.SearchPreferences;
import com.atmko.onmywatch.view_models.ListResultsViewModelFactory;
import com.atmko.onmywatch.view_models.ListsResultsViewModel;

import org.parceler.Parcels;

import java.util.Arrays;
import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class HomeListDisplayFragment extends Fragment implements MediaDataAdapter.OnListItemClickListener{
    public static String FRAGMENT_KEY = "home_list_display_fragment";

    private static final String LIST_TYPE_KEY = "list_type";
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String LIST_NAME_KEY = "list_name";

    //fragment instantiation values
    private int mListType;
    private int mMediaType;
    private String mListName;

    //post instantiation values
    private RecyclerView mRecyclerView;
    private MediaDataAdapter mMediaDataAdapter;


    public HomeListDisplayFragment() {
        // Required empty public constructor
    }

    public static HomeListDisplayFragment newInstance(int mediaType, int listType, String listName) {
        HomeListDisplayFragment fragment = new HomeListDisplayFragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_home_list_display, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();

        observeData();

        if (savedInstanceState == null) {

        } else {

        }
    }

    private void defineViews() {
        mRecyclerView = getView().findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(configureLayoutManager());
        mMediaDataAdapter = new MediaDataAdapter(this,
                getActivity().getApplicationContext());
        mRecyclerView.setAdapter(mMediaDataAdapter);
    }

    private void observeData() {
        final String[] watchStatusMoviesTitles = getContext().getResources()
                .getStringArray(R.array.watch_status_movie_titles);
        List<String> titleList = Arrays.asList(watchStatusMoviesTitles);

        AppDatabase database = AppDatabase.getInstance(getContext());
        ListResultsViewModelFactory resultsViewModelFactory =
                new ListResultsViewModelFactory(database, ListsWatchAndUserParentFragment.LIST_TYPE_WATCH,
                        mMediaType, titleList, mListName);

        final ListsResultsViewModel viewModel =
                ViewModelProviders.of(this, resultsViewModelFactory)
                        .get(ListsResultsViewModel.class);

        //if this is a watch list
        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                viewModel.getAllMoviesInWatchList().observe(this, new Observer<List<MovieData>>() {
                    @Override
                    public void onChanged(List<MovieData> mediaDataList) {
                        populateAndNotifyAdapter(mediaDataList);
                    }
                });

            //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                viewModel.getAllSeriesInWatchList().observe(this, new Observer<List<SeriesData>>() {
                    @Override
                    public void onChanged(List<SeriesData> mediaDataList) {
                        populateAndNotifyAdapter(mediaDataList);
                    }
                });
            }
        }

        //if this is a user list
        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_USER) {
            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                viewModel.getAllMoviesInUserList().observe(this, new Observer<List<MovieData>>() {
                    @Override
                    public void onChanged(List<MovieData> mediaDataList) {
                        populateAndNotifyAdapter(mediaDataList);
                    }
                });

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                viewModel.getAllSeriesInUserList().observe(this, new Observer<List<SeriesData>>() {
                    @Override
                    public void onChanged(List<SeriesData> mediaDataList) {
                        populateAndNotifyAdapter(mediaDataList);
                    }
                });
            }
        }
    }

    private LinearLayoutManager configureLayoutManager() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        return layoutManager;
    }

    private void populateAndNotifyAdapter(List mediaDataList) {
        if (mediaDataList.size() == 0) {
            mMediaDataAdapter.setInPlaceholderMode(true);

        } else {
            mMediaDataAdapter.setInPlaceholderMode(false);
            mMediaDataAdapter.getAdapterData().clear();
            mMediaDataAdapter.addAdapterData(mediaDataList);
        }
    }
    @Override
    public void onItemClick(int position) {
        if (mMediaDataAdapter.inPlaceholderMode()) {
            SearchParentFragment searchParentFragment = SearchParentFragment.newInstance();

            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_down_entry, android.R.animator.fade_out)
                    .add(R.id.master_fragments_container,searchParentFragment,
                            SearchParentFragment.FRAGMENT_KEY)
                    .commit();

            return;
        }

        MediaData selectedData = mMediaDataAdapter.getAdapterData().get(position);

        ((MasterActivity) getActivity())
                .launchDetailsFragment(HomeListDisplayFragment.this, selectedData);
    }
}