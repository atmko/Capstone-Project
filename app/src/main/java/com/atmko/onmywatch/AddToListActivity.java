/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.onmywatch.adapters.AddToListAdapter;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.Episode;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.MovieDataParser;
import com.atmko.onmywatch.utils.NotificationHandler;
import com.atmko.onmywatch.utils.SearchPreferences;
import com.atmko.onmywatch.utils.SeriesDataParser;
import com.atmko.onmywatch.utils.UpdateMediaWorker;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.MovieApiConstants;
import com.atmko.onmywatch.utils.network_utils.NetworkFunctions;
import com.atmko.onmywatch.utils.network_utils.SeriesApiConstants;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;
import com.atmko.onmywatch.view_models.AddToListViewModel;
import com.atmko.onmywatch.view_models.AddToListViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

import org.parceler.Parcels;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.atmko.onmywatch.Fragments.DetailsFragment.COOL_DOWN_REQUEST_TMDB_ID;
import static com.atmko.onmywatch.Fragments.DetailsFragment.COOL_DOWN_REQUEST_TRAKT_ID;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.models.SeriesNotifier.CONDITION_NEW_EPISODE;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;

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
                updateData();

                //exit activity
                finish();
            }
        });
    }

    private void observeViewModel() {
        //create view model
        AddToListViewModelFactory addToListViewModelFactory =
                new AddToListViewModelFactory(mDatabase, mMediaType, mMediaData.getId());

        final AddToListViewModel viewModel =
                ViewModelProviders.of(this, addToListViewModelFactory)
                        .get(AddToListViewModel.class);

        //observe live data of media's watch status
        final LiveData<Integer> watchStatusLiveData = viewModel.getWatchStatus();
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

                            //update list so onCheckDatabaseRecords function can run
                            mAdapter.getAdapterData().clear();
                            mAdapter.addAdapterData(allUserLists);
                        }
                    });
                    //else if no user list(s) exist
                } else {
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

                updateReleaseNotifier(newMediaData);
                if (newMediaData instanceof SeriesData) {
                    updateNewEpisodeNotifier(((SeriesData) newMediaData));
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

    //creates release notifier if new watch status is to watch or watching,
    //otherwise delete notifier with this media id and cancel alarm
    private void updateReleaseNotifier(MediaData newMediaData) {
        int newWatchStatus = newMediaData.getWatchStatus();
        if (newWatchStatus == MediaData.WATCH_STATUS_TO_WATCH
                || newWatchStatus == MediaData.WATCH_STATUS_WATCHING) {

            //if release date exists set release notifier through date caparison
            //otherwise create a notifier via release status without creating an alarm
            if (!newMediaData.getReleaseDate().equals("")) {
                setReleaseNotifierThroughDateComparision(newMediaData);

            } else {
                setReleaseNotifierThroughReleaseStatus(newMediaData);
            }

        } else {
            cancelMediaAlarmIfExists(MediaNotifier.CONDITION_ON_RELEASE);
        }
    }

    //compares release date and current date and sets release notifier if release date is in the future
    //then schedules alarm notification for future
    private void setReleaseNotifierThroughDateComparision(MediaData newMediaData) {
        Date currentDate = new Date();
        Date releaseDate;

        try {
            //TODO: local format not used. Using API date format
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ApiConstants.DATE_FORMAT);
            releaseDate = simpleDateFormat.parse(newMediaData.getReleaseDate());

        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        //if release date has passed, return
        if (releaseDate.before(currentDate)) return;

        //create notifier and set alarm with release notification
        MediaNotifier releaseNotifier = createReleaseNotifier(newMediaData);

        NotificationHandler.scheduleReleaseNotification(this, newMediaData, releaseNotifier);
    }

    //used when release date doesn't exist. Checks if media has been released by getting release status via media's details
    //if release status not released, canceled, pilot, ended or returning series, save notifier object without creating accompanying alarm notification.
    //NOTE: alarm will be created when media is updated and a release date becomes available
    private void setReleaseNotifierThroughReleaseStatus(final MediaData newMediaData) {
        //if release status exists create notifier and return
        //otherwise fetch release status from media details, then create notifier
        //NOTE: release status will be null when not accessing this activity via DetailsFragment, because details won't have been fetched
        if (newMediaData.getReleaseStatus() != null) {
            createReleaseNotifierPendingRelease(newMediaData);
            return;
        }

        String[] detailUrls = getResources().getStringArray(R.array.details_urls);
        String detailUrl = null;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            detailUrl = detailUrls[MEDIA_TYPE_SERIES];
        }

        SearchPreferences searchPreferences =  new SearchPreferences();

        //build AN request
        ANRequest request = NetworkFunctions.agnosticDetailRequestById(detailUrl, newMediaData.getId(),
                searchPreferences, this);

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                try {
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            //get release status, set release status and create notifier
                            MediaData detailsMediaData;

                            if (mMediaType == MEDIA_TYPE_MOVIE) {
                                detailsMediaData =
                                        MovieDataParser.parseDetails(returnedJSONString, ((MovieData) mMediaData));

                            } else {
                                detailsMediaData =
                                        SeriesDataParser.parseDetails(returnedJSONString,
                                                ((SeriesData) mMediaData));
                            }

                            String releaseStatus = detailsMediaData.getReleaseStatus();
                            newMediaData.setReleaseStatus(releaseStatus);
                            createReleaseNotifierPendingRelease(newMediaData);
                        }
                    });

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError, COOL_DOWN_REQUEST_TMDB_ID, newMediaData);

                    return;
                }

                //notify user of error
                Snackbar.make(AddToListActivity.this.findViewById(R.id.top_layout),
                        getString(R.string.details_error_message), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    //create notifier if media release still pending
    private void createReleaseNotifierPendingRelease(MediaData newMediaData) {
        String releaseStatus = newMediaData.getReleaseStatus();

        //create notifier if media release still pending
        if (!releaseStatus.equals(MovieApiConstants.RELEASE_STATUS_RELEASED)
                && !releaseStatus.equals(SeriesApiConstants.SeriesTextReplacement.REPLACEMENT_RETURNING_SERIES)
                && !releaseStatus.equals(SeriesApiConstants.RELEASE_STATUS_PILOT)
                && !releaseStatus.equals(SeriesApiConstants.RELEASE_STATUS_ENDED)
                && !releaseStatus.equals(ApiConstants.RELEASE_STATUS_CANCELED)) {

            createReleaseNotifier(newMediaData);
        }
    }

    //creates new Media release notifier in database and returns notifier
    private MediaNotifier createReleaseNotifier(MediaData newMediaData) {
        //create notifier and set alarm with release notification
        MediaNotifier releaseNotifier;

        if (newMediaData instanceof MovieData) {
            releaseNotifier = new MovieNotifier(newMediaData.getId(), MediaNotifier.CONDITION_ON_RELEASE);
            mDatabase.movieNotifierDao().addMediaNotifier(((MovieNotifier) releaseNotifier));

        } else {
            releaseNotifier = new SeriesNotifier(newMediaData.getId(), MediaNotifier.CONDITION_ON_RELEASE);
            mDatabase.seriesNotifierDao().addMediaNotifier(((SeriesNotifier) releaseNotifier));
        }

        return releaseNotifier;
    }

    //creates new episode notifier if new watch status is watching,
    //otherwise delete notifier with this media id and cancel alarm
    private void updateNewEpisodeNotifier(SeriesData newMediaData) {
        int newWatchStatus = newMediaData.getWatchStatus();
        if (newWatchStatus == MediaData.WATCH_STATUS_WATCHING) {
            getTraktNextEpisodeDetails(newMediaData);

        } else {
            cancelMediaAlarmIfExists(CONDITION_NEW_EPISODE);
        }
    }

    //get next episode details from trakt api
    //gets called twice: once to get matching trakt id, again to get trakt next episode details
    //if trakt id already exists, its called only once
    private void getTraktNextEpisodeDetails(final SeriesData newMediaData) {
        //if inputTraktId id is null make url to get trakt id
        //otherwise make url to get next episode details

        final String inputTraktId = newMediaData.getTraktId();

        String[] traktFetchUrls;
        ANRequest request;

        if (inputTraktId == (null)) {
            traktFetchUrls = getResources().getStringArray(R.array.trakt_matching_media_urls);
            String traktFetchUrl = traktFetchUrls[mMediaType];
            request = NetworkFunctions.traktAgnosticRequestById(
                    traktFetchUrl, newMediaData.getId());

        } else {
            String traktFetchUrl = getString(R.string.trakt_next_episode_urls);
            request = NetworkFunctions.traktAgnosticRequestById(
                    traktFetchUrl, newMediaData.getTraktId());
        }

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (inputTraktId == null) {
                                String outputTraktId = SeriesDataParser.parseAndGetTraktId(returnedJSONString);

                                //rerun the function with non null trakt id
                                if (outputTraktId != null) {
                                    newMediaData.setTraktId(outputTraktId);
                                    getTraktNextEpisodeDetails(newMediaData);
                                }

                            } else {
                                //parse trakt info
                                SeriesData detailsMediaData =
                                        SeriesDataParser.parseTraktNextEpisodeDetails(returnedJSONString, newMediaData);

                                //if there is a next episode and date, create notifier using date, otherwise try using tmdb details
                                Episode nextEpisode = detailsMediaData.getNextEpisodeToAir();
                                if (nextEpisode != null && nextEpisode.getBestAvailableDate() != null) {
                                    setNewEpisodeNotifierThroughDateComparison(detailsMediaData);

                                } else {
                                    getTmdbNextEpisodeDetails(detailsMediaData);
                                }
                            }

                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() == TraktApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError, COOL_DOWN_REQUEST_TRAKT_ID, newMediaData);

                    return;
                }

                //notify user of error
                Snackbar.make(findViewById(R.id.top_layout),
                        getString(R.string.details_error_message), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    //creates new episode notifier and notification alarm if release date exists and is in the future
    private void setNewEpisodeNotifierThroughDateComparison(SeriesData newMediaData) {
        //if release date is null or if release date has passed, return
        Date releaseDate = newMediaData.getNextEpisodeToAir().getLocalAirDate();
        if (releaseDate == null || releaseDate.before(new Date())) return;

        SeriesNotifier newEpisodeNotifier = createNewEpisodeNotifier();

        NotificationHandler
                .scheduleNewEpisodeNotification(this, newMediaData, newEpisodeNotifier);
    }

    //Checks if media has been released by getting release status via media's details
    //if episode and air date available, create notification alarm using date
    //if no new episode and or episode date available, save notifier object without creating accompanying alarm notification.
    private void getTmdbNextEpisodeDetails(final SeriesData newMediaData) {
        //if release status exists create notifier and return
        //otherwise fetch release status from media details, then create notifier
        //NOTE: release status will be null when not accessing this activity via DetailsFragment, because details won't have been fetched
        if (newMediaData.getReleaseStatus() != null) {
            createNewEpisodeNotifierPendingRelease(newMediaData);
            return;
        }

        String[] detailUrls = getResources().getStringArray(R.array.details_urls);
        String detailUrl = null;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            detailUrl = detailUrls[MEDIA_TYPE_SERIES];
        }

        SearchPreferences searchPreferences =  new SearchPreferences();

        //build AN request
        ANRequest request = NetworkFunctions.agnosticDetailRequestById(detailUrl, newMediaData.getId(),
                searchPreferences, this);

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                try {
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            //get release status, set release status and create notifier
                            SeriesData detailsMediaData =
                                        SeriesDataParser.parseDetails(returnedJSONString,
                                                ((SeriesData) mMediaData));

                            String releaseStatus = detailsMediaData.getReleaseStatus();
                            newMediaData.setReleaseStatus(releaseStatus);


                            //if there is a next episode and date, create notifier using date, otherwise create notifier without alarm
                            Episode nextEpisode = detailsMediaData.getNextEpisodeToAir();
                            if (nextEpisode != null && nextEpisode.getBestAvailableDate() != null) {
                                setNewEpisodeNotifierThroughDateComparison(newMediaData);

                            } else {
                                //NOTE: alarm will be created when media is updated and a release date becomes available
                                createNewEpisodeNotifierPendingRelease(newMediaData);
                            }
                        }
                    });

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError, COOL_DOWN_REQUEST_TRAKT_ID, newMediaData);

                    return;
                }

                //notify user of error
                Snackbar.make(AddToListActivity.this.findViewById(R.id.top_layout),
                        getString(R.string.details_error_message), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    //create notifier if episodes still pending
    private void createNewEpisodeNotifierPendingRelease(SeriesData newMediaData) {
        String releaseStatus = newMediaData.getReleaseStatus();

        //create notifier if new episodes still pending
        if (releaseStatus.equals(SeriesApiConstants.SeriesTextReplacement.REPLACEMENT_RETURNING_SERIES)) {
            createNewEpisodeNotifier();
        }
    }

    private SeriesNotifier createNewEpisodeNotifier() {
        SeriesNotifier newEpisodeNotifier =
                new SeriesNotifier(mMediaData.getId(), CONDITION_NEW_EPISODE);
        mDatabase.seriesNotifierDao().addMediaNotifier(newEpisodeNotifier);

        return newEpisodeNotifier;
    }

    //retry method if api returns too may requests error
    private void retryAfterCoolDOwn(ANError anError, final int coolDownRequestId,
                                    final MediaData newMediaData) {
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
                    if (coolDownRequestId == COOL_DOWN_REQUEST_TMDB_ID) {
                        setReleaseNotifierThroughReleaseStatus(newMediaData);

                    } else if (coolDownRequestId == COOL_DOWN_REQUEST_TRAKT_ID){
                        getTraktNextEpisodeDetails(((SeriesData) newMediaData));
                    }

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

            }
        }, coolDownInMilliSecs);
    }

    //cancels all alarms with media id and deletes notifiers
    private void cancelMediaAlarmIfExists(int condition) {
        MediaNotifier notifier;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            notifier = mDatabase.movieNotifierDao().getNotifierByIdAlt(mMediaData.getId(), condition);

        } else {
            notifier = mDatabase.seriesNotifierDao().getNotifierByIdAlt(mMediaData.getId(), condition);
        }

        if (notifier != null) {
            //cancel alarm and delete media notifier
            NotificationHandler.cancelAlarm(this, notifier);
        }
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
