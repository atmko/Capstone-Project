/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.CustomParams;
import com.atmko.onmywatch.adapters.MediaDataAdapter;
import com.atmko.onmywatch.adapters.MediaLogAdapter;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaLog;
import com.atmko.onmywatch.models.MovieLog;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.view_models.HomeListDisplayViewModel;
import com.atmko.onmywatch.view_models.HomeListDisplayViewModelFactory;

import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;

public class HomeListDisplayFragment extends Fragment
        implements MediaDataAdapter.OnListItemClickListener, MediaLogAdapter.OnListItemClickListener{
    public static final String FRAGMENT_KEY = "home_list_display_fragment";

    public static final String UPCOMING_MOVIES = "upcoming_movies";
    public static final String ALREADY_RELEASED_MOVIES = "released_movies";
    public static final String UNDATED_MOVIES = "undated_movies";

    public static final String UPCOMING_EPISODES = "upcoming_episodes";
    public static final String ENDED_SERIES = "ended_series";
    public static final String UNDATED_SERIES = "undated_series";

    private static final String LIST_NAME_KEY = "list_name";
    private static final String MEDIA_TYPE_KEY = "media_type";

    //fragment instantiation values
    private String mListName;
    private int mMediaType;

    //post instantiation values
    private RecyclerView mRecyclerView;
    private MediaLogAdapter mMediaLogAdapter;

    public HomeListDisplayFragment() {
        // Required empty public constructor
    }

    public static HomeListDisplayFragment newInstance(String listName, int mediaType) {
        HomeListDisplayFragment fragment = new HomeListDisplayFragment();
        Bundle args = new Bundle();
        args.putString(LIST_NAME_KEY, listName);
        args.putInt(MEDIA_TYPE_KEY, mediaType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mListName = getArguments().getString(LIST_NAME_KEY);
            mMediaType = getArguments().getInt(MEDIA_TYPE_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_recycler_results, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();

        observeData();
    }

    private void defineViews() {
        if (getView() == null) return;
        if (getActivity() == null) return;

        mRecyclerView = getView().findViewById(R.id.results_recycler_view);
        mRecyclerView.setLayoutManager(configureLayoutManager());

        mMediaLogAdapter = new MediaLogAdapter(this, getActivity().getApplicationContext(),
                CustomParams.getSearchParams(this));
        mMediaLogAdapter
                .setPlaceHolderCapacity(getResources().getInteger(R.integer.search_column_span));

        mRecyclerView.setAdapter(mMediaLogAdapter);
    }

    private void observeData() {
        AppDatabase database = AppDatabase.getInstance(getContext());
        HomeListDisplayViewModelFactory homeListDisplayViewModelFactory =
                new HomeListDisplayViewModelFactory(database, mListName);

        final HomeListDisplayViewModel listsViewModel = ViewModelProviders.of(this, homeListDisplayViewModelFactory)
                .get(HomeListDisplayViewModel.class);
        //noinspection unchecked
        final LiveData<List> displayListLiveData = listsViewModel.getHomeDisplayList();

        displayListLiveData.observe(getViewLifecycleOwner(), new Observer<List>() {
            @Override
            public void onChanged(List mediaDataList) {
                populateAndNotifyAdapter(mediaDataList);
            }
        });
    }

    private LinearLayoutManager configureLayoutManager() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        return layoutManager;
    }

    private void populateAndNotifyAdapter(List mediaList) {
        mMediaLogAdapter.getAdapterData().clear();
        if (mMediaType == MEDIA_TYPE_MOVIE) mediaList = MovieLog.convertMediaToLogs(mediaList, mListName);

        mMediaLogAdapter.addAdapterData(mediaList);
        mMediaLogAdapter.setPlaceholders();
    }
    @Override
    public void onItemClick(final int position) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) return;

                if (mMediaLogAdapter.inPlaceholderMode(position)) {
                    DiscoverParentFragment discoverParentFragment = DiscoverParentFragment.newInstance();

                    getActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_down_entry, android.R.animator.fade_out)
                            .add(R.id.master_fragments_container, discoverParentFragment,
                                    DiscoverParentFragment.FRAGMENT_KEY)
                            .commit();

                    return;
                }

                MediaLog mediaLog = mMediaLogAdapter.getAdapterData().get(position);
                MediaData selectedData;
                if (mMediaType == MEDIA_TYPE_MOVIE) {
                    selectedData = AppDatabase.getInstance(getContext())
                            .movieDataDao().getMovieByIdAlt(mediaLog.parentId);

                } else {
                    selectedData = AppDatabase.getInstance(getContext())
                            .seriesDataDao().getSeriesByIdAlt(mediaLog.parentId);
                }

                ((MasterActivity) getActivity()).launchDetailsFragment(selectedData, null);
            }
        });
    }

    @Override
    public void onAddButtonClick(final int position) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                MediaLog mediaLog = mMediaLogAdapter.getAdapterData().get(position);
                if (getActivity() != null) {
                    MediaData mediaData;
                    if (mMediaType == MEDIA_TYPE_MOVIE) {
                        mediaData = AppDatabase.getInstance(getActivity())
                                .movieDataDao().getMovieByIdAlt(mediaLog.parentId);

                    } else {
                        mediaData = AppDatabase.getInstance(getActivity())
                                .seriesDataDao().getSeriesByIdAlt(mediaLog.parentId);
                    }

                    ((MasterActivity) getActivity())
                            .launchAddToListActivity(mediaData);
                }
            }
        });
    }
}