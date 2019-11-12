/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.adapters.AddToListAdapter;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.view_models.AddToListViewModel;
import com.atmko.onmywatch.view_models.AddToListViewModelFactory;
import com.atmko.onmywatch.view_models.FirebaseAddToListViewModel;
import com.google.android.material.snackbar.Snackbar;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class AddToListActivity extends AppCompatActivity implements AddToListAdapter.OnListItemClickListener,
        AddToListAdapter.OnListCheckListener{
    private static final String TAG = "add_to_list_activity";

    public static final String MEDIA_TYPE_KEY = "media_type";
    public static final String MEDIA_DATA_KEY = "media_data";

    //save instance keys
    private static final String OLD_WATCH_STATUS_KEY = "old_watch_status";
    private static final String NEW_CONTAINING_LIST_KEY = "new_containing_list";
    private static final String SELECTED_WATCH_STATUS_KEY = "selected_watch_status";

    private int mMediaType;
    private MediaData mMediaData;

    private Bundle mSavedInstanceState;
    private AppDatabase mDatabase;
    private AddToListAdapter mAdapter;
    private Integer mOldWatchStatus;
    private Integer mNewWatchStatus;
    private int mSelectedWatchStatus;
    private ArrayList<UserListModel> mOriginalContainingLists;
    private ArrayList<UserListModel> mNewContainingLists;

    private RecyclerView mRecyclerView;
    private EditText mSearchEditTextView;
    private RadioGroup mWatchStatusRadioGroup;
    private Button mSaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_to_list);

        //configure percentage of display dialog activity takes
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int height = displayMetrics.heightPixels *
                getResources().getInteger(R.integer.add_to_list_activity_popup_screen_percent) / 100;

        int width = displayMetrics.widthPixels *
                getResources().getInteger(R.integer.add_to_list_activity_popup_screen_percent) / 100;

        getWindow().setLayout(width, height);

        Intent intent = getIntent();
        mMediaType = intent.getIntExtra(MEDIA_TYPE_KEY, 0);
        mMediaData = Parcels.unwrap(intent.getParcelableExtra(MEDIA_DATA_KEY));

        //save saveInstanceState value for onCreateAnimator and mNewContainingLists to check if
        // this is the first instance
        mSavedInstanceState = savedInstanceState;

        defineViews();

        //observe view model data
        observeViewModel();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(NEW_CONTAINING_LIST_KEY, Parcels.wrap(mNewContainingLists));
        outState.putInt(SELECTED_WATCH_STATUS_KEY, mSelectedWatchStatus);
    }

    private void defineViews() {
        mDatabase = AppDatabase.getInstance(this);

        //configure recycler view
        mRecyclerView = findViewById(R.id.lists_recycler_view);
        mRecyclerView.setLayoutManager(configureLayoutManager());
        mAdapter = new AddToListAdapter(this);

        //configure search box
        mSearchEditTextView = findViewById(R.id.search_edit_text_view);
        mSearchEditTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //TODO: implement search for pro mode
                if (!MasterActivity.isProMode()) {
                    String listName = s.toString();
                    listName = "%" + listName + "%";

                    //observe lists with searched name then remove observer
                    final LiveData<List<UserListModel>> listLiveData = mDatabase.userListsDao().getListsWithNameLike(listName);
                    listLiveData.observe(AddToListActivity.this, new Observer<List<UserListModel>>() {
                        @Override
                        public void onChanged(List<UserListModel> userListModels) {
                            listLiveData.removeObserver(this);

                            mAdapter.getAdapterData().clear();
                            mAdapter.addAdapterData(userListModels);
                        }
                    });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //configure watch status selector
        mWatchStatusRadioGroup = findViewById(R.id.watch_status_radio_group);
        mWatchStatusRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mSelectedWatchStatus = mWatchStatusRadioGroup.indexOfChild(findViewById(checkedId));

            }
        });

        //configure save button
        mSaveButton = findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MasterActivity.isProMode()) {
                    AddToListFirebaseHelper.saveFirebaseData(
                            AddToListActivity.this,
                            mMediaData,
                            mMediaType,
                            mSelectedWatchStatus,
                            mOriginalContainingLists,
                            mNewContainingLists);

                } else {
                    updateData();

                }

                //exit activity
                finish();
            }
        });
    }

    private void observeViewModel() {
        final ViewModel viewModel;
        final LiveData<Integer> watchStatusLiveData;
        final LiveData<List<UserListModel>> allUserListLiveData;
        final LiveData<List<UserListModel>> containingUserListLiveData;

        AddToListViewModelFactory addToListViewModelFactory =
                new AddToListViewModelFactory(mDatabase, mMediaType, mMediaData.getId());

        if (MasterActivity.isProMode()) {
            //create view model
            viewModel = ViewModelProviders.of(this, addToListViewModelFactory)
                    .get(FirebaseAddToListViewModel.class);

            watchStatusLiveData = ((FirebaseAddToListViewModel) viewModel).getWatchStatus();
            allUserListLiveData = ((FirebaseAddToListViewModel) viewModel).getAllUserLists();
            containingUserListLiveData = ((FirebaseAddToListViewModel) viewModel).getContainingLists();

        } else {
            //create view model
            viewModel = ViewModelProviders.of(this, addToListViewModelFactory)
                    .get(AddToListViewModel.class);

            watchStatusLiveData = ((AddToListViewModel) viewModel).getWatchStatus();
            allUserListLiveData = ((AddToListViewModel) viewModel).getAllUserLists();
            containingUserListLiveData = ((AddToListViewModel) viewModel).getContainingLists();
        }

        //observe live data of media's watch status
        watchStatusLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer watchStatus) {
                mOldWatchStatus = watchStatus;

                //if mSavedInstanceState is null
                if (mSavedInstanceState == null) {

                    //if status exists
                    if (watchStatus != null) {
                        //check status
                        mWatchStatusRadioGroup.check(mWatchStatusRadioGroup.getChildAt(watchStatus).getId());

                    } else {//if status doesn't exist
                        //check "none" status
                        mWatchStatusRadioGroup.check(mWatchStatusRadioGroup.getChildAt(0).getId());

                    }

                } else {//if mSavedInstanceState exists select saved value
                    mSelectedWatchStatus =
                            mSavedInstanceState.getInt(SELECTED_WATCH_STATUS_KEY, 0);

                    mWatchStatusRadioGroup
                            .check(mWatchStatusRadioGroup
                                    .getChildAt(mSelectedWatchStatus).getId());
                }
            }
        });

        //TODO: fix bug where lists disappear on rotate
        //observe live data of all of user's lists
        allUserListLiveData.observe(this, new Observer<List<UserListModel>>() {
            @Override
            public void onChanged(final List<UserListModel> allUserLists) {
                //if user list(s) exist
                if (allUserLists != null) {
                    //observe live data of lists containing media
                    containingUserListLiveData.observe(AddToListActivity.this,
                            new Observer<List<UserListModel>>() {
                                @Override
                                public void onChanged(List<UserListModel> containingLists) {
                                    //if media is contained in user list(s)
                                    if (containingLists != null) {
                                        mOriginalContainingLists = ((ArrayList<UserListModel>) containingLists);

                                    } else {
                                        mOriginalContainingLists = new ArrayList<>();

                                    }

                                    //define mNewContainingLists
                                    if (mSavedInstanceState == null) {
                                        mNewContainingLists = new ArrayList<>(mOriginalContainingLists);

                                    } else {
                                        mNewContainingLists =
                                                Parcels.unwrap(mSavedInstanceState
                                                        .getParcelable(NEW_CONTAINING_LIST_KEY));
                                    }

                                    //setting adapter here avoids null pointer crash when restoring app...
                                    // after app is killed
                                    mRecyclerView.setAdapter(mAdapter);

                                    //update list so onCheckDatabaseRecords function can run
                                    mAdapter.getAdapterData().clear();
                                    mAdapter.addAdapterData(allUserLists);
                                }
                            });
                    //else if no user list(s) exist
                } else {
                    //TODO: null value may never be triggered because Live Data returns empty list instead of null
                    Snackbar.make(findViewById(R.id.top_layout),
                            getString(R.string.no_created_lists_message), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private LinearLayoutManager configureLayoutManager() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void updateData() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                MediaData newMediaData;

                if (mMediaType == MEDIA_TYPE_MOVIE) {
                    newMediaData = updateMovieData();

                } else {
                    newMediaData = updateSeriesData();

                }

                int uerListNetCountChange = updateUserListRecords();
                int newContainingListValue = mOriginalContainingLists.size() + uerListNetCountChange;

                boolean isDeleted = deleteMediaDataIfDataNotUsed(newMediaData.getWatchStatus(),
                        newContainingListValue);

                mNewWatchStatus = isDeleted? null : newMediaData.getWatchStatus();

                updateWatchListCounts();
            }
        });
    }

    private MovieData updateMovieData() {
        //check if movie exists in db
        MovieData movieData = mDatabase.movieDataDao().getMovieByIdAlt(mMediaData.getId());

        //if movie exists
        if (movieData != null) {
            movieData.setWatchStatus(mSelectedWatchStatus);
            mDatabase.movieDataDao().updateMovieData(movieData);
            Log.d(TAG, "update media data");
            return movieData;

        } else {
            //create new movie data
            MovieData newMovieData = ((MovieData) mMediaData);
            newMovieData.setWatchStatus(mSelectedWatchStatus);
            mDatabase.movieDataDao().addMovieData(newMovieData);
            Log.d(TAG, "creating new media data");
            return newMovieData;
        }
    }

    private SeriesData updateSeriesData() {
        //check if series exists in db
        SeriesData seriesData = mDatabase.seriesDataDao().getSeriesByIdAlt(mMediaData.getId());

        //if series exists
        if (seriesData != null) {
            seriesData.setWatchStatus(mSelectedWatchStatus);
            mDatabase.seriesDataDao().updateSeriesData(seriesData);
            Log.d(TAG, "update media data");
            return seriesData;

        } else {
            //create new series data
            SeriesData newSeriesData = ((SeriesData) mMediaData);
            newSeriesData.setWatchStatus(mSelectedWatchStatus);
            mDatabase.seriesDataDao().addSeriesData(newSeriesData);
            Log.d(TAG, "creating new media data");
            return newSeriesData;
        }
    }

    private int updateUserListRecords() {
        int netCountChange = 0;

        for (UserListModel userListModel : mNewContainingLists) {
            if (!mOriginalContainingLists.contains(userListModel)) {
                //add the media to the list
                Log.d(TAG, "adding to list");
                addToList(userListModel);
                updateListCount(userListModel, +1);

                netCountChange += 1;
            }
        }

        for (UserListModel userListModel : mOriginalContainingLists) {
            if (!mNewContainingLists.contains(userListModel)) {
                //remove the media from the list
                Log.d(TAG, "removing from list");
                removeFromList(userListModel);
                updateListCount(userListModel, -1);

                netCountChange -= 1;
            }
        }

        return netCountChange;
    }

    private void updateWatchListCounts() {
        //get watch status names
        //get watch status lists
        //update counts
        //update lists
        if (mOldWatchStatus != null) {
            String oldWatchStatusName = MediaData.getWatchStatusTitle(mOldWatchStatus,
                    getApplicationContext());

            WatchListModel oldWatchStatusList =
                    mDatabase.watchListsDao().getListByNameAlt(oldWatchStatusName);

            oldWatchStatusList.setItemCount(oldWatchStatusList.getItemCount() - 1);
            mDatabase.watchListsDao().updateListConfiguration(oldWatchStatusList);
        }

        if (mNewWatchStatus != null) {
            String newWatchStatusName = MediaData.getWatchStatusTitle(mNewWatchStatus,
                    getApplicationContext());

            WatchListModel newWatchStatusList =
                    mDatabase.watchListsDao().getListByNameAlt(newWatchStatusName);

            newWatchStatusList.setItemCount(newWatchStatusList.getItemCount() + 1);
            mDatabase.watchListsDao().updateListConfiguration(newWatchStatusList);
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

        Log.d(TAG, "deleted record");
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

        Log.d(TAG, "add record");
    }

    private void updateListCount(UserListModel userListModel, int factor) {
        //update list count
        userListModel.setItemCount(userListModel.getItemCount() + factor);
        mDatabase.userListsDao().updateListConfiguration(userListModel);
        Log.d(TAG, "update list count");
    }

    private boolean deleteMediaDataIfDataNotUsed(int newWatchStatus, int newContainingListValue) {
        //if watch status is none and if there are no lists containing this media
        if (newWatchStatus == MediaData.WATCH_STATUS_NONE
                && newContainingListValue == 0) {
            //delete the media from the database
            Log.d(TAG, "deleting empty media data");

            if (mMediaType == MEDIA_TYPE_MOVIE) {
                mDatabase.movieDataDao().deleteMovieData(((MovieData) mMediaData));

            } else if (mMediaType == MEDIA_TYPE_SERIES) {
                mDatabase.seriesDataDao().deleteSeriesData(((SeriesData) mMediaData));

            }

            return true;
        }

        return false;
    }

    @Override
    public void onCheckDatabaseRecords(AppCompatCheckBox checkBox, UserListModel userListModel) {
        if (mNewContainingLists.contains(userListModel)) {
            checkBox.setChecked(true);

        } else {
            checkBox.setChecked(false);
        }
    }

    //TODO: modifying newContainingList here makes getting newContainingList value when saving redundant
    //avoids inconsistent checks when recycler view recycles views
    @Override
    public void onItemClick(final UserListModel userListModel, AppCompatCheckBox checkBox) {
        //if list model doesn't exist in mNewContainingLists
        if (!mNewContainingLists.contains(userListModel)) {
            //add list
            mNewContainingLists.add(userListModel);

        } else {//if name exists in mNewContainingLists
            //remove from list
            mNewContainingLists.remove(userListModel);
        }

        //toggle checkbox;
        checkBox.toggle();
    }
}
