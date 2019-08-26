package com.atmko.onmywatch.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

public class ListResultsFragment extends Fragment implements MediaDataAdapter.OnListItemClickListener{
    public static final String FRAGMENT_KEY = "list_results_fragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String LIST_TYPE_KEY = "list_type";
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String LIST_NAME_KEY = "list_name";

    // TODO: Rename and change types of parameters
    private int mListType;
    private int mMediaType;
    private String mListName;

    private MediaDataAdapter mDataAdapter;
    private SearchPreferences mSearchPreferences;

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

        final String[] watchStatusMoviesTitles = getContext().getResources()
                .getStringArray(R.array.watch_status_movie_titles);
        List<String> titleList = Arrays.asList(watchStatusMoviesTitles);

        AppDatabase database = AppDatabase.getInstance(getContext());
        ListResultsViewModelFactory resultsViewModelFactory =
                new ListResultsViewModelFactory(database, mListType, mMediaType, titleList, mListName);

        final ListsResultsViewModel viewModel =
                ViewModelProviders.of(this, resultsViewModelFactory)
                .get(ListsResultsViewModel.class);

        observeData(viewModel);

        if (savedInstanceState == null) {

        } else {

        }
    }

    private void defineViews() {
        RecyclerView mListResultsRecyclerView =
                getView().findViewById(R.id.list_results_recycler_view);

        mListResultsRecyclerView.setLayoutManager(configureLayoutManager());

        mDataAdapter = new MediaDataAdapter(this);
        mListResultsRecyclerView.setAdapter(mDataAdapter);
        mSearchPreferences = new SearchPreferences();
    }

    private void observeData(ListsResultsViewModel viewModel) {
        //if this is a watch list
        if (mListType == ListsParentFragment.LIST_TYPE_WATCH) {
            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                viewModel.getAllMoviesInWatchList().observe(this,
                        new Observer<List<MovieData>>() {
                    @Override
                    public void onChanged(List<MovieData> movieDataList) {
                        mDataAdapter.getAdapterData().clear();
                        mDataAdapter.addAdapterData(movieDataList);

                        if (mDataAdapter.getAdapterData().size() > 0 ) loadDetailFragment();
                    }
                });

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                viewModel.getAllSeriesInWatchList().observe(this,
                        new Observer<List<SeriesData>>() {
                    @Override
                    public void onChanged(List<SeriesData> seriesDataList) {
                        mDataAdapter.getAdapterData().clear();
                        mDataAdapter.addAdapterData(seriesDataList);

                        if (mDataAdapter.getAdapterData().size() > 0 ) loadDetailFragment();
                    }
                });
            }
        }

        //if this is a user list
        if (mListType == ListsParentFragment.LIST_TYPE_USER) {
            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                viewModel.getAllMoviesInUserList().observe(this, new Observer<List<MovieData>>() {
                    @Override
                    public void onChanged(List<MovieData> movieDataList) {
                        mDataAdapter.getAdapterData().clear();
                        mDataAdapter.addAdapterData(movieDataList);

                        if (mDataAdapter.getAdapterData().size() > 0 ) loadDetailFragment();
                    }
                });

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                viewModel.getAllSeriesInUserList().observe(this, new Observer<List<SeriesData>>() {
                    @Override
                    public void onChanged(List<SeriesData> seriesDataList) {
                        mDataAdapter.getAdapterData().clear();
                        mDataAdapter.addAdapterData(seriesDataList);

                        if (mDataAdapter.getAdapterData().size() > 0 ) loadDetailFragment();
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

    //loads detail fragment:
    //if tablet is landscape
    // && detail fragment container has no fragment
    // && is containing fragment on top in fragment detail container
    private void loadDetailFragment() {
        MasterActivity masterActivity = ((MasterActivity) getParentFragment().getActivity());

        Fragment activeFragment = masterActivity.getSupportFragmentManager()
                .findFragmentById(R.id.master_fragments_container);
        String activeClassName = activeFragment.getClass().getName();
        String parentClassName = getParentFragment().getClass().getName();

        boolean isParentActive = activeClassName.equals(parentClassName);

        if (masterActivity.isTabletLandscape()
                && !masterActivity.hasFragment(R.id.detail_fragments_container)
                //TODO consider detaching fragments to disable background updates instead of "isParentActive"
                && isParentActive) {

            MediaData firstMediaData = mDataAdapter.getAdapterData().get(0);

            startDetailsFragment(firstMediaData);
        }
    }

    @Override
    public void onItemClick(int position) {
        MediaData selectedData = mDataAdapter.getAdapterData().get(position);

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
                getParentFragment().getActivity().getSupportFragmentManager()
                        .findFragmentById(R.id.detail_fragments_container);

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

    }
}
