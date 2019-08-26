package com.atmko.onmywatch.Fragments;

import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.AddToListAdapter;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.view_models.AddToListViewModel;
import com.atmko.onmywatch.view_models.AddToListViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class AddToListFragment extends Fragment implements AddToListAdapter.OnListItemClickListener,
AddToListAdapter.OnListCheckListener{
    public static String FRAGMENT_KEY = "add_to_list_fragment";

    // the fragment initialization parameter keys
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String MEDIA_DATA_KEY = "media_data";

    //save instance keys
    private static final String NEW_CONTAINING_LIST_KEY = "new_containing_list";
    private static final String SELECTED_WATCH_STATUS_KEY = "selected_watch_status";

    // the fragment initialization parameters
    private int mMediaType;
    private MediaData mMediaData;

    //fragment values
    private Bundle mSavedInstanceState;
    private AppDatabase mDatabase;
    private AddToListAdapter mAdapter;
    private int mSelectedWatchStatus;
    private ArrayList<UserListModel> mOriginalContainingLists;
    private ArrayList<UserListModel> mNewContainingLists;

    private RecyclerView mRecyclerView;
    private EditText mSearchEditTextView;
    private RadioGroup mWatchStatusRadioGroup;
    private Button mSaveButton;

    //interfaces
    private OnSavePressedActionListener mSaveActionListener;

    public AddToListFragment() {
        // Required empty public constructor
    }

    public interface OnSavePressedActionListener {
        void onSavePressed();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(NEW_CONTAINING_LIST_KEY, Parcels.wrap(mNewContainingLists));
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

        //save saveInstanceState value for onCreateAnimator and mNewContainingLists to check if
        // this is the first instance
        mSavedInstanceState = savedInstanceState;

        defineViews();

        //observe view model data
        observeViewModel();
    }

    private void defineViews() {
        mDatabase = AppDatabase.getInstance(getContext());

        //configure recycler view
        mRecyclerView = getView().findViewById(R.id.lists_recycler_view);
        mRecyclerView.setLayoutManager(configureLayoutManager());
        mAdapter = new AddToListAdapter(this, mMediaData.getId());
        mRecyclerView.setAdapter(mAdapter);

        //configure search box
        mSearchEditTextView = getView().findViewById(R.id.search_edit_text_view);
        mSearchEditTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String listName = s.toString();
                listName = "%" + listName + "%";

                //observe lists with searched name then remove observer
                final LiveData<List<UserListModel>> listLiveData = mDatabase.userListsDao().getListsWithNameLike(listName);
                listLiveData.observe(AddToListFragment.this, new Observer<List<UserListModel>>() {
                    @Override
                    public void onChanged(List<UserListModel> userListModels) {
                        listLiveData.removeObserver(this);

                        mAdapter.getAdapterData().clear();
                        mAdapter.addAdapterData(userListModels);
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //configure watch status selector
        mWatchStatusRadioGroup = getView().findViewById(R.id.watch_status_radio_group);
        mWatchStatusRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mSelectedWatchStatus = mWatchStatusRadioGroup.indexOfChild(getView().findViewById(checkedId));

                Log.d(FRAGMENT_KEY , "selected watchStatus:  " + String.valueOf(MediaData.getWatchStatusTitle(mSelectedWatchStatus, getContext())));
            }
        });

        //configure save button
        mSaveButton = getView().findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateData();

                mSaveActionListener.onSavePressed();
            }
        });
    }

    private void observeViewModel() {
        //create view model
        AddToListViewModelFactory detailsViewModelFactory =
                new AddToListViewModelFactory(mDatabase, mMediaType, mMediaData.getId());

        final AddToListViewModel viewModel = ViewModelProviders.of(this, detailsViewModelFactory)
                .get(AddToListViewModel.class);

        //observe live data of media's watch status
        final LiveData<Integer> watchStatusLiveData = viewModel.getWatchStatus();
        watchStatusLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer watchStatus) {
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

        //observe live data of all of user's lists
        final LiveData<List<UserListModel>> allUserListLiveData = viewModel.getAllUserLists();
        allUserListLiveData.observe(this, new Observer<List<UserListModel>>() {
            @Override
            public void onChanged(final List<UserListModel> allUserLists) {
                //if user list(s) exist
                if (allUserLists != null) {
                    //observe live data of lists containing media
                    final LiveData<List<UserListModel>> containingUserListLiveData =
                            viewModel.getContainingLists();

                    containingUserListLiveData.observe(AddToListFragment.this, new Observer<List<UserListModel>>() {
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

                            //update list so onCheckDatabaseRecords function can run
                            mAdapter.getAdapterData().clear();
                            mAdapter.addAdapterData(allUserLists);
                        }
                    });
                //else if no user list(s) exist
                } else {
                    Snackbar.make(getActivity().findViewById(R.id.top_layout),
                            getString(R.string.no_created_lists_message), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private LinearLayoutManager configureLayoutManager() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

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

                MediaNotifier mediaNotifier = getMediaNotifier();

                if (newWatchStatus == MediaData.WATCH_STATUS_TO_WATCH
                        || newWatchStatus == MediaData.WATCH_STATUS_WATCHING
                        || newWatchStatus == MediaData.WATCH_STATUS_WATCHED) {

                    if (mediaNotifier == null) {
                        createMediaNotifier();
                    }

                } else if (newWatchStatus == MediaData.WATCH_STATUS_NONE
                        || newWatchStatus == MediaData.WATCH_STATUS_DROPPED){
                    if (mediaNotifier != null) {
                        deleteMediaNotifier(mediaNotifier);
                    }
                }

                //TODO observe user list counts by view model instead of manually through netCountChange
                int netCountChange = updateUserListRecords();
                int newContainingListValue = mOriginalContainingLists.size() + netCountChange;

                deleteMediaDataIfDataNotUsed(newWatchStatus, newContainingListValue);
            }
        });
    }

    private int updateMovieData() {
        //check if movie exists in db
        MovieData movieData = mDatabase.movieDataDao().getMovieByIdAlt(mMediaData.getId());

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
            Log.d(FRAGMENT_KEY, "creating new media data");
            return newMovieData.getWatchStatus();
        }
    }

    private int updateSeriesData() {
        //check if series exists in db
        SeriesData seriesData = mDatabase.seriesDataDao().getSeriesByIdAlt(mMediaData.getId());

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
            Log.d(FRAGMENT_KEY, "creating new media data");
            return newSeriesData.getWatchStatus();
        }
    }

    private MediaNotifier getMediaNotifier() {
        MediaNotifier mediaNotifier;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            mediaNotifier = mDatabase.movieNotifierDao().getNotifierByIdAlt(mMediaData.getId());

        } else {
            mediaNotifier = mDatabase.seriesNotifierDao().getNotifierByIdAlt(mMediaData.getId());
        }

        return mediaNotifier;
    }

    private MediaNotifier createMediaNotifier() {
        MediaNotifier mediaNotifier;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            mediaNotifier =
                    new MovieNotifier(getContext(), mMediaData.getId());
            mediaNotifier.setConditionValue(MediaNotifier.CONDITION_ON_RELEASE_KEY, true);
            mediaNotifier.setConditionValue(MediaNotifier.CONDITION_NEW_TRAILER_KEY, true);

            mDatabase.movieNotifierDao().addMovieNotifier(((MovieNotifier) mediaNotifier));

        } else {
            mediaNotifier =
                    new SeriesNotifier(getContext(), mMediaData.getId());
            mediaNotifier.setConditionValue(MediaNotifier.CONDITION_ON_RELEASE_KEY, true);
            mediaNotifier.setConditionValue(MediaNotifier.CONDITION_NEW_TRAILER_KEY, true);

            mDatabase.seriesNotifierDao().addSeriesNotifier(((SeriesNotifier) mediaNotifier));
        }

        Log.d(FRAGMENT_KEY, "media notifier created");

        return mediaNotifier;

    }

    private int updateUserListRecords() {
        int netCountChange = 0;

        for (UserListModel userListModel : mNewContainingLists) {
            if (!mOriginalContainingLists.contains(userListModel)) {
                //add the media to the list
                Log.d(FRAGMENT_KEY, "adding to list");
                addToList(userListModel);
                updateListCount(userListModel, +1);

                netCountChange += 1;
            }
        }

        for (UserListModel userListModel : mOriginalContainingLists) {
            if (!mNewContainingLists.contains(userListModel)) {
                //remove the media from the list
                Log.d(FRAGMENT_KEY, "removing from list");
                removeFromList(userListModel);
                updateListCount(userListModel, -1);

                netCountChange -= 1;
            }
        }

        return netCountChange;
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

    private void deleteMediaDataIfDataNotUsed(int newWatchStatus, int newContainingListValue) {
        //if watch status is none and if there are no lists containing this media
        if (newWatchStatus == MediaData.WATCH_STATUS_NONE && newContainingListValue == 0) {
            //delete the media from the database
            Log.d(FRAGMENT_KEY, "deleting empty media data");

            if (mMediaType == MEDIA_TYPE_MOVIE) {
                mDatabase.movieDataDao().deleteMovieData(((MovieData) mMediaData));

            } else if (mMediaType == MEDIA_TYPE_SERIES) {
                mDatabase.seriesDataDao().deleteSeriesData(((SeriesData) mMediaData));

            }
        }
    }

    private void deleteMediaNotifier(MediaNotifier mediaNotifier) {
        if (mMediaType == MEDIA_TYPE_MOVIE) {
            mDatabase.movieNotifierDao().deleteMovieNotifier(((MovieNotifier) mediaNotifier));

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            mDatabase.seriesNotifierDao().deleteSeriesNotifier(((SeriesNotifier) mediaNotifier));

        }

        Log.d(FRAGMENT_KEY, "media notifier deleted");
    }

    @Override
    public void onCheckDatabaseRecords(AppCompatCheckBox checkBox, UserListModel userListModel) {
        if (mNewContainingLists.contains(userListModel)) {
            checkBox.setChecked(true);

        } else {
            checkBox.setChecked(false);
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

    @Override
    public void onDetach() {
        super.onDetach();
        mSaveActionListener = null;
    }

    //toggles checkbox and updates mNewContainingLists membership
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
