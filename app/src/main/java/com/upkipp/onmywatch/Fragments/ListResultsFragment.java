package com.upkipp.onmywatch.Fragments;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.upkipp.onmywatch.MasterActivity;
import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.adapters.MediaDataAdapter;
import com.upkipp.onmywatch.database.AppDatabase;
import com.upkipp.onmywatch.models.MediaData;
import com.upkipp.onmywatch.models.MovieData;
import com.upkipp.onmywatch.models.SeriesData;
import com.upkipp.onmywatch.utils.SearchPreferences;

import org.parceler.Parcels;

import java.util.Arrays;
import java.util.List;

import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class ListResultsFragment extends Fragment implements MediaDataAdapter.OnListItemClickListener{
    public static final String FRAGMENT_KEY = "list_results_fragment";

    private static final String ADAPTER_DATA_LIST_KEY = "adapter_data_list";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String LIST_TYPE_KEY = "list_type";
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String LIST_NAME_KEY = "list_name";

    // TODO: Rename and change types of parameters
    private int mListType;
    private int mMediaType;
    private String mListName;

    private MediaDataAdapter mMediaDataAdapter;
    private SearchPreferences mSearchPreferences;


    private AppDatabase mDatabase;

    private OnFragmentInteractionListener mListener;

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
        RecyclerView mListResultsRecyclerView =
                getView().findViewById(R.id.list_results_recycler_view);

        mListResultsRecyclerView.setLayoutManager(configureLayoutManager());

        mMediaDataAdapter = new MediaDataAdapter(this);
        mListResultsRecyclerView.setAdapter(mMediaDataAdapter);
        mSearchPreferences = new SearchPreferences();
    }

    private void observeData() {
        mDatabase = AppDatabase.getInstance(getContext());

        //if this is a watch list
        if (mListType == ListsParentFragment.LIST_TYPE_WATCH) {
            final String[] watchStatusMoviesTitles = getContext().getResources().getStringArray(R.array.watch_status_movies_titles);
            List<String> titleList = Arrays.asList(watchStatusMoviesTitles);

            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                LiveData<List<MovieData>> listLiveData = mDatabase.movieDataDao()
                        .getMoviesByWatchStatus(titleList.indexOf(mListName));

                listLiveData.observe(getActivity(), new Observer<List<MovieData>>() {
                    @Override
                    public void onChanged(List<MovieData> movieDataList) {
                        mMediaDataAdapter.getAdapterData().clear();
                        mMediaDataAdapter.addAdapterData(movieDataList);

                    }
                });

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                LiveData<List<SeriesData>> listLiveData = mDatabase.seriesDataDao()
                        .getSeriesByWatchStatus(titleList.indexOf(mListName));

                listLiveData.observe(getActivity(), new Observer<List<SeriesData>>() {
                    @Override
                    public void onChanged(List<SeriesData> seriesDataList) {
                        mMediaDataAdapter.getAdapterData().clear();
                        mMediaDataAdapter.addAdapterData(seriesDataList);

                    }
                });

                Log.d(FRAGMENT_KEY, "update watch list");
            }
        }

        //if this is a user list
        if (mListType == ListsParentFragment.LIST_TYPE_USER) {
            //if media data is movie
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                final LiveData<List<MovieData>> moviesInList = mDatabase.movieDataRecordsDao().getAllMoviesInList(mListName);
                moviesInList.observe(getActivity(), new Observer<List<MovieData>>() {
                    @Override
                    public void onChanged(List<MovieData> movieDataList) {
                        mMediaDataAdapter.getAdapterData().clear();
                        mMediaDataAdapter.addAdapterData(movieDataList);

                    }
                });

                //if media data is series
            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
                final LiveData<List<SeriesData>> seriesInList = mDatabase.seriesDataRecordsDao().getAllSeriesInList(mListName);
                seriesInList.observe(getActivity(), new Observer<List<SeriesData>>() {
                    @Override
                    public void onChanged(List<SeriesData> seriesDataList) {
                        mMediaDataAdapter.getAdapterData().clear();
                        mMediaDataAdapter.addAdapterData(seriesDataList);

                    }
                });

                Log.d(FRAGMENT_KEY, "update user list");
            }
        }
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.search_column_span));

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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

        Parcelable parceledData = Parcels.wrap(selectedData);
        Parcelable parceledSharedPreferences = Parcels.wrap(mSearchPreferences);

        DetailsFragmentTemp detailsFragment =
                DetailsFragmentTemp.newInstance(mMediaType, detailUrl, parceledData, parceledSharedPreferences);

        getActivity().getSupportFragmentManager().beginTransaction()
//                .addToBackStack(FRAGMENT_KEY)
                .add(R.id.master_fragments_container, detailsFragment, DetailsFragmentTemp.FRAGMENT_KEY)
                .commit();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ADAPTER_DATA_LIST_KEY, Parcels.wrap(mMediaDataAdapter.getAdapterData()));

    }
}
