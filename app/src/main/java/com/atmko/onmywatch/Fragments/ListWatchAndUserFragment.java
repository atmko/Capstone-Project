package com.atmko.onmywatch.Fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.UserListsAdapter;
import com.atmko.onmywatch.adapters.WatchListsAdapter;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.ListCounts;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.view_models.ListsViewModel;

import org.parceler.Parcels;

import java.util.List;

public class ListWatchAndUserFragment extends Fragment implements WatchListsAdapter.OnListItemClickListener,
        UserListsAdapter.OnListItemClickListener, UserListsAdapter.OnSpinnerItemClickListener {

    public static String FRAGMENT_KEY = "list_watch_and_user_fragment";

    //fragment initialization parameters
    private static final String LIST_TYPE_KEY = "list_type";
    private static final String ADAPTER_DATA_LIST_KEY = "adapter_data_list";
    private int mListType;

    //post initialization parameters
    private static final String SEARCH_TEXT_KEY = "search_text";
    private static final String SEARCH_BAR_VISIBILITY_KEY = "visible_search_bar";

    //check for restoring state
    private boolean mFirstInit = true;
    private Bundle mSavedInstanceState;
    private AppDatabase mDatabase;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView mRecyclerView;

    private FloatingActionButton mFab;
    private EditText mSearchTextView;


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
                    launchCreateListFragment(CreateListFragment.MODE_CREATE, "", 0);
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

    //value restore convenience method
    private void restoreSavedSearch(Bundle savedInstanceState) {
        mFirstInit = false;

        String savedSearchText = savedInstanceState.getString(SEARCH_TEXT_KEY);
        int savedBarVisibility = savedInstanceState.getInt(SEARCH_BAR_VISIBILITY_KEY);
        if (savedBarVisibility == View.VISIBLE) {
            getParentFragment()
                    .getView().findViewById(R.id.title_text_view).setVisibility(View.GONE);
        }
        mSearchTextView.setText(savedSearchText);
        mSearchTextView.setVisibility(savedBarVisibility);
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

    private void loadWatchLists(final ListsViewModel viewModel) {
        viewModel.getWatchLists().observe(getViewLifecycleOwner(), new Observer<List<WatchListModel>>() {
            @Override
            public void onChanged(List<WatchListModel> watchListModels) {
                ((WatchListsAdapter) mAdapter).getAdapterData().clear();
                ((WatchListsAdapter) mAdapter).addAdapterData(watchListModels);

                observeWatchListCounts(viewModel);

                //restore values to views but only
                //if saved state exists
                //&& if this is the first run
                if (mSavedInstanceState != null && mFirstInit) {
                    restoreSavedSearch(mSavedInstanceState);

                }

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
                    int moviesCount;
                    int seriesCount;

                    if (listCounts != null) {
                        moviesCount = listCounts.getMoviesCount();
                        seriesCount = listCounts.getSeriesCount();

                    } else {
                        moviesCount = 0;
                        seriesCount = 0;
                    }

                    WatchListModel watchListModel =
                            ((WatchListsAdapter) mAdapter).getAdapterData().get(finalIndex);
                    watchListModel.setItemCount(moviesCount + seriesCount);

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

                //restore values to views but only
                //if saved state exists
                //&& if this is the first run
                if (mSavedInstanceState != null && mFirstInit) {
                    restoreSavedSearch(mSavedInstanceState);

                }

                Log.d(FRAGMENT_KEY, "update user lists");
            }
        });
    }

    private void onSearchTextChanged(CharSequence searchText) {
        String listName = searchText.toString();
        listName = "%" + listName + "%";

        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            //observe lists with searched name then remove observer
            final LiveData<List<WatchListModel>> listLiveData = mDatabase.watchListsDao().getListsWithNameLike(listName);
            listLiveData.observe(getParentFragment(), new Observer<List<WatchListModel>>() {
                @Override
                public void onChanged(List<WatchListModel> watchListModels) {
                    listLiveData.removeObserver(this);

                    ((WatchListsAdapter) mAdapter).getAdapterData().clear();
                    ((WatchListsAdapter) mAdapter).addAdapterData(watchListModels);
                }
            });
        }

        if (mListType == ListsWatchAndUserParentFragment.LIST_TYPE_USER) {
            //observe lists with searched name then remove observer
            final LiveData<List<UserListModel>> listLiveData = mDatabase.userListsDao().getListsWithNameLike(listName);
            listLiveData.observe(getParentFragment(), new Observer<List<UserListModel>>() {
                @Override
                public void onChanged(List<UserListModel> userListModels) {
                    listLiveData.removeObserver(this);

                    ((UserListsAdapter) mAdapter).getAdapterData().clear();
                    ((UserListsAdapter) mAdapter).addAdapterData(userListModels);
                }
            });
        }
    }

    private void launchCreateListFragment(int mode, String listName, int itemCount) {
        getParentFragment().getActivity().findViewById(R.id.popup_container).setVisibility(View.VISIBLE);

        CreateListFragment createListFragment =
                CreateListFragment.newInstance(mode, listName, itemCount);

        getParentFragment().getActivity().getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.popup_container, createListFragment, CreateListFragment.FRAGMENT_KEY)
                .commit();
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

        getParentFragment().getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                .add(R.id.master_fragments_container, fragment, ListResultsParentFragment.FRAGMENT_KEY)
                .commit();
    }

    @Override
    public void onEditClick(UserListModel userListModel) {
        launchCreateListFragment(CreateListFragment.MODE_EDIT,
                userListModel.getName(), userListModel.getItemCount());
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //TODO restoring obsolete due to view model and database reading
        if (mAdapter instanceof WatchListsAdapter) {
            outState.putParcelable(ADAPTER_DATA_LIST_KEY, Parcels.wrap(((WatchListsAdapter) mAdapter).getAdapterData()));

        } else if (mAdapter instanceof UserListsAdapter) {
            outState.putParcelable(ADAPTER_DATA_LIST_KEY, Parcels.wrap(((UserListsAdapter) mAdapter).getAdapterData()));

        }

        //save search bar text
        outState.putString(SEARCH_TEXT_KEY, mSearchTextView.getText().toString());

        //save search bar visibility
        outState.putInt(SEARCH_BAR_VISIBILITY_KEY, mSearchTextView.getVisibility());
    }
}


