/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.atmko.onmywatch.Fragments.DetailsFragment;
import com.atmko.onmywatch.Fragments.HomeFragment;
import com.atmko.onmywatch.Fragments.ListResultsParentFragment;
import com.atmko.onmywatch.Fragments.PeopleDetailsFragment;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.database.daos.FirebaseUserDataDao;
import com.atmko.onmywatch.models.Backup;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.PersonData;
import com.atmko.onmywatch.models.SimpleIdlingResource;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.BackupService;
import com.atmko.onmywatch.utils.network_utils.RestoreService;
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.BackupWorker;
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker;
import com.atmko.onmywatch.view_models.MasterActivityViewModel;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.parceler.Parcels;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MasterActivity extends AppCompatActivity
        implements BackupService.OnBackupCompleteListener, RestoreService.OnRestoreCompleteListener {
    private static final int FREE_MODE_LIST_COUNT_LIMIT = 3;

    public static final int MEDIA_TYPE_SERIES = 0;
    public static final int MEDIA_TYPE_MOVIE = 1;
    public static final int MEDIA_TYPE_PEOPLE = 2;

    private static final String KEYBOARD_VISIBILITY_KEY = "keyboard_visibility";

    public static final String SEARCH_TEXT_KEY = "search_text";
    public static final String SEARCH_BAR_VISIBILITY_KEY = "visible_search_bar";

    private static final String UPDATE_MEDIA_WORKER_KEY = "update_media_worker";
    private static final String BACKUP_WORKER_KEY = "backup worker";
    private static final int REPEAT_INTERVAL = 2;
    private static final int INITIAL_DELAY = 15;

    private static final String USER_COLLECTION_PATH = "users";
    private final int SIGN_IN_REQUEST_CODE = 10;
    private static final int REQUEST_LOG_OUT = 20;

    //for restoring keyboard visibility upon configuration change
    public static boolean sIsKeyboardVisible;
    private boolean mIsTabletLandscape;
    private FirebaseAnalytics mFirebaseAnalytics;

    private Bundle mSavedInstanceState;

    private FrameLayout progressLayout;

    // The Idling Resource which will be null in production.
    @Nullable
    public SimpleIdlingResource mIdlingResource;

    public static boolean sIsAuthUiActive;
    public static boolean sIsProMode;
    public static boolean sAllowCloudBackup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        defineViews();
        mSavedInstanceState = savedInstanceState;

        createNotificationChannels();

        //set / restore values
        setValues();

        mIsTabletLandscape = getResources().getBoolean(R.bool.isTabletLandscape);

        // Obtain Analytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //if current user is null start login
        if (getCurrentUser() == null) {
            if (!sIsAuthUiActive) {
                sIsAuthUiActive = true;
                startSignInActivity();
            }

        } else {
            //observe user data via view model
            observeData(false);
        }
    }

    private void defineViews() {
        progressLayout = findViewById(R.id.progress_layout);
    }

    public static FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public static DocumentReference getUserDbHomeReference() {
        return FirebaseFirestore.getInstance()
                .collection(USER_COLLECTION_PATH)
                .document(getCurrentUser().getUid());
    }

    //retrieve data from the activity's view model
    private void observeData(boolean isLoggingIn) {
        if (mIdlingResource != null) {
            mIdlingResource.setIdleState(false);
        }

        RoomDatabase.Callback callback = databaseInitializer(isLoggingIn);
        AppDatabase.getInstance(this, callback);

        MasterActivityViewModel masterActivityViewModel =
                ViewModelProviders.of(this).get(MasterActivityViewModel.class);

        masterActivityViewModel.getIsProModeLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isProMode) {
                if (isProMode == null) return;

                sIsProMode = isProMode;
                if (!sIsProMode) {
                    initializeAdMob();
                }

                if (getSupportFragmentManager().getFragments().size() == 0) loadUi();
                //start background work managers
                startWorkers();
            }
        });
    }

    public void loadUi() {
        //remove all fragments
        Fragment masterFragment =
                getSupportFragmentManager().findFragmentById(R.id.master_fragments_container);
        if (masterFragment != null) getSupportFragmentManager().beginTransaction().remove(masterFragment);

        Fragment detailsFragment =
                getSupportFragmentManager().findFragmentById(R.id.detail_fragments_container);
        if (detailsFragment != null) getSupportFragmentManager().beginTransaction().remove(detailsFragment);

        //start ui
        startHomeFragment();

        if (getIntent() != null) {
            Intent intent = getIntent();
            String action = intent.getAction();

            if (action != null && action.equals(DetailsFragment.ACTION_LAUNCH_DETAILS)) {
                Bundle extras = intent.getExtras();
                launchDetailsFromIntent(intent, extras);
            }
        }

        getSupportFragmentManager().executePendingTransactions();

        if (mIdlingResource != null) {
            mIdlingResource.setIdleState(true);
        }
    }

    private void startSignInActivity() {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                SIGN_IN_REQUEST_CODE);
    }

    public static void startLogOutBackupService(Activity activity) {
        //backup before logging out;
        Intent intent = new Intent(activity, BackupService.class);
        BackupService.enqueueWork(activity, intent);
    }

    private void logOut() {
        AppDatabase.deleteLocallySavedData(this);
        //remove database instance so signing in has proper functionality
        AppDatabase.closeDatabase();

        //log out
        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();

        FirebaseAuth.getInstance().signOut();
        GoogleSignIn.getClient(this, gso).signOut();

        //restart activity
        Intent intent = new Intent(getApplicationContext(), MasterActivity.class);
        finish();
        startActivity(intent);
    }

    @Override
    public void onLogOutBackupComplete() {
        logOut();
    }

    @Override
    public void onBackupComplete() {

    }

    @Override
    public void onBackupFailure(final String errorMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (errorMessage != null && !errorMessage.equals("")) {
                    showSnackBarMessage(errorMessage);

                } else {
                    showSnackBarMessage(getString(R.string.backup_failure_message));
                }

                launchConfirmationActivity(MasterActivity.this,
                        REQUEST_LOG_OUT, ConfirmationActivity.ACTION_LOG_OUT);
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                //if returning from firebase sign in activity, configure database and observe data
                if (requestCode == SIGN_IN_REQUEST_CODE) {
                    sIsAuthUiActive = false;

                    if (resultCode == RESULT_OK) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //observe user data via view model
                                observeData(true);
                            }
                        });
                    } else {
                        if (!NetworkFunctions.isOnline()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MasterActivity.this,
                                            "No Connection Established",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        finish();
                    }
                } else if (requestCode == REQUEST_LOG_OUT) {
                    if (resultCode == RESULT_OK) {
                        logOut();
                    }
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();

        if (intent.getAction().equals(DetailsFragment.ACTION_LAUNCH_DETAILS)) {
            launchDetailsFromIntent(intent, extras);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //save keyboard visibility value
        outState.putBoolean(KEYBOARD_VISIBILITY_KEY, sIsKeyboardVisible);
    }

    private void initializeAdMob() {
        try {
            String adMobIdKey = getString(R.string.ad_mob_application_id_key);

            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(),
                    PackageManager.GET_META_DATA);
            Bundle applicationMetaData = applicationInfo.metaData;

            String adMobId = applicationMetaData.getString(adMobIdKey);

            MobileAds.initialize(this, adMobId);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setValues() {
        if (mSavedInstanceState != null) {
            //restore keyboard visibility value
            sIsKeyboardVisible =
                    mSavedInstanceState.getBoolean(KEYBOARD_VISIBILITY_KEY);
        }
    }

    //condition 1
    //hides master fragment if details fragment is loaded (only when two non two pane)
    //condition 2
    //hides fragment if its not active in master container
    public void onResumeMasterContainerFragment(Fragment fragment) {
        //condition 1
        //if not tablet landscape (not two pane)
        //&& there's a fragment in details container
        //hide fragment
        if (!mIsTabletLandscape && hasFragment(R.id.detail_fragments_container)) {
            //hide background fragment to reserve keyboard focus for newly loaded fragment
            fragment.getView().findViewById(R.id.top_layout).setVisibility(View.GONE);

        }

        //condition 2
        //compare master fragment name and resumed fragment name
        //if no match, hide fragment
        String masterContainerFragmentName =
                getSupportFragmentManager()
                        .findFragmentById(R.id.master_fragments_container)
                        .getClass().getSimpleName();

        String resumedFragmentName = fragment.getClass().getSimpleName();

        if (!masterContainerFragmentName.equals(resumedFragmentName)) {

            //hide background fragment to reserve keyboard focus for newly loaded fragment
            hideFragment(fragment);
        }
    }

    private void startHomeFragment() {
        HomeFragment homeFragment = HomeFragment.newInstance();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.master_fragments_container, homeFragment, HomeFragment.FRAGMENT_KEY)
                .commit();
    }

    private void createNotificationChannels() {
        BackupWorker.createBackupNotificationChannel(this);
        MediaNotifier.createReleaseNotificationChannel(this);
    }

    private void startWorkers() {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build();

        PeriodicWorkRequest updateMediaDataRequest = new PeriodicWorkRequest.Builder(
                UpdateMediaWorker.class, REPEAT_INTERVAL, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(INITIAL_DELAY, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                UPDATE_MEDIA_WORKER_KEY, ExistingPeriodicWorkPolicy.KEEP, updateMediaDataRequest);

        PeriodicWorkRequest backupRequest = new PeriodicWorkRequest.Builder(
                BackupWorker.class, REPEAT_INTERVAL, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(INITIAL_DELAY, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                BACKUP_WORKER_KEY, ExistingPeriodicWorkPolicy.KEEP, backupRequest);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();

            return true;

        }else {
            return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onBackPressed() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        boolean hasDetailFragment = getSupportFragmentManager()
                        .findFragmentById(R.id.detail_fragments_container) != null;
        Fragment fragment;
        if (!isTabletLandscape() && hasDetailFragment) {
            fragment = getSupportFragmentManager().findFragmentById(R.id.detail_fragments_container);

        } else {
            fragment = fragments.get(fragments.size() - 1);
        }

        //if in tablet landscape and there are only 2 fragments left, finish
        //OR if not in tablet landscape and there is only 1 fragment, finish
        if (fragments.size() == 1 && fragment instanceof HomeFragment) {
            finish();
            return;
        }

        //condition for navigation
        //this removes details fragment because master container is behind detail container
        // (via frame layout) in non tablet landscape
        if (fragment instanceof DetailsFragment || fragment instanceof PeopleDetailsFragment) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                    .remove(fragment).commit();

        } else {
            //condition for exit animation transition
            if (fragment instanceof ListResultsParentFragment) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                        .remove(fragment).commit();

            //condition for navigation
            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_down_entry, R.anim.slide_up_exit)
                        .remove(fragment)
                        .commit();
            }
        }

        //finish above transaction to prevent null data
        getSupportFragmentManager().executePendingTransactions();

        Fragment backgroundFragment;
        if (fragment instanceof DetailsFragment || fragment instanceof PeopleDetailsFragment) {
            fragments = getSupportFragmentManager().getFragments();
            backgroundFragment = fragments.get(fragments.size() - 1);

        } else {
            backgroundFragment = getSupportFragmentManager().findFragmentById(R.id.master_fragments_container);
        }

        if (backgroundFragment != null) showBackgroundFragment(backgroundFragment);
    }

    private void showBackgroundFragment(@NonNull Fragment backgroundFragment) {
        if (backgroundFragment.getView() == null) return;

        //show hidden background fragment
        backgroundFragment.getView().findViewById(R.id.top_layout).setVisibility(View.VISIBLE);

        if (!(backgroundFragment instanceof DetailsFragment || backgroundFragment instanceof PeopleDetailsFragment)) {
            //set toolbar
            Toolbar toolbar = backgroundFragment.getView().findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            //restore search bar visibility if title is hidden
            //exclude fragments that don't have search bar
            if (!(backgroundFragment instanceof HomeFragment)) {
                TextView titleTextView =
                        backgroundFragment.getView().findViewById(R.id.title_text_view);

                if (titleTextView.getVisibility() != View.VISIBLE) {
                    SuperEditText searchTextView =
                            backgroundFragment.getView().findViewById(R.id.search_edit_text_view);
                    showSearchBar(searchTextView);
                }
            }
        }
    }

    public boolean isTabletLandscape(){
        return mIsTabletLandscape;
    }

    public boolean hasFragment(int containerId) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(containerId);

        return fragment != null;
    }

    public void launchDetailsFragment(MediaData selectedData, String quickAction) {
        //catch error from restoring fragments after configuration change
        try {
            getSupportFragmentManager().executePendingTransactions();

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        int mediaType = selectedData instanceof MovieData ? MEDIA_TYPE_MOVIE : MEDIA_TYPE_SERIES;

        String[] detailUrls = getResources().getStringArray(R.array.details_urls);
        String detailUrl = null;

        if (mediaType == MEDIA_TYPE_MOVIE) {
            detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        } else if (mediaType == MEDIA_TYPE_SERIES) {
            detailUrl = detailUrls[MEDIA_TYPE_SERIES];
        }

        Parcelable parceledData = Parcels.wrap(selectedData);
        SearchPreferences searchPreferences =  new SearchPreferences();
        Parcelable parceledSharedPreferences = Parcels.wrap(searchPreferences);

        DetailsFragment detailsFragment = DetailsFragment.newInstance(
                mediaType, detailUrl, parceledData, parceledSharedPreferences);

        if (quickAction != null) {
            detailsFragment.setQuickAction(quickAction);
        }

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                .add(R.id.detail_fragments_container, detailsFragment, DetailsFragment.FRAGMENT_KEY)
                .commit();
    }

    public void launchPeopleDetailsFragment(PersonData selectedData) {
        //catch error from restoring fragments after configuration change
        try {
            getSupportFragmentManager().executePendingTransactions();

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        SearchPreferences searchPreferences =  new SearchPreferences();
        PeopleDetailsFragment detailsFragment =
                PeopleDetailsFragment.newInstance(selectedData, searchPreferences);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_entry, R.anim.slide_left_exit)
                .add(R.id.detail_fragments_container, detailsFragment, PeopleDetailsFragment.FRAGMENT_KEY)
                .commit();
    }

    private void launchDetailsFromIntent(Intent intent, Bundle extras) {
        String quickAction = extras.getString(DetailsFragment.QUICK_ACTION_KEY);

        MediaData mediaData =
                Parcels.unwrap(intent.getParcelableExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY));

        launchDetailsFragment(mediaData, quickAction);
    }

    public void launchAddToListActivity(MediaData mediaData) {
        //fix for stack placeholder, do nothing if no id
        if (mediaData.getId() == null) return;

        int mediaType = mediaData instanceof MovieData ? MEDIA_TYPE_MOVIE : MEDIA_TYPE_SERIES;

        Intent intent = new Intent(getApplicationContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, mediaType);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(mediaData));

        startActivity(intent);
    }

    public static void launchCreateListActivity(final Activity activity) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                int userListCount = AppDatabase.getInstance(activity).
                        userListsDao().getAllListsAlt().size();

                if (userListCount >= FREE_MODE_LIST_COUNT_LIMIT && !sIsProMode) {
                    Snackbar.make(activity.findViewById(R.id.top_layout),
                            activity.getString(R.string.pro_mode_list_limit_message),
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(activity, CreateListActivity.class);
                intent.putExtra(CreateListActivity.MODE_KEY, CreateListActivity.MODE_CREATE);

                activity.startActivity(intent);
            }
        });
    }

    public static void launchConfirmationActivity(final Activity activity,
                                                  final int requestId,
                                                  final String action) {
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                Intent confirmationActivityIntent =
                        new Intent(activity, ConfirmationActivity.class);
                confirmationActivityIntent.setAction(action);
                activity.startActivityForResult(confirmationActivityIntent, requestId);
            }
        });
    }

    public static void launchConfirmationActivity(final Fragment fragment,
                                                  final Object selectedData, final int requestId,
                                                  final String action) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                Intent confirmationActivityIntent =
                        new Intent(fragment.getContext(), ConfirmationActivity.class);
                confirmationActivityIntent.setAction(action);
                confirmationActivityIntent.putExtra(ConfirmationActivity.SELECTED_DATA_KEY,
                        Parcels.wrap(selectedData));
                fragment.startActivityForResult(confirmationActivityIntent, requestId);
            }
        });
    }

    public static void launchConfirmationActivity(final Activity activity,
                                                  final Object selectedData, final int requestId,
                                                  final String action) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                Intent confirmationActivityIntent =
                        new Intent(activity, ConfirmationActivity.class);
                confirmationActivityIntent.setAction(action);
                confirmationActivityIntent.putExtra(ConfirmationActivity.SELECTED_DATA_KEY,
                        Parcels.wrap(selectedData));
                activity.startActivityForResult(confirmationActivityIntent, requestId);
            }
        });
    }

    public static void launchConfirmationActivity(final Fragment fragment,
                                                  final int requestId,
                                                  final String action) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                Intent confirmationActivityIntent =
                        new Intent(fragment.getContext(), ConfirmationActivity.class);
                confirmationActivityIntent.setAction(action);
                fragment.startActivityForResult(confirmationActivityIntent, requestId);
            }
        });
    }

    public void hideBackgroundFragment(Fragment fragment) {
        if (fragment.getView() != null) {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();

            //TODO: check if fragment is always last index(size -1), if so, no need to use indexOf() for fragment index,...
            // instead use size - 1
            int fragmentIndex = fragments.indexOf(fragment);
            int backgroundFragmentIndex = fragmentIndex - 1;

            Fragment backgroundFragment = fragments.get(backgroundFragmentIndex);

            if (backgroundFragment instanceof DetailsFragment) {
                backgroundFragmentIndex = backgroundFragmentIndex - 1;
                backgroundFragment = fragments.get(backgroundFragmentIndex);
            }

            hideFragment(backgroundFragment);

            //hide search bar and dismiss keyboard
            //exclude fragments that don't have search bar
            if (!(backgroundFragment instanceof HomeFragment)
                    && !(backgroundFragment instanceof DetailsFragment)
                    && !(backgroundFragment instanceof PeopleDetailsFragment)) {
                SuperEditText searchTextView =
                        backgroundFragment.getView().findViewById(R.id.search_edit_text_view);
                hideSearchBar(searchTextView);
            }
        }
    }

    private void hideFragment(Fragment fragment) {
        View backgroundFragmentParentView =
                fragment.getView().findViewById(R.id.top_layout);

        backgroundFragmentParentView.setVisibility(View.GONE);
    }

    public void onSearchButtonPressed(ImageButton searchButton,
                                      SuperEditText searchEditText, TextView toolbarTitle) {
        if (searchEditText.getVisibility() == View.VISIBLE) {
            searchEditText.setText("");
            searchButton.setImageResource(R.drawable.ic_manual_search);
            hideSearchBar(searchEditText);
            toolbarTitle.setVisibility(View.VISIBLE);

        } else {
            searchButton.setImageResource(R.drawable.ic_cancel_manual_search);
            searchEditText.setVisibility(View.VISIBLE);
            searchEditText.requestFocus();
            showSoftKeyboard(searchEditText);
            toolbarTitle.setVisibility(View.GONE);
        }
    }

    public static void restoreSearchIfAvailable(Fragment fragment, Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        String savedSearch = savedInstanceState.getString(SEARCH_TEXT_KEY);
        if (savedSearch == null || savedSearch.equals("")) return;

        SuperEditText searchTextView =
                fragment.getParentFragment().getView().findViewById(R.id.search_edit_text_view);

        //show keyboard if restore value is true
        //else hide it
        if (sIsKeyboardVisible) {
            showSoftKeyboard(searchTextView);

        } else {
            hideSoftKeyboard(searchTextView);
        }

        String savedSearchText = savedInstanceState.getString(SEARCH_TEXT_KEY);
        int savedBarVisibility = savedInstanceState.getInt(SEARCH_BAR_VISIBILITY_KEY);

        searchTextView.setText(savedSearchText);
        searchTextView.setVisibility(savedBarVisibility);

        final ImageButton searchButton =
                fragment.getParentFragment().getView().findViewById(R.id.search_image_button);

        if (savedBarVisibility == View.VISIBLE) {
            searchButton.setImageResource(R.drawable.ic_cancel_manual_search);

            fragment.getParentFragment()
                    .getView().findViewById(R.id.title_text_view).setVisibility(View.GONE);

        } else {
            searchButton.setImageResource(R.drawable.ic_manual_search);
        }
    }

    //hide soft keyboard and update keyboard visibility property
    public static void hideSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            sIsKeyboardVisible = false;
        }
    }

    //show soft keyboard and update keyboard visibility property
    static void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);

            sIsKeyboardVisible = true;
        }
    }

    //convenience method for hiding search bar edit text
    private void hideSearchBar(SuperEditText searchEditText) {
        hideSoftKeyboard(searchEditText);
        searchEditText.setVisibility(View.GONE);
    }

    //convenience method for showing search bar edit text
    private void showSearchBar(SuperEditText searchEditText) {
        searchEditText.setVisibility(View.VISIBLE);
    }

    public RoomDatabase.Callback databaseInitializer(final boolean isLoggingIn) {
        final boolean[] isRestored = {false};
        //reference
        //https://medium.com/@srinuraop/database-create-and-open-callbacks-in-room-7ca98c3286ab
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (isLoggingIn) {
                            addWatchLists();
                            restoreLatestBackup();

                            //prevent restoring again if database opens
                            isRestored[0] = true;
                        }
                    }
                });
            }

            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (isLoggingIn) {
                            //check if database has been restored already (from onCreate method)
                            if (!isRestored[0]) {
                                restoreLatestBackup();
                            }
                        }
                    }
                });
            }
        };
    }

    private void addWatchLists() {
        String[] seriesWatchListTitles = getResources()
                .getStringArray(R.array.watch_status_series_titles);
        for (String title: seriesWatchListTitles) {
            WatchListModel watchListModel = new WatchListModel(title);
            AppDatabase.getInstance(this).watchListsDao()
                    .addList(watchListModel);
        }
    }

    private void restoreLatestBackup() {
        Backup latestBackup = FirebaseUserDataDao.getLatestBackupAlt();
        if (latestBackup != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressLayout.setVisibility(View.VISIBLE);
                    showSnackBarMessage(getString(R.string.restoring_last_backup_message));
                }
            });
            //restore backup;
            Intent intent = new Intent(this, RestoreService.class);
            intent.putExtra(RestoreService.FOLDER_KEY, RestoreService.BACKUP_FOLDER_NAME);
            intent.putExtra(RestoreService.FILENAME_KEY, latestBackup.getFileName());
            RestoreService.enqueueWork(this, intent);
        }
    }

    private void showSnackBarMessage(String string) {
        if (string == null || string.equals("")) return;
        Snackbar.make(findViewById(R.id.top_layout), string, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onRestoreComplete() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressLayout.setVisibility(View.GONE);
                showSnackBarMessage(getString(R.string.restore_completed_message));
            }
        });
    }

    @Override
    public void onRestoreFailed() {
        addWatchLists();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressLayout.setVisibility(View.GONE);
                showSnackBarMessage(getString(R.string.restore_failed_message));
            }
        });
    }

    /**
     * Only called from test, creates and returns a new {@link SimpleIdlingResource}.
     */
//    @VisibleForTesting
    @NonNull
    public SimpleIdlingResource getIdlingResource() {
        if (mIdlingResource == null) {
            mIdlingResource = new SimpleIdlingResource();
        }
        return mIdlingResource;
    }
}