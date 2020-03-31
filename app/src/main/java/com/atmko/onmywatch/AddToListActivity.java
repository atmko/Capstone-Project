/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.IdlingResource;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.core.MainThreadExecutor;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.onmywatch.adapters.AddToListAdapter;
import com.atmko.onmywatch.adapters.TagAdapter;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.SearchMediaTag;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.SimpleIdlingResource;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.UpdateNotifierService;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.MovieDataParser;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;
import com.atmko.onmywatch.utils.api_utils.SeriesDataParser;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker;
import com.atmko.onmywatch.view_models.AddToListViewModel;
import com.atmko.onmywatch.view_models.AddToListViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.MasterActivity.SEARCH_TEXT_KEY;
import static com.atmko.onmywatch.MasterActivity.hideSoftKeyboard;
import static com.atmko.onmywatch.MasterActivity.showSoftKeyboard;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;
import static com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker.NEW_MEDIA_DATA_KEY;

public class AddToListActivity extends AppCompatActivity implements AddToListAdapter.OnListItemClickListener,
        AddToListAdapter.OnListCheckListener{
    private static final String TAG = "add_to_list_activity";

    public static final String MEDIA_TYPE_KEY = "media_type";
    public static final String MEDIA_DATA_KEY = "media_data";

    //save instance keys
    private static final String NEW_CONTAINING_LIST_KEY = "new_containing_list";
    private static final String SELECTED_WATCH_STATUS_KEY = "selected_watch_status";

    private int mMediaType;
    private MediaData mMediaData;
    private MediaData mSavedMedia;

    private Bundle mSavedInstanceState;
    private AppDatabase mDatabase;
    private AddToListAdapter mAdapter;
    private TagAdapter tagAdapter;
    private Integer mOldWatchStatus;
    private int mSelectedWatchStatus;
    private Integer mNewWatchStatus;// value is either null (after media is deleted) or (new media data's watch status i.e mSelectedWatchStatus)
    private ArrayList<UserListModel> mOriginalContainingLists;
    private ArrayList<UserListModel> mNewContainingLists;

    private RecyclerView mRecyclerView;
    private SuperEditText mSearchTextView;
    private RadioGroup mWatchStatusRadioGroup;
    private Button mSaveButton;

    // The Idling Resource which will be null in production.
    @Nullable
    private SimpleIdlingResource mIdlingResource;

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
        mSearchTextView = findViewById(R.id.search_edit_text_view);
        mSearchTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        onSearchTextChanged();
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        tagAdapter = new TagAdapter(
                this,
                R.layout.fragment_list_results_parent,
                R.id.search_edit_text_view,
                new ArrayList<String>()
        );

        mSearchTextView.setAdapter(tagAdapter);
        mSearchTextView.setThreshold(1);

        ImageButton createListButton = findViewById(R.id.create_list_button);
        createListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MasterActivity.launchCreateListActivity(AddToListActivity.this);
            }
        });

        //configure watch status selector
        mWatchStatusRadioGroup = findViewById(R.id.watch_status_radio_group);
        mWatchStatusRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mSelectedWatchStatus = mWatchStatusRadioGroup.indexOfChild(findViewById(checkedId));
                mMediaData.setWatchStatus(mSelectedWatchStatus);

            }
        });

        mSaveButton = findViewById(R.id.save_button);
    }

    private void observeViewModel() {
        if (mIdlingResource != null) {
            mIdlingResource.setIdleState(false);
        }

        AddToListViewModelFactory addToListViewModelFactory =
                new AddToListViewModelFactory(mDatabase, mMediaType, mMediaData.getId());

        //create view model
        final AddToListViewModel viewModel = ViewModelProviders.of(this, addToListViewModelFactory)
                .get(AddToListViewModel.class);
        final LiveData<Integer> watchStatusLiveData = viewModel.getWatchStatus();
        final LiveData<List<UserListModel>> allUserListLiveData = viewModel.getAllUserLists();
        final LiveData<List<UserListModel>> containingUserListLiveData = viewModel.getContainingLists();

        //observe live data of media's watch status
        watchStatusLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer watchStatus) {
                watchStatusLiveData.removeObserver(this);

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
                    containingUserListLiveData.observe(AddToListActivity.this, new Observer<List<UserListModel>>() {
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

                            populateAndNotifyAdapter(allUserLists);

                            //restore search if it exists
                            restoreSearchIfAvailable();

                            allowSave();
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

    private void populateAndNotifyAdapter(List listModels) {
        if (listModels.size() == 0) {
            mAdapter.setInPlaceholderMode(true);

        } else {
            mAdapter.setInPlaceholderMode(false);
            mAdapter.getAdapterData().clear();
            mAdapter.addAdapterData(listModels);
        }
    }

    private void restoreSearchIfAvailable() {
        if (mSavedInstanceState == null) return;
        String savedSearch = mSavedInstanceState.getString(SEARCH_TEXT_KEY);
        if (savedSearch == null || savedSearch.equals("")) return;

        //show keyboard if restore value is true
        //else hide it
        if (MasterActivity.sIsKeyboardVisible) {
            showSoftKeyboard(mSearchTextView);

        } else {
            hideSoftKeyboard(mSearchTextView);
        }

        String savedSearchText = mSavedInstanceState.getString(SEARCH_TEXT_KEY);

        mSearchTextView.setText(savedSearchText);
    }

    private static final int TAG_COUNT_LIMIT = 7;
    private void onSearchTextChanged() {
        String activeText = mSearchTextView.getActiveText();
        final List<String> searchTags = AppDatabase.getLocalDatabase(this).searchListTagsDao()
                .getTagsLikeAlt(activeText);

        new MainThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                tagAdapter.clear();
                tagAdapter.addAll(searchTags);
                tagAdapter.notifyDataSetChanged();

                performFullSearchWithTags();
            }
        });
    }

    private void performFullSearchWithTags() {
        String searchBoxStrings = mSearchTextView.getText().toString();
        String[] terms = searchBoxStrings.split(" ");
        final List<String> formattedTags = new ArrayList<>();

        for (int i = 0; i < TAG_COUNT_LIMIT; i++) {
            if (!(i > terms.length - 1)) {
                formattedTags.add(terms[i]);

            } else {
                formattedTags.add("");
            }
        }

        searchInUserList(formattedTags);
    }

    private void searchInUserList(List<String> formattedTags) {
        final LiveData<List<UserListModel>> listsLiveData = mDatabase.userListsDao()
                .getListsWithNameLike(formattedTags.get(0), formattedTags.get(1),
                        formattedTags.get(2), formattedTags.get(3), formattedTags.get(4),
                        formattedTags.get(5), formattedTags.get(6));

        listsLiveData.observe(this, new Observer<List<UserListModel>>() {
            @Override
            public void onChanged(List<UserListModel> userListModels) {
                listsLiveData.removeObserver(this);
                populateAndNotifyAdapter(userListModels);
            }
        });
    }

    private void allowSave() {
        //configure save button
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDetailsAndUpdateData();

                //exit activity
                finish();
            }
        });

        if (mIdlingResource != null) {
            mIdlingResource.setIdleState(true);
        }
    }

    private LinearLayoutManager configureLayoutManager() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void getDetailsAndUpdateData() {
        String[] detailUrls = getResources().getStringArray(R.array.details_urls);
        String detailUrl = null;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            detailUrl = detailUrls[MEDIA_TYPE_SERIES];
        }

        SearchPreferences searchPreferences =  new SearchPreferences();

        //build AN request
        ANRequest request = NetworkFunctions.agnosticDetailRequestById(detailUrl, mMediaData.getId(),
                searchPreferences, this);

        // The IdlingResource is null in production.
        if (mIdlingResource != null) {
            mIdlingResource.setIdleState(false);
        }

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                try {
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mMediaType == MEDIA_TYPE_MOVIE) {
                                mMediaData =
                                        MovieDataParser.parseDetails(returnedJSONString, ((MovieData) mMediaData));

                            } else {
                                mMediaData =
                                        SeriesDataParser.parseDetails(returnedJSONString, ((SeriesData) mMediaData));
                            }

                            updateData();
                        }
                    });

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(final ANError anError) {
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            retryAfterCoolDOwn(anError);
                        }
                    });

                } else {
                    updateData();
                }

                //notify user of error
                Log.d(TAG, getString(R.string.details_error_message));
            }
        });
    }

    //retry method if api returns too may requests error
    private void retryAfterCoolDOwn(ANError anError) {
        Log.d(TAG, "retrying details fetch");

        int coolDown;

        try {
            //noinspection ConstantConditions
            coolDown = Integer.parseInt(anError.getResponse().header(ApiConstants.RETRY_AFTER_KEY));

        } catch (NullPointerException e) {
            e.printStackTrace();
            coolDown = UpdateMediaWorker.REQUEST_COOL_DOWN;

        }

        int coolDownInMilliSecs = coolDown * MILLISECOND_CONVERSION;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    getDetailsAndUpdateData();

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

            }
        }, coolDownInMilliSecs);
    }

    private void updateData() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                //TODO: ADD CONDITION TO DO NOTHING IF NO CHANGES ARE MADE
                int userListNetCountChange = getUserListNetCountChange();
                int newContainingListValue = mOriginalContainingLists.size() + userListNetCountChange;

                boolean isMediaUnused = isMediaUnused(mSelectedWatchStatus, newContainingListValue);

                mSavedMedia = getSavedMedia();

                if (isMediaUnused) {
                    updateUserListRecords();
                    if (mSavedMedia != null) {
                        deleteSavedMedia();
                        deleteTags();
                    }

                } else {
                    saveTags();
                    updateMediaData();
                    updateUserListRecords();
                }

                setNotifiers();

                mNewWatchStatus = isMediaUnused? null : mSelectedWatchStatus;

                updateWatchListCounts();

                if (mIdlingResource != null) {
                    mIdlingResource.setIdleState(true);
                }
            }
        });
    }

    private MediaData getSavedMedia() {
        MediaData mediaData;
        if (mMediaType == MEDIA_TYPE_MOVIE) {
            mediaData = mDatabase.movieDataDao().getMovieByIdAlt(mMediaData.getId());

        } else {
            mediaData = mDatabase.seriesDataDao().getSeriesByIdAlt(mMediaData.getId());
        }

        return mediaData;
    }

    private void updateMediaData() {
        if (mMediaType == MEDIA_TYPE_MOVIE) {
            updateMovieData();

        } else {
            updateSeriesData();
        }
    }

    private void updateMovieData() {
        mMediaData.setWatchStatus(mSelectedWatchStatus);

        if (mSavedMedia != null) {
            //preserve user rating, trakt id and unique externalId
            mMediaData.setUserRating(mSavedMedia.getUserRating());
            mMediaData.setTraktId(mSavedMedia.getTraktId());
            mMediaData.setUniqueExternalId(mSavedMedia.getUniqueExternalId());

            mDatabase.movieDataDao().updateMovieData(((MovieData) mMediaData));

        } else {
            mDatabase.movieDataDao().addMovieData(((MovieData) mMediaData));
        }

        Log.d(TAG, "update media data");
    }

    private void updateSeriesData() {
        mMediaData.setWatchStatus(mSelectedWatchStatus);

        if (mSavedMedia != null) {
            //preserve user rating, trakt id and unique externalId
            mMediaData.setUserRating(mSavedMedia.getUserRating());
            mMediaData.setTraktId(mSavedMedia.getTraktId());
            mMediaData.setUniqueExternalId(mSavedMedia.getUniqueExternalId());

            mDatabase.seriesDataDao().updateSeriesData(((SeriesData) mMediaData));

        } else {
            mDatabase.seriesDataDao().addSeriesData(((SeriesData) mMediaData));
        }

        Log.d(TAG, "update media data");
    }

    private void saveTags() {
        mMediaData.createTags();

        AppDatabase localDb = AppDatabase.getLocalDatabase(this);
        for (SearchMediaTag tag: mMediaData.searchTags) {
            SearchMediaTag savedTag = localDb.searchMediaTagsDao().getTagAlt(tag.mTag);

            if (savedTag == null) {
                localDb.searchMediaTagsDao().addTag(tag);
            }
        }
    }

    private void deleteTags() {
        if (mSavedMedia.searchTags == null) return;

        for (SearchMediaTag tag: mSavedMedia.searchTags) {
            int tagUsage;

            AppDatabase localDb = AppDatabase.getLocalDatabase(this);
            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
                tagUsage = localDb.movieDataDao().getAllMediaWithTagAlt(tag.mTag).size()
                        + localDb.movieDataDao().getAllMediaWithTagAlt(tag.mTag).size();

            } else {
                tagUsage = localDb.seriesDataDao().getAllMediaWithTagAlt(tag.mTag).size()
                        + localDb.seriesDataDao().getAllMediaWithTagAlt(tag.mTag).size();
            }

            if (tagUsage == 0) {
                localDb.searchMediaTagsDao().deleteTag(tag);
            }
        }
    }

    private int getUserListNetCountChange() {
        int netCountChange = 0;

        for (UserListModel userListModel : mNewContainingLists) {
            if (!mOriginalContainingLists.contains(userListModel)) {
                netCountChange += 1;
            }
        }

        for (UserListModel userListModel : mOriginalContainingLists) {
            if (!mNewContainingLists.contains(userListModel)) {
                netCountChange -= 1;
            }
        }

        return netCountChange;
    }

    private void updateUserListRecords() {
        for (UserListModel userListModel : mNewContainingLists) {
            if (!mOriginalContainingLists.contains(userListModel)) {
                //add the media to the list
                Log.d(TAG, "adding to list");
                addToList(userListModel);
                updateListCount(userListModel, +1);
            }
        }

        for (UserListModel userListModel : mOriginalContainingLists) {
            if (!mNewContainingLists.contains(userListModel)) {
                //remove the media from the list
                Log.d(TAG, "removing from list");
                removeFromList(userListModel);
                updateListCount(userListModel, -1);
            }
        }
    }

    private void setNotifiers() {
        Intent intent = new Intent(getApplicationContext(), UpdateNotifierService.class);
        intent.putExtra(NEW_MEDIA_DATA_KEY, Parcels.wrap(mMediaData));
        UpdateNotifierService.enqueueWork(getApplicationContext(), intent);
    }

    private void updateWatchListCounts() {
        //get watch status names
        //get watch status lists
        //update counts
        //update lists

        if (String.valueOf(mOldWatchStatus).equals(mNewWatchStatus)) return;

        if (mOldWatchStatus != null) {
            String oldWatchStatusName = MediaData.getWatchStatusTitle(mOldWatchStatus,
                    getApplicationContext());

            WatchListModel oldWatchStatusList =
                    mDatabase.watchListsDao().getListByNameAlt(oldWatchStatusName);

            if (oldWatchStatusList.getItemCount() >= 1) {
                oldWatchStatusList.setItemCount(oldWatchStatusList.getItemCount() - 1);
                mDatabase.watchListsDao().updateListConfiguration(oldWatchStatusList);
            }
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
            //todo: to avoid 2nd query, instead separate getAllListsContainingMedia query into two queries and use the media record to delete
            MovieDataRecord dataRecord = mDatabase.movieDataRecordsDao()
                    .getRecordByIdAlt(mMediaData.getId(), userListModel.getName());
            mDatabase.movieDataRecordsDao().deleteRecord((dataRecord));

        } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
            //remove record from list
            //todo: to avoid 2nd query, instead separate getAllListsContainingMedia query into two queries and use the media record to delete
            SeriesDataRecord dataRecord = mDatabase.seriesDataRecordsDao()
                    .getRecordByIdAlt(mMediaData.getId(), userListModel.getName());
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

    private boolean isMediaUnused(int newWatchStatus, int newContainingListValue) {
        //if watch status is none and if there are no lists containing this media
        return newWatchStatus == MediaData.WATCH_STATUS_NONE
                && newContainingListValue == 0;
    }

    private void deleteSavedMedia() {
        if (mMediaType == MEDIA_TYPE_MOVIE) {
            mDatabase.movieDataDao().deleteMovieData(((MovieData) mSavedMedia));

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            mDatabase.seriesDataDao().deleteSeriesData(((SeriesData) mSavedMedia));
        }
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
    public void onItemClick(final ListModel listModel, AppCompatCheckBox checkBox) {
        if (mAdapter.inPlaceholderMode()) {
            MasterActivity.launchCreateListActivity(this);

            return;
        }

        //if list model doesn't exist in mNewContainingLists
        //noinspection SuspiciousMethodCalls
        if (!mNewContainingLists.contains(listModel)) {
            //add list
            mNewContainingLists.add(((UserListModel) listModel));

        } else {//if name exists in mNewContainingLists
            //remove from list
            //noinspection RedundantCast
            mNewContainingLists.remove(((UserListModel) listModel));
        }

        //toggle checkbox;
        checkBox.toggle();
    }

    /**
     * Only called from test, creates and returns a new {@link SimpleIdlingResource}.
     */
    @VisibleForTesting
    @NonNull
    public IdlingResource getIdlingResource() {
        if (mIdlingResource == null) {
            mIdlingResource = new SimpleIdlingResource();
        }
        return mIdlingResource;
    }
}