package com.upkipp.onmywatch.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.upkipp.onmywatch.MasterActivity;
import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.adapters.AddToListAdapter;
import com.upkipp.onmywatch.database.AppDatabase;
import com.upkipp.onmywatch.models.MediaData;
import com.upkipp.onmywatch.models.MovieData;
import com.upkipp.onmywatch.models.MovieDataRecord;
import com.upkipp.onmywatch.models.SeriesData;
import com.upkipp.onmywatch.models.SeriesDataRecord;
import com.upkipp.onmywatch.models.UserListModel;
import com.upkipp.onmywatch.utils.network_utils.AppExecutors;
import com.upkipp.onmywatch.view_models.AddToListViewModel;
import com.upkipp.onmywatch.view_models.AddToListViewModelFactory;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class AddToListFragment extends Fragment implements AddToListAdapter.OnListItemClickListener,
AddToListAdapter.OnListCheckListener{
    public static String FRAGMENT_KEY = "add_to_list_fragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String MEDIA_TYPE_KEY = "param1";
    private static final String MEDIA_DATA_KEY = "media_data";
    private static final String ID_KEY = "id";

    private static final String CONTAINING_LIST_KEY = "containing_list";
    private static final String CHANGE_LIST_KEY = "containing_list";
    private static final String SELECTED_WATCH_STATUS_KEY = "selected_watch_status";

    // TODO: Rename and change types of parameters
    private int mMediaType;
    private MediaData mMediaData;

    private OnSavePressedActionListener mSaveActionListener;

    private ArrayList<UserListModel> mChangeList;

    private AppDatabase mDatabase;
    private ArrayList<String> mContainingLists;
    private int mSelectedWatchStatus;

    private AddToListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private EditText mSearchEditTextView;
    private RadioGroup mWatchStatusRadioGroup;
    private Button mSaveButton;

    public AddToListFragment() {
        // Required empty public constructor
    }

    public interface OnSavePressedActionListener {
        void onSavePressed();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList(CONTAINING_LIST_KEY, mContainingLists);
        outState.putParcelable(CHANGE_LIST_KEY, Parcels.wrap(mChangeList));
        outState.putInt(SELECTED_WATCH_STATUS_KEY, mSelectedWatchStatus);
    }

    public static AddToListFragment newInstance(int mediaType, Parcelable mediaDataParcelable) {
        AddToListFragment fragment = new AddToListFragment();
        Bundle args = new Bundle();
        args.putInt(MEDIA_TYPE_KEY, mediaType);
        args.putParcelable(MEDIA_DATA_KEY, mediaDataParcelable);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMediaType = getArguments().getInt(MEDIA_TYPE_KEY);
            mMediaData = Parcels.unwrap(getArguments().getParcelable(MEDIA_DATA_KEY));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_to_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();

        AddToListViewModelFactory detailsViewModelFactory =
                new AddToListViewModelFactory(mDatabase, mMediaType, mMediaData.getId());

        final AddToListViewModel viewModel = ViewModelProviders.of(this, detailsViewModelFactory)
                .get(AddToListViewModel.class);

        final LiveData<Integer> watchStatusLiveData = viewModel.getWatchStatus();
        watchStatusLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer watchStatus) {
                watchStatusLiveData.removeObserver(this);

                if (watchStatus != null) {
                    Toast.makeText(getContext(), "not null", Toast.LENGTH_SHORT).show();
                    mWatchStatusRadioGroup.check(mWatchStatusRadioGroup.getChildAt(watchStatus).getId());

                } else {
                    Toast.makeText(getContext(), "null data", Toast.LENGTH_SHORT).show();
                    mWatchStatusRadioGroup.check(mWatchStatusRadioGroup.getChildAt(0).getId());
                }
            }
        });

        //get all user lists
        final LiveData<List<UserListModel>> allUserListLiveData = viewModel.getAllUserLists();
        allUserListLiveData.observe(this, new Observer<List<UserListModel>>() {
            @Override
            public void onChanged(final List<UserListModel> allUserLists) {
                allUserListLiveData.removeObserver(this);

                if (allUserLists != null) {
                    Toast.makeText(getContext(), "user lists not null", Toast.LENGTH_SHORT).show();

                    //get lists containing media
                    final LiveData<List<String>> containingUserListLiveData =viewModel.getContainingLists();

                    containingUserListLiveData.observe(AddToListFragment.this, new Observer<List<String>>() {
                        @Override
                        public void onChanged(List<String> containingLists) {
                            containingUserListLiveData.removeObserver(this);

                            if (containingLists != null) {
                                Toast.makeText(getContext(), "containing lists not null", Toast.LENGTH_SHORT).show();
                                Toast.makeText(getContext(), containingLists.toString(), Toast.LENGTH_SHORT).show();
                                mContainingLists = ((ArrayList<String>) containingLists);

                            } else {
                                Toast.makeText(getContext(), "null containing lists", Toast.LENGTH_SHORT).show();
                                mContainingLists = new ArrayList<>();

                            }

                            mChangeList = new ArrayList<>();

                            //update list so onCheckDatabaseRecords function can run
                            mAdapter.getAdapterData().clear();
                            mAdapter.addAdapterData(allUserLists);
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "null user lists", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    private void defineViews() {
        mDatabase = AppDatabase.getInstance(getContext());
        mRecyclerView = getView().findViewById(R.id.lists_recycler_view);
        mRecyclerView.setLayoutManager(configureLayoutManager());
        mAdapter = new AddToListAdapter(this, mMediaData.getId());
        mRecyclerView.setAdapter(mAdapter);

        mSearchEditTextView = getView().findViewById(R.id.search_edit_text_view);
        mSearchEditTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String listName = s.toString();
                listName = "%" + listName + "%";

                final LiveData<List<UserListModel>> listLiveData = mDatabase.userListsDao().getListsWithNameLike(listName);
                listLiveData.observe(AddToListFragment.this, new Observer<List<UserListModel>>() {
                    @Override
                    public void onChanged(List<UserListModel> userListModels) {
                        listLiveData.removeObserver(this);
                        Toast.makeText(getContext(), "searching", Toast.LENGTH_SHORT).show();
                        mAdapter.getAdapterData().clear();
                        mAdapter.addAdapterData(userListModels);
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mWatchStatusRadioGroup = getView().findViewById(R.id.watch_status_radio_group);

        mWatchStatusRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mSelectedWatchStatus = mWatchStatusRadioGroup.indexOfChild(getView().findViewById(checkedId));

                Log.d(FRAGMENT_KEY , "selected " + String.valueOf(MediaData.getWatchStatusTitle(mSelectedWatchStatus, getContext())));

            }
        });

        mSaveButton = getView().findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateData();

                mSaveActionListener.onSavePressed();
            }
        });
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void updateData() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                int newWatchStatus;


                if (mMediaType == MEDIA_TYPE_MOVIE) {
                    newWatchStatus = updateMovieData();

                } else {
                    newWatchStatus = updateSeriesData();

                }
                Log.d(FRAGMENT_KEY + "002", String.valueOf(mMediaData.getWatchStatus()));

                int netCountChange = updateUserListRecords();
                int newContainingListValue = mContainingLists.size() + netCountChange;
                deleteIfDataNotUsed(newWatchStatus, newContainingListValue);

                Log.d(FRAGMENT_KEY, "final watch status " + String.valueOf(mMediaData.getWatchStatus()));

            }
        });
    }

    private int updateMovieData() {
        //check if movie exists in db
        MovieData movieData = mDatabase.movieDataDao().getMovieById(mMediaData.getId()).getValue();

        //if movie exists
        if (movieData != null) {
            movieData.setWatchStatus(mSelectedWatchStatus);
            mDatabase.movieDataDao().updateMovieData(movieData);
            Log.d(FRAGMENT_KEY, "update media data");
            return movieData.getWatchStatus();

        } else {
            //create new movie data
            MovieData newMovieData = ((MovieData) mMediaData);
            newMovieData.setWatchStatus(mSelectedWatchStatus);
            mDatabase.movieDataDao().addMovieData(newMovieData);
            Log.d(FRAGMENT_KEY, "inserting new media data");
            return newMovieData.getWatchStatus();
        }
    }

    private int updateSeriesData() {
        //check if series exists in db
        SeriesData seriesData = mDatabase.seriesDataDao().getSeriesById(mMediaData.getId()).getValue();

        //if series exists
        if (seriesData != null) {
            seriesData.setWatchStatus(mSelectedWatchStatus);
            mDatabase.seriesDataDao().updateSeriesData(seriesData);
            Log.d(FRAGMENT_KEY, "update media data");
            return seriesData.getWatchStatus();

        } else {
            //create new series data
            SeriesData newSeriesData = ((SeriesData) mMediaData);
            newSeriesData.setWatchStatus(mSelectedWatchStatus);
            mDatabase.seriesDataDao().addSeriesData(newSeriesData);
            Log.d(FRAGMENT_KEY, "inserting new media data");
            return newSeriesData.getWatchStatus();
        }
    }

    private int updateUserListRecords() {
        int netCountChange = 0;

        for (UserListModel userListModel : mChangeList) {
            if (mContainingLists.contains(userListModel.getName())) {
                Log.d(FRAGMENT_KEY, "removing from list");
                removeFromList(userListModel);
                updateListCount(userListModel, -1);

                netCountChange -= 1;

            } else {
                Log.d(FRAGMENT_KEY, "adding to list");
                addToList(userListModel);
                updateListCount(userListModel, +1);

                netCountChange += 1;
            }
        }

        return netCountChange;
    }

    private void deleteIfDataNotUsed(int newWatchStatus, int newContainingListValue) {
        if (newWatchStatus == MediaData.WATCH_STATUS_NONE && newContainingListValue == 0) {
            Log.d(FRAGMENT_KEY, "deleting empty movie data");

            if (mMediaType == MEDIA_TYPE_MOVIE) {
                mDatabase.movieDataDao().deleteMovieData(((MovieData) mMediaData));

            } else if (mMediaType == MEDIA_TYPE_SERIES) {
                mDatabase.seriesDataDao().deleteSeriesData(((SeriesData) mMediaData));

            }
        }
    }

    private void removeFromList(final UserListModel userListModel) {
        if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            //remove record from list
            MovieDataRecord dataRecord = new MovieDataRecord(mMediaData.getId(), userListModel.getName());
            mDatabase.movieDataRecordsDao().deleteRecord((dataRecord));

        } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            //remove record from list
            SeriesDataRecord dataRecord = new SeriesDataRecord(mMediaData.getId(), userListModel.getName());
            mDatabase.seriesDataRecordsDao().deleteRecord((dataRecord));

        }

        Log.d(FRAGMENT_KEY, "deleted record");
    }

    private void addToList(final UserListModel userListModel) {
        if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            //add record to list
            MovieDataRecord dataRecord = new MovieDataRecord(mMediaData.getId(), userListModel.getName());
            mDatabase.movieDataRecordsDao().addRecord(dataRecord);

        } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            //add record to list
            SeriesDataRecord dataRecord = new SeriesDataRecord(mMediaData.getId(), userListModel.getName());
            mDatabase.seriesDataRecordsDao().addRecord(dataRecord);

        }

        Log.d(FRAGMENT_KEY, "add record");
    }

    private void updateListCount(UserListModel userListModel, int factor) {
        //update list count
        userListModel.setItemCount(userListModel.getItemCount() + factor);
        mDatabase.userListsDao().updateListConfiguration(userListModel);
        Log.d(FRAGMENT_KEY, "update list count");
    }

    @Override
    public void onCheckDatabaseRecords(AppCompatCheckBox checkBox, String listName) {
        if (mContainingLists.contains(listName)) {
            checkBox.setChecked(true);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSavePressedActionListener) {
            mSaveActionListener = (OnSavePressedActionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSavePressedAction");
        }
    }

//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }

    //toggles checkbox and toggles changelist values
    @Override
    public void onItemClick(final UserListModel userListModel, AppCompatCheckBox checkBox) {
        Log.d(FRAGMENT_KEY, "1. listModel was in list: " + mChangeList.contains(userListModel));

        //if list model doesn't exist in mChangeList
        if (!mChangeList.contains(userListModel)) {
            //create changed record
            mChangeList.add(userListModel);

        } else {//if name exists in mChangeList
            //delete record
            mChangeList.remove(userListModel);
        }

        Log.d(FRAGMENT_KEY, "2. listModel is in list: " + mChangeList.contains(userListModel));

        //toggle checkbox;
        checkBox.toggle();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
