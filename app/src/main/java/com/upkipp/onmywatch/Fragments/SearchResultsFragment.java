package com.upkipp.onmywatch.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.stack.PagingBlock;
import com.atmko.stack.Stack;
import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.adapters.MediaDataAdapter;
import com.upkipp.onmywatch.adapters.PeopleDataAdapter;
import com.upkipp.onmywatch.models.MediaData;
import com.upkipp.onmywatch.models.PersonData;
import com.upkipp.onmywatch.utils.MovieDataParser;
import com.upkipp.onmywatch.utils.PersonDataParser;
import com.upkipp.onmywatch.utils.SearchPreferences;
import com.upkipp.onmywatch.utils.SeriesDataParser;
import com.upkipp.onmywatch.utils.network_utils.NetworkFunctions;

import org.json.JSONException;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_PEOPLE;

public class SearchResultsFragment extends Fragment implements
        MediaDataAdapter.OnListItemClickListener,
        PeopleDataAdapter.OnListItemClickListener{

    public static String FRAGMENT_KEY = "search_results_fragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String SEARCH_URL_KEY = "search_url";
    private static final String SEARCH_PREFERENCES_KEY = "search_preferences";

    // TODO: Rename and change types of parameters
    private int mMediaType;
    private String mSearchUrl;

    private static final String ADAPTER_DATA_LIST_KEY = "adapter_data_list";
    private static final String PAGING_BLOCK_MAP_KEY = "paging_block_map";

    private RecyclerView.Adapter mDataAdapter;
    private Stack stack;
    private SearchPreferences mSearchPreferences;

//    private OnFragmentInteractionListener mListener;

    public SearchResultsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param searchUrl Parameter 2.
     * @return A new instance of fragment SearchResultsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SearchResultsFragment newInstance(int mediaType, String searchUrl,
                                                    SearchPreferences searchPreferencesParcel) {
        SearchResultsFragment fragment = new SearchResultsFragment();
        Bundle args = new Bundle();
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

        Log.d(FRAGMENT_KEY, "checking");

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
        }, 20, 3);

        RecyclerView recyclerView = getView().findViewById(R.id.search_results_recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());

        if (mMediaType == MEDIA_TYPE_PEOPLE) {
            mDataAdapter = new PeopleDataAdapter(this);

        } else {
            mDataAdapter = new MediaDataAdapter(this);
        }

        recyclerView.setAdapter(mDataAdapter);
        stack = new Stack(false, 2, pagingBlockTemplate, recyclerView, mDataAdapter);
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
        ANRequest request = NetworkFunctions.agnosticSearchRequest(mSearchUrl, mSearchPreferences, getActivity());

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

//                    //if two pane
//                    //and if this is first time loading fragment i.e if SearchFragment's saved instance state == null)
//                    if (mIsTwoPane && isFirstInit) {
//                        loadDetailFragment();
//                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ANError anError) {
                //notify error
//                Snackbar.make(mRootView.findViewById(R.id.topLayout),
//                        anError.getErrorDetail(), Snackbar.LENGTH_LONG).show();

                Toast.makeText(getContext(), String.valueOf(anError.getErrorCode()), Toast.LENGTH_SHORT).show();

            }
        });
    }

    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }

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

//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }

    @Override
    public void onItemClick(int position) {
        String[] detailUrls = getContext().getResources().getStringArray(R.array.details_urls);
        String detailUrl = null;
        MediaData selectedData = ((MediaDataAdapter) mDataAdapter).getAdapterData().get(position);

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

//    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
//    }
}
