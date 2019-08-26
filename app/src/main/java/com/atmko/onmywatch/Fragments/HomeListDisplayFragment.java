package com.atmko.onmywatch.Fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
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

import org.parceler.Parcels;

import java.util.Arrays;
import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class HomeListDisplayFragment extends Fragment implements MediaDataAdapter.OnListItemClickListener{
    public static String FRAGMENT_KEY = "home_list_display_fragment";

    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String LIST_NAME_KEY = "list_name";

    //fragment instantiation values
    private int mMediaType;
    private String mListName;

    private static final String ADAPTER_DATA_LIST_KEY = "adapter_data_list";

    //post instantiation values
    private RecyclerView mRecyclerView;
    private MediaDataAdapter mMediaDataAdapter;


    public HomeListDisplayFragment() {
        // Required empty public constructor
    }

    public static HomeListDisplayFragment newInstance(int mediaType, String listName) {
        HomeListDisplayFragment fragment = new HomeListDisplayFragment();
        Bundle args = new Bundle();
        args.putInt(MEDIA_TYPE_KEY, mediaType);
        args.putString(LIST_NAME_KEY, listName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
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

        if (savedInstanceState == null) {
            observeData();

            //TODO live data isn't called on restore this will be fixed with view model
        } else {
            List<MediaData> mediaDataList =
                    Parcels.unwrap(savedInstanceState.getParcelable(ADAPTER_DATA_LIST_KEY));
            mMediaDataAdapter.addAdapterData(mediaDataList);
        }
    }

    private void defineViews() {
        mRecyclerView = getView().findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(configureLayoutManager());
        mMediaDataAdapter = new MediaDataAdapter(this);
        mRecyclerView.setAdapter(mMediaDataAdapter);
    }

    private void observeData() {
        AppDatabase mDatabase = AppDatabase.getInstance(getContext());
        final String[] watchStatusMoviesTitles =
                getContext().getResources().getStringArray(R.array.watch_status_movie_titles);
        List<String> titleList = Arrays.asList(watchStatusMoviesTitles);

        if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            LiveData<List<MovieData>> listLiveData = mDatabase.movieDataDao()
                    .getMoviesByWatchStatus(titleList.indexOf(mListName));

            listLiveData.observe(getActivity(), new Observer<List<MovieData>>() {
                @Override
                public void onChanged(List<MovieData> movieDataList) {
                    mMediaDataAdapter.getAdapterData().clear();
                    mMediaDataAdapter.addAdapterData(movieDataList);

                    Log.d(FRAGMENT_KEY, "querying database");
                }
            });

        } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            LiveData<List<SeriesData>> listLiveData = mDatabase.seriesDataDao()
                    .getSeriesByWatchStatus(titleList.indexOf(mListName));

            listLiveData.observe(getActivity(), new Observer<List<SeriesData>>() {
                @Override
                public void onChanged(List<SeriesData> seriesDataList) {
                    mMediaDataAdapter.getAdapterData().clear();
                    mMediaDataAdapter.addAdapterData(seriesDataList);

                    Log.d(FRAGMENT_KEY, "querying database");
                }
            });
        }
    }

    private LinearLayoutManager configureLayoutManager() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        return layoutManager;
    }

    @Override
    public void onItemClick(int position) {
        String[] detailUrls = getContext().getResources().getStringArray(R.array.details_urls);
        String detailUrl = null;
        MediaData selectedData = mMediaDataAdapter.getAdapterData().get(position);

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            detailUrl = detailUrls[MEDIA_TYPE_SERIES];
        }

        SearchPreferences searchPreferences =  new SearchPreferences();
        Parcelable parceledData = Parcels.wrap(selectedData);
        Parcelable parceledSharedPreferences = Parcels.wrap(searchPreferences);

        DetailsFragment detailsFragment =
                DetailsFragment.newInstance(mMediaType, detailUrl, parceledData, parceledSharedPreferences);

        Fragment detailContainerFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.detail_fragments_container);

        ///remove existing fragment
        if (detailContainerFragment != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                    .remove(detailContainerFragment).commit();

        }

        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                .add(R.id.detail_fragments_container, detailsFragment, DetailsFragment.FRAGMENT_KEY)
                .commit();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ADAPTER_DATA_LIST_KEY, Parcels.wrap(mMediaDataAdapter.getAdapterData()));

    }
}
