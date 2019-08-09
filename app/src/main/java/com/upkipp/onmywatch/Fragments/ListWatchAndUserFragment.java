package com.upkipp.onmywatch.Fragments;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.adapters.UserListsAdapter;
import com.upkipp.onmywatch.adapters.WatchListsAdapter;
import com.upkipp.onmywatch.database.AppDatabase;
import com.upkipp.onmywatch.models.ListCounts;
import com.upkipp.onmywatch.models.ListModel;
import com.upkipp.onmywatch.models.UserListModel;
import com.upkipp.onmywatch.models.WatchListModel;
import com.upkipp.onmywatch.utils.network_utils.AppExecutors;
import com.upkipp.onmywatch.view_models.ListsViewModel;

import org.parceler.Parcels;

import java.util.List;

public class ListWatchAndUserFragment extends Fragment implements WatchListsAdapter.OnListItemClickListener,
        UserListsAdapter.OnListItemClickListener, UserListsAdapter.OnSpinnerItemClickListener {

    public static String FRAGMENT_KEY = "list_watch_and_user_fragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //TODO: REPLACE "OBJECT" WITH LISTS OBJECT
    private static final String LIST_TYPE_KEY = "list_type";

    private static final String ADAPTER_DATA_LIST_KEY = "adapter_data_list";

    // TODO: Rename and change types of parameters
    private int mListType;

    private OnFragmentInteractionListener mListener;

    private AppDatabase mDatabase;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView mRecyclerView;

    private FloatingActionButton mFab;

    private static LiveData<Integer> mWatchListCountLiveData;
    private static LiveData<List<UserListModel>> mUserListCountLiveData;

    public ListWatchAndUserFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
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

        defineViews();

        if (savedInstanceState == null) {
            observeData();

            //TODO live data isn't called on restore this will be fixed with view model
        } else {
            List<UserListModel> userListModels =
                    Parcels.unwrap(savedInstanceState.getParcelable(ADAPTER_DATA_LIST_KEY));

            if (mAdapter instanceof WatchListsAdapter) {
                ((WatchListsAdapter) mAdapter).addAdapterData(userListModels);

            } else if (mAdapter instanceof UserListsAdapter) {
                ((UserListsAdapter) mAdapter).addAdapterData(userListModels);

            }
        }
    }

    private void defineViews() {
        mDatabase = AppDatabase.getInstance(getContext());
        mRecyclerView = getView().findViewById(R.id.lists_recycler_view);
        mRecyclerView.setLayoutManager(configureLayoutManager());

        if (mListType == ListsParentFragment.LIST_TYPE_WATCH) {
            mAdapter = new WatchListsAdapter(this);

        } else if (mListType == ListsParentFragment.LIST_TYPE_USER) {
            mAdapter = new UserListsAdapter(this);

        }

        mRecyclerView.setAdapter(mAdapter);

        configureUpNavigationButton();

        mFab = getView().findViewById(R.id.new_list_fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAddToListFragment();
            }
        });
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void observeData() {
        ListsViewModel viewModel = ViewModelProviders.of(this).get(ListsViewModel.class);

        if (mAdapter instanceof WatchListsAdapter) {
            loadWatchLists(viewModel);

        } else if (mAdapter instanceof UserListsAdapter) {
            loadUserLists(viewModel);
        }
    }

    private void configureUpNavigationButton() {
        getView().findViewById(R.id.up_navigation_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                fragmentManager.popBackStack();
            }
        });
    }

    private void loadWatchLists(final ListsViewModel viewModel) {
        viewModel.getWatchLists().observe(getViewLifecycleOwner(), new Observer<List<WatchListModel>>() {
            @Override
            public void onChanged(List<WatchListModel> watchListModels) {
                ((WatchListsAdapter) mAdapter).getAdapterData().clear();
                ((WatchListsAdapter) mAdapter).addAdapterData(watchListModels);

                observeWatchListCounts(viewModel);

                Log.d(FRAGMENT_KEY, "update watch lists");
            }
        });
    }

    private void observeWatchListCounts(ListsViewModel viewModel) {
        SparseArray<LiveData<ListCounts>> countsLiveDataList = viewModel.getWatchStatusCountList();

        for (int index = 0; index< countsLiveDataList.size(); index++) {
            LiveData<ListCounts> listCountLiveData = countsLiveDataList.get(index);

            final int finalIndex = index;
            listCountLiveData.observe(this, new Observer<ListCounts>() {
                @Override
                public void onChanged(ListCounts listCounts) {
                    WatchListModel watchListModel =
                            ((WatchListsAdapter) mAdapter).getAdapterData().get(finalIndex);
                    watchListModel.setItemCount(listCounts.getMoviesCount() + listCounts.getSeriesCount());

                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void loadUserLists(ListsViewModel viewModel) {
        viewModel.getUserLists().observe(getViewLifecycleOwner(), new Observer<List<UserListModel>>() {
            @Override
            public void onChanged(List<UserListModel> userListModels) {
                ((UserListsAdapter) mAdapter).getAdapterData().clear();
                ((UserListsAdapter) mAdapter).addAdapterData(userListModels);
                Log.d(FRAGMENT_KEY, "update user lists");
            }
        });
    }

    private void launchAddToListFragment() {
        getActivity().findViewById(R.id.popup_container).setVisibility(View.VISIBLE);

        CreateListFragment createListFragment =
                CreateListFragment.newInstance();

        getActivity().getSupportFragmentManager().beginTransaction()
//                .addToBackStack(FRAGMENT_KEY)
                .add(R.id.popup_container, createListFragment, CreateListFragment.FRAGMENT_KEY)
                .commit();
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

        Log.d(FRAGMENT_KEY, "detaching");
    }

    @Override
    public void onItemClick(int position) {
        String listName = null;

        if (mAdapter instanceof WatchListsAdapter) {
            listName = ((WatchListsAdapter) mAdapter).getAdapterData().get(position).getName();

        } else if (mAdapter instanceof UserListsAdapter) {
            listName = ((UserListsAdapter) mAdapter).getAdapterData().get(position).getName();

        }

        Fragment fragment = ListResultsParentFragment.newInstance(mListType, listName);

        getActivity().getSupportFragmentManager().beginTransaction()
//                .addToBackStack(FRAGMENT_KEY)
                .add(R.id.master_fragments_container, fragment, ListResultsParentFragment.FRAGMENT_KEY)
                .commit();
    }

    @Override
    public void onEditClick() {
        launchAddToListFragment();
    }

    @Override
    public void onDeleteClick(final UserListModel userListModel) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                    mDatabase.userListsDao().deleteList(new UserListModel(userListModel.getName()));
            }
        });
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mAdapter instanceof WatchListsAdapter) {
            outState.putParcelable(ADAPTER_DATA_LIST_KEY, Parcels.wrap(((WatchListsAdapter) mAdapter).getAdapterData()));

        } else if (mAdapter instanceof UserListsAdapter) {
            outState.putParcelable(ADAPTER_DATA_LIST_KEY, Parcels.wrap(((UserListsAdapter) mAdapter).getAdapterData()));

        }
    }
}


