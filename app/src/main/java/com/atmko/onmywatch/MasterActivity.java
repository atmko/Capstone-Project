/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.atmko.onmywatch.Fragments.DetailsFragment;
import com.atmko.onmywatch.Fragments.DiscoverCustomResultsFragment;
import com.atmko.onmywatch.Fragments.DiscoverParentFragment;
import com.atmko.onmywatch.Fragments.HomeFragment;
import com.atmko.onmywatch.Fragments.ListResultsParentFragment;
import com.atmko.onmywatch.Fragments.ListWatchAndUserFragment;
import com.atmko.onmywatch.Fragments.ListsWatchAndUserParentFragment;
import com.atmko.onmywatch.Fragments.PeopleDetailsFragment;
import com.atmko.onmywatch.adapters.ListWatchAndUserAdapter;
import com.atmko.onmywatch.adapters.ListsAdapter;
import com.atmko.onmywatch.custom_views.SuperEditText;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.database.daos.FirebaseUserDataDao;
import com.atmko.onmywatch.models.Backup;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaLog;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieLog;
import com.atmko.onmywatch.models.PersonData;
import com.atmko.onmywatch.models.SimpleIdlingResource;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.api_utils.SearchPreferences;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.BackupService;
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.BackupWorker;
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker;
import com.atmko.onmywatch.view_models.MasterActivityViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import hotchemi.android.rate.AppRate;

import static com.atmko.onmywatch.ConfirmationActivity.ACTION_PENDING_PURCHASE;

public class MasterActivity extends AppCompatActivity implements
        BackupService.OnBackupCompleteListener,
        ListsWatchAndUserParentFragment.ListFragmentImplementation,
        ListWatchAndUserAdapter.LogicImplementation,
        ListWatchAndUserFragment.OnListModelClickListener,
        BillingClientStateListener,
        PurchasesUpdatedListener {
    private static final String TAG = MasterActivity.class.getSimpleName();

    private static final int FREE_MODE_LIST_COUNT_LIMIT = 3;

    public static final int MEDIA_TYPE_SERIES = 0;
    public static final int MEDIA_TYPE_MOVIE = 1;
    public static final int MEDIA_TYPE_PEOPLE = 2;

    public static final String IS_LOGGING_IN_KEY = "is_logging_in";
    private static final String INITIAL_PRO_CHECK_KEY = "initial_pro_check";
    private static final String KEYBOARD_VISIBILITY_KEY = "keyboard_visibility";

    public static final String SEARCH_TEXT_KEY = "search_text";
    public static final String SEARCH_BAR_VISIBILITY_KEY = "visible_search_bar";

    private static final String UPDATE_MEDIA_WORKER_KEY = "update_media_worker";
    private static final String BACKUP_WORKER_KEY = "backup worker";
    private static final int UPDATE_REPEAT_INTERVAL = 18;
    private static final int BACKUP_REPEAT_INTERVAL = 12;
    private static final int INITIAL_DELAY = 15;

    private static final String USER_COLLECTION_PATH = "users";

    private static final int REQUEST_LOG_OUT = 20;
    public static final int REQUEST_GUEST_LOG_OUT = 30;

    private static final int PENDING_PURCHASE_ID = 1;

    //for restoring keyboard visibility upon configuration change
    public static boolean sIsKeyboardVisible;
    private boolean mIsTabletLandscape;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private FirebaseAnalytics mFirebaseAnalytics;

    private Bundle mSavedInstanceState;

    private FrameLayout progressLayout;

    // The Idling Resource which will be null in production.
    @Nullable
    public static SimpleIdlingResource sIdlingResource;

    public static boolean sIsProMode;

    //checks if sIsProMode value has be observed from view model
    private boolean mInitialProCheck = true;

    private static BillingClient mBillingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
            startLaunchActivity();

        } else {
            boolean isLoggingIn = getIntent().getBooleanExtra(IS_LOGGING_IN_KEY, false);
            getIntent().removeExtra(IS_LOGGING_IN_KEY);
            //observe user data via view model
            observeData(isLoggingIn);
        }
    }

    private void startReviewsLibrary() {
        AppRate.with(this)
                .setInstallDays(1) // default 10, 0 means install day.
                .setLaunchTimes(3) // default 10
                .setRemindInterval(2) // default 1
                .setShowLaterButton(true) // default true

                .monitor();

        // Show a dialog if meets conditions
        AppRate.showRateDialogIfMeetsConditions(this);
    }

    private void startBillingClient() {
        if (mBillingClient != null) {
            if (mBillingClient.isReady()) {
                queryPurchases(this);

            } else {
                mBillingClient.startConnection(this);
            }
        } else {
            mBillingClient = BillingClient.newBuilder(this)
                    .enablePendingPurchases()
                    .setListener(this).build();
            mBillingClient.startConnection(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mInitialProCheck) startBillingClient();
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
        if (sIdlingResource != null) {
            sIdlingResource.setIdleState(false);
        }

        if (isLoggingIn) {
            addWatchListsAndCheckForBackups();
        }

        AppDatabase.getInstance(this);

        MasterActivityViewModel masterActivityViewModel =
                ViewModelProviders.of(this).get(MasterActivityViewModel.class);

        masterActivityViewModel.getIsProModeLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isProMode) {
                if (isProMode == null) return;

                //update pro mode variable and shared preference
                sIsProMode = isProMode;
                getSharedPreferences(getString(R.string.application_shared_prefs_key),
                        Context.MODE_PRIVATE).edit()
                        .putBoolean(getString(R.string.is_pro_mode_key), isProMode)
                        .apply();

                //activate billing client here only if mInitialProCheck
                //subsequent billing client calls handled in on resume
                //this is necessary to avoid unneeded purchase verifications because sIsProMode...
                //...is always false before observing data and purchase verification happens if...
                //...sIsProMode is false
                if (mInitialProCheck) {
                    startBillingClient();
                    mInitialProCheck = false;
                }

                if (getSupportFragmentManager().getFragments().size() == 0) loadUi();
                //start background work managers
                startWorkers();
            }
        });
    }

    private void loadUi() {
        startReviewsLibrary();

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
                launchDetailsFromIntent(intent);
            }
        }

        getSupportFragmentManager().executePendingTransactions();

        if (sIdlingResource != null) {
            sIdlingResource.setIdleState(true);
        }
    }

    private void startLaunchActivity() {
        Intent launchIntent = new Intent(getApplicationContext(), LaunchActivity.class);
        finish();
        startActivity(launchIntent);
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

        //remove pro mode value
        getSharedPreferences(getString(R.string.application_shared_prefs_key),
                Context.MODE_PRIVATE).edit()
                .putBoolean(getString(R.string.is_pro_mode_key), false)
                .apply();

        startLaunchActivity();
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
                    showSnackBarMessage(errorMessage, MasterActivity.this);

                } else {
                    showSnackBarMessage(getString(R.string.backup_failure_message), MasterActivity.this);
                }

                launchConfirmationActivity(MasterActivity.this,
                        REQUEST_LOG_OUT, ConfirmationActivity.ACTION_LOG_OUT);
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                //if returning from firebase sign in activity, configure database and observe data
                if (requestCode == REQUEST_LOG_OUT) {
                    if (resultCode == RESULT_OK) {
                        logOut();
                    }

                } else if (requestCode == REQUEST_GUEST_LOG_OUT) {
                    if (resultCode == RESULT_OK) {
                        getCurrentUser().delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        logOut();
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                showSnackBarMessage("Log out failed", MasterActivity.this);
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null
                && intent.getAction().equals(DetailsFragment.ACTION_LAUNCH_DETAILS)) {
            launchDetailsFromIntent(intent);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //save initial pro check value
        outState.putBoolean(INITIAL_PRO_CHECK_KEY, mInitialProCheck);
        //save keyboard visibility value
        outState.putBoolean(KEYBOARD_VISIBILITY_KEY, sIsKeyboardVisible);
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
        if (fragment.getView() == null) return;
        if (!mIsTabletLandscape && hasFragment(R.id.detail_fragments_container)) {
            //hide background fragment to reserve keyboard focus for newly loaded fragment
            fragment.getView().findViewById(R.id.top_layout).setVisibility(View.GONE);

        }

        //condition 2
        //compare master fragment name and resumed fragment name
        //if no match, hide fragment

        if (fragment.getView() == null) return;
        Fragment masterContainerFragment =
                getSupportFragmentManager()
                        .findFragmentById(R.id.master_fragments_container);

        if (masterContainerFragment == null) return;
        String masterContainerFragmentName = masterContainerFragment.getClass().getSimpleName();

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
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build();

        PeriodicWorkRequest updateMediaDataRequest = new PeriodicWorkRequest.Builder(
                UpdateMediaWorker.class, UPDATE_REPEAT_INTERVAL, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(INITIAL_DELAY, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                UPDATE_MEDIA_WORKER_KEY, ExistingPeriodicWorkPolicy.KEEP, updateMediaDataRequest);

        PeriodicWorkRequest backupRequest = new PeriodicWorkRequest.Builder(
                BackupWorker.class, BACKUP_REPEAT_INTERVAL, TimeUnit.HOURS)
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

        } else {
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
            if (fragments.size() > 0) {
                fragment = fragments.get(fragments.size() - 1);

            } else {
                finish();
                return;
            }
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
                    .setCustomAnimations(R.animator.slide_right_entry, R.animator.slide_left_exit)
                    .remove(fragment).commit();

        } else {
            //condition for exit animation transition
            if (fragment instanceof ListResultsParentFragment) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.animator.slide_right_entry, R.animator.slide_left_exit)
                        .remove(fragment).commit();

            } else if (fragment instanceof DiscoverParentFragment) {
                DiscoverParentFragment parentFragment = ((DiscoverParentFragment) fragment);
                FragmentManager fragmentManager = parentFragment.getChildFragmentManager();
                Fragment childFragment = fragmentManager.getFragments().get(0);

                if (childFragment instanceof DiscoverCustomResultsFragment) {
                    DiscoverCustomResultsFragment customResultsFragment =
                            ((DiscoverCustomResultsFragment) childFragment);
                    if (customResultsFragment.isCurrentTab() && customResultsFragment.isSearchActive()) {
                        customResultsFragment.setSearchActive(false);

                    } else {
                        removeFragment(fragment, R.animator.slide_down_entry, R.animator.slide_up_exit);
                    }
                } else {
                    removeFragment(fragment, R.animator.slide_down_entry, R.animator.slide_up_exit);
                }
            } else {
                removeFragment(fragment, R.animator.slide_down_entry, R.animator.slide_up_exit);
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

    @SuppressWarnings("SameParameterValue")
    private void removeFragment(Fragment fragment, int entry, int exit) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(entry, exit)
                .remove(fragment)
                .commit();
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
        String detailUrl;

        if (mediaType == MEDIA_TYPE_MOVIE) {
            detailUrl = detailUrls[MEDIA_TYPE_MOVIE];

        } else {
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
                .setCustomAnimations(R.animator.slide_right_entry, R.animator.slide_left_exit)
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
                .setCustomAnimations(R.animator.slide_right_entry, R.animator.slide_left_exit)
                .add(R.id.detail_fragments_container, detailsFragment, PeopleDetailsFragment.FRAGMENT_KEY)
                .commit();
    }

    private void launchDetailsFromIntent(final Intent intent) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                Bundle extras = intent.getExtras();

                String quickAction =
                        extras != null ? extras.getString(DetailsFragment.QUICK_ACTION_KEY) : null;

                Object object =
                        Parcels.unwrap(intent.getParcelableExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY));

                //if object is media log, media log id to get parent media data
                if (object instanceof MediaLog) {
                    MediaLog mediaLog = ((MediaLog) object);
                    if (mediaLog instanceof MovieLog) {
                        object = AppDatabase.getLocalDatabase(MasterActivity.this).movieDataDao()
                                .getMovieByIdAlt(mediaLog.parentId);

                    } else {
                        object = AppDatabase.getLocalDatabase(MasterActivity.this).seriesDataDao()
                                .getSeriesByIdAlt(mediaLog.parentId);
                    }
                }

                launchDetailsFragment(((MediaData) object), quickAction);
            }
        });
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

    private static void launchConfirmationActivity(final Activity activity,
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

    public static void launchConfirmationActivity(final Activity activity,
                                                  final int requestId,
                                                  final String action,
                                                  final int confirmationLimit) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                Intent confirmationActivityIntent =
                        new Intent(activity, ConfirmationActivity.class);
                confirmationActivityIntent.setAction(action);
                confirmationActivityIntent.putExtra(ConfirmationActivity.COUNTER_LIMIT_KEY,
                        confirmationLimit);
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
        if (fragment.getView() == null) return;
        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        //TODO: check if fragment is always last index(size -1), if so, no need to use indexOf() for fragment index,...
        // instead use size - 1
        int fragmentIndex = fragments.indexOf(fragment);
        int backgroundFragmentIndex = fragmentIndex - 1;

        if (backgroundFragmentIndex < 0 || backgroundFragmentIndex >= fragments.size()) return;
        Fragment backgroundFragment = fragments.get(backgroundFragmentIndex);

        if (backgroundFragment == null) return;
        if (backgroundFragment instanceof DetailsFragment) {
            backgroundFragmentIndex = backgroundFragmentIndex - 1;
            backgroundFragment = fragments.get(backgroundFragmentIndex);
        }

        if (backgroundFragment == null) return;
        if (backgroundFragment.getView() == null) return;
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

    private void hideFragment(Fragment fragment) {
        if (fragment.getView() == null) return;
        View backgroundFragmentParentView =
                fragment.getView().findViewById(R.id.top_layout);

        backgroundFragmentParentView.setVisibility(View.GONE);
    }

    public static void onSearchButtonPressed(ImageButton searchButton,
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
        if (fragment.getParentFragment() == null) return;
        if (fragment.getParentFragment().getView() == null) return;

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
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            sIsKeyboardVisible = false;
        }
    }

    //show soft keyboard and update keyboard visibility property
    static void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);

            sIsKeyboardVisible = true;
        }
    }

    //convenience method for hiding search bar edit text
    private static void hideSearchBar(SuperEditText searchEditText) {
        hideSoftKeyboard(searchEditText);
        searchEditText.setVisibility(View.GONE);
    }

    //convenience method for showing search bar edit text
    private void showSearchBar(SuperEditText searchEditText) {
        searchEditText.setVisibility(View.VISIBLE);
    }

    private void addWatchListsAndCheckForBackups() {
        showSnackBarMessage(getString(R.string.checking_for_backups_message), this);
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                addWatchLists();
                checkForBackups();
            }
        });
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

    private void checkForBackups() {
        List<Backup> backups = FirebaseUserDataDao.getBackupsAlt();
        if (backups.size() != 0) {
            Intent intent = new Intent(MasterActivity.this, RestoreActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    public void setProgressVisibility(int visibility) {
        progressLayout.setVisibility(visibility);
    }

    public static void showSnackBarMessage(String string, Activity activity) {
        if (string == null || string.equals("")) return;
        Snackbar.make(activity.findViewById(R.id.top_layout), string, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Only called from test, creates and returns a new {@link SimpleIdlingResource}.
     */
//    @VisibleForTesting
    @NonNull
    public static SimpleIdlingResource getIdlingResource() {
        if (sIdlingResource == null) {
            sIdlingResource = new SimpleIdlingResource();
        }
        return sIdlingResource;
    }

    @Override
    public void onListFragmentResume(Fragment fragment) {
        onResumeMasterContainerFragment(fragment);
    }

    @Override
    public void onAnimationEnd(Fragment fragment) {
        hideBackgroundFragment(fragment);
    }

    @Override
    public Fragment launchFragment(int position) {
        ListWatchAndUserFragment fragment;
        if (position == ListsWatchAndUserParentFragment.LIST_TYPE_WATCH) {
            fragment = ListWatchAndUserFragment
                    .newInstance(ListsWatchAndUserParentFragment.LIST_TYPE_WATCH, false);
        } else  {
            fragment = ListWatchAndUserFragment
                    .newInstance(ListsWatchAndUserParentFragment.LIST_TYPE_USER, true);
        }

        return fragment;
    }

    @Override
    public void onListModelClick(ListsAdapter adapter, Fragment childFragment, int listType,
                                 ListModel listModel) {
        if (adapter.inPlaceholderMode()) {
            if (childFragment.getParentFragment() != null) {
                launchCreateListActivity(this);
                return;
            }
        }

        if (childFragment.getParentFragment() == null) return;
        if (childFragment.getParentFragment().getActivity() == null) return;
        Fragment fragment = ListResultsParentFragment.newInstance(listType, listModel.getName());
        childFragment.getParentFragment().getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.slide_right_entry, R.animator.slide_left_exit)
                .add(R.id.master_fragments_container, fragment, ListResultsParentFragment.FRAGMENT_KEY)
                .commit();
    }

    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        //if connection successful
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            queryPurchases(this);
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        mBillingClient.startConnection(this);
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase, this);
            }

        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            showSnackBarMessage(billingResult.getDebugMessage(), this);
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            showSnackBarMessage(billingResult.getDebugMessage(), this);
            // Handle any other error codes.
        }
    }

    //get purchases made by user
    //ensures app values match purchases
    private static void queryPurchases(Activity activity) {
        Log.d(TAG, "fetch user's purchases");
        //get in app purchases to check for unverified, unacknowledged and pending transactions
        Purchase.PurchasesResult inAppResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        List<Purchase> inAppPurchases = inAppResult.getPurchasesList();

        if (inAppResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && inAppPurchases != null) {
            //iterate through list and ensure that app values match purchases
            //handle purchases if app values are false
            for (Purchase purchase : inAppPurchases) {
                if (purchase.getSku().equals("pro_mode") && !MasterActivity.sIsProMode) {
                    Log.d(TAG, "handling pro_mode");
                    handlePurchase(purchase, activity);
                }
            }
        } else {
            showSnackBarMessage(inAppResult.getBillingResult().getDebugMessage(), activity);
        }
    }

    static void handlePurchase(final Purchase purchase, final Activity activity) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            showSnackBarMessage("Your Purchase Has Been Completed", activity);

            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams,
                        new AcknowledgePurchaseResponseListener() {
                            @Override
                            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    Log.d(TAG, "Purchase Acknowledged");
                                    // Grant entitlement to the user.
                                    verifyPurchase(purchase, activity);
                                } else {
                                    Log.d(TAG, "Acknowledgement Failed");
                                }
                            }
                        });
            } else {
                // Grant entitlement to the user.
                verifyPurchase(purchase, activity);
            }

        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            MasterActivity.launchConfirmationActivity(activity,
                    PENDING_PURCHASE_ID, ACTION_PENDING_PURCHASE);
            //inform user of steps to complete purchase
        }
    }

    private static void verifyPurchase(final Purchase purchase, final Activity activity) {
        List<String> credentials = new ArrayList<>();
        credentials.add(purchase.getSku());
        credentials.add(purchase.getPurchaseToken());
        FirebaseFunctions.getInstance().getHttpsCallable("verifyPurchase").call(credentials)
                .addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
                    @Override
                    public void onSuccess(HttpsCallableResult httpsCallableResult) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> results = ((Map<String, String>) httpsCallableResult.getData());
                        if (results != null) {
                            if (results.get("error") != null) {
                                showSnackBarMessage(results.get("error"), activity);

                            } else if (results.get("status") != null) {
                                //query purchases to update check marks on purchases
                                showSnackBarMessage("Purchase Verified", activity);
                                justTurnedPro(activity);
                            }
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e.getMessage() != null) Log.d(TAG, e.getMessage());
                Log.d(TAG, "Purchase Verification Failed");
                showSnackBarMessage("sever down, purchase will complete when server available", activity);
            }
        });
    }

    private static void justTurnedPro(Activity activity) {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build();

        OneTimeWorkRequest proUpdateForNotifications = new OneTimeWorkRequest.Builder(
                UpdateMediaWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(activity).enqueue(proUpdateForNotifications);
    }
}