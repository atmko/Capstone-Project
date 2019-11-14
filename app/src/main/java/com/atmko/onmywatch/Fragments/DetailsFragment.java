/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.RateActivity;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.view_models.DetailsViewModel;
import com.atmko.onmywatch.view_models.DetailsViewModelFactory;
import com.atmko.onmywatch.view_models.FirebaseDetailsViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.DetailMovieExtrasAdapter;
import com.atmko.onmywatch.adapters.DetailSeriesExtrasAdapter;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.MovieDataParser;
import com.atmko.onmywatch.utils.SearchPreferences;
import com.atmko.onmywatch.utils.SeriesDataParser;
import com.atmko.onmywatch.utils.network_utils.NetworkFunctions;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;
import static com.atmko.onmywatch.models.MediaData.WATCH_STATUS_DROPPED;
import static com.atmko.onmywatch.models.MediaData.WATCH_STATUS_WATCHED;
import static com.atmko.onmywatch.models.MediaData.WATCH_STATUS_WATCHING;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;

public class DetailsFragment extends Fragment {
    public static final String FRAGMENT_KEY = "details_fragment";

    //TODO consider putting media type as an attribute in MediaData Class
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String DETAIL_URL_KEY = "detail_url";
    public static final String MEDIA_DATA_PARCELABLE_KEY = "media_data";
    private static final String SEARCH_PREFERENCES_KEY = "search_preferences";

    public static final String ACTION_LAUNCH_DETAILS = "launch_details";
    public static final String QUICK_ACTION_KEY = "quick_action";
    public static final String QUICK_ACTION_SHARE = "qa_share";
    public static final String QUICK_ACTION_RATE = "qa_rate";

    private int mMediaType;
    private String mDetailUrl;
    private MediaData mMediaData;
    private SearchPreferences mSearchPreferences;

    public static final int REVIEW_CUT_OFF_INDEX = 100;

    private Bundle mSavedInstanceState;
    private int mWatchStatus;

    private FloatingActionButton mFab;
    private ImageButton mShareButton;
    private ImageButton mRateButton;

    //details views
    private TabLayout mDetailExtrasTabLayout;
    private ViewPager mDetailExtrasViewPager;
    private TextView mReleaseStatusTextView;

    //values
    private int mOverviewCutoffIndex;

    //quick action string
    private String mQuickAction;

    public DetailsFragment() {
        // Required empty public constructor
    }

    public static DetailsFragment newInstance(int mediaType, String detailUrl,
                                              Parcelable mediaDataParcel,
                                              Parcelable searchPreferencesParcel) {
        DetailsFragment fragment = new DetailsFragment();
        Bundle args = new Bundle();
        args.putInt(MEDIA_TYPE_KEY, mediaType);
        args.putString(DETAIL_URL_KEY, detailUrl);
        args.putParcelable(MEDIA_DATA_PARCELABLE_KEY, mediaDataParcel);
        args.putParcelable(SEARCH_PREFERENCES_KEY, searchPreferencesParcel);
        fragment.setArguments(args);
        return fragment;
    }

    public void setQuickAction(String quickAction) {
        mQuickAction = quickAction;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMediaType = getArguments().getInt(MEDIA_TYPE_KEY);
            mDetailUrl = getArguments().getString(DETAIL_URL_KEY);
            mMediaData = Parcels.unwrap(getArguments().getParcelable(MEDIA_DATA_PARCELABLE_KEY));
            mSearchPreferences = Parcels.unwrap(getArguments().getParcelable(SEARCH_PREFERENCES_KEY));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_details, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //save saveInstanceState value for onCreateAnimator to check if this is the first instance
        mSavedInstanceState = savedInstanceState;

        try {
            defineViews();
            defineValues();
            observeViewModel();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }


        if (savedInstanceState == null) {
            //startup code moved to onCreateAnimator

        } else {
            try {
                setDetailViewValues();
                configureDetailExtrasAdapter();

            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        //basic values as opposed to values retrieved by getting details
        try {
            setBasicViewValues();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public Animator onCreateAnimator(int transit, final boolean enter, int nextAnim) {
        //if this is entry animation
        //&& if this is the first instance
        if (enter && mSavedInstanceState == null) {
            //create entry animator
            Animator animator = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
            //add animator listener
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    //run code after entry animation is complete
                    try {
                        //reserve focus by hiding background fragment
                        //TODO: NullPointerException handled in try block
                        //noinspection ConstantConditions
                        if (!((MasterActivity) getActivity()).isTabletLandscape()) {
                            ((MasterActivity) getActivity())
                                    .hideBackgroundFragment(DetailsFragment.this);
                        }

                        if (mQuickAction != null){
                            launchQuickAction();
                        }

                        getMediaDetails();

                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });

            return animator;

        }

        //return super method
        return super.onCreateAnimator(transit, enter, nextAnim);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //update initialized media data
        assert getArguments() != null;
        getArguments().putParcelable(MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mMediaData));

        outState.putString(ApiConstants.RELEASE_STATUS_KEY, mReleaseStatusTextView.getText().toString());
    }

    private static final String STATUS_BAR_IDENTIFIER = "status_bar_height";
    private static final String STATUS_BAR_IDENTIFIER_TYPE = "dimen";
    private static final String STATUS_BAR_IDENTIFIER_PACKAGE = "android";
    private int getStatusBarHeight() {
        //source: https://stackoverflow.com/questions/3407256/height-of-status-bar-in-android
        //user: Jorgesys
        //date: Aug 4, 2010
        int result = 0;
        int resourceId = getResources().getIdentifier
                (STATUS_BAR_IDENTIFIER, STATUS_BAR_IDENTIFIER_TYPE, STATUS_BAR_IDENTIFIER_PACKAGE);
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void defineViews() throws NullPointerException {
        //if mIsTablet landscape (2-pane), remove up navigation button from details layout
        ImageButton upNavigationButton = getView().findViewById(R.id.up_navigation_button);
        upNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        if (((MasterActivity) getActivity()).isTabletLandscape()) {
            upNavigationButton.setVisibility(View.GONE);
        }

        mFab = getView().findViewById(R.id.add_to_list_fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MasterActivity) getActivity()).launchAddToListActivity(mMediaData);
            }
        });

        //configure share button
        mShareButton = getView().findViewById(R.id.share_button);
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    launchShareWindow();

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });

        //configure rate button
        mRateButton = getView().findViewById(R.id.rate_button);
        mRateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    launchRateActivity();

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });

        //configure trailer button
        getView().findViewById(R.id.trailer_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, String> videoData = mMediaData.getVideos().get(0);
                String path = videoData.get(ApiConstants.VIDEO_PATH_KEY);

                Uri fullVideoPath = Uri.parse((ApiConstants.YOUTUBE_INTENT_BASE_URL + path));

                Intent videoIntent = new Intent(Intent.ACTION_VIEW, fullVideoPath);
                if (videoIntent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivity(videoIntent);

                }
            }
        });

        //define views dependent on retrieving details
        mDetailExtrasTabLayout = getView().findViewById(R.id.detail_extras_tab_layout);
        mDetailExtrasViewPager = getView().findViewById(R.id.details_extra_view_pager);

        try {
            //configure show more overview button
            getView().findViewById(R.id.show_more_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((TextView) getView().findViewById(R.id.overView_text_view))
                            .setText(limitText(mMediaData.getOverview(), mOverviewCutoffIndex));
                }
            });

            configureBackDropDimensions();
            configureDetailExtrasSize();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        mReleaseStatusTextView = getView().findViewById(R.id.release_status_text);

    }

    private void defineValues() {
        mOverviewCutoffIndex = getResources().getInteger(R.integer.detail_overview_cutoff_index);
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void configureDetailExtrasSize() throws NullPointerException{
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int pixelHeight = displayMetrics.heightPixels;
        int pixelWidth = displayMetrics.widthPixels;

        int pixelStatusBarHeight = getStatusBarHeight();

        final ConstraintLayout includeDetailsExtras = getView().findViewById(R.id.include_details_extras);

        int masterContainerWeight;
        int detailContainerWeight;

        if (!getResources().getBoolean(R.bool.isPhoneLandscape)) {
            masterContainerWeight = getResources().getInteger(R.integer.master_fragment_layout_weight);
            detailContainerWeight = getResources().getInteger(R.integer.detail_fragment_layout_weight);

        } else {
            masterContainerWeight = getResources().getInteger(R.integer.details_main_layout_weight);
            detailContainerWeight = getResources().getInteger(R.integer.details_extras_layout_weight);

        }

        int weightTotal = masterContainerWeight + detailContainerWeight;

        int weightedWidth;

        //get total weightedWidth
        if (((MasterActivity) getActivity()).isTabletLandscape()
                || getResources().getBoolean(R.bool.isPhoneLandscape)) {
            weightedWidth = pixelWidth * detailContainerWeight/weightTotal;

        } else {
            weightedWidth = pixelWidth;

        }

        FrameLayout.LayoutParams detailExtrasParams =
                new FrameLayout.LayoutParams(weightedWidth, pixelHeight - pixelStatusBarHeight);

        includeDetailsExtras.setLayoutParams(detailExtrasParams);
    }

    //observe view models to get watch status, lists containing this media
    //TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void observeViewModel() throws NullPointerException{
        final ViewModel viewModel;
        final LiveData mediaDataLiveData;
        final LiveData<List<String>> containingUserLists;

        AppDatabase database = AppDatabase.getInstance(getContext());
        DetailsViewModelFactory viewModelFactory =
                new DetailsViewModelFactory(database, mMediaType, mMediaData.getId());

        if (MasterActivity.isProMode()) {
            viewModel =
                    ViewModelProviders.of(this,
                            viewModelFactory).get(FirebaseDetailsViewModel.class);
            mediaDataLiveData = ((FirebaseDetailsViewModel) viewModel).getMediaData();
            containingUserLists = ((FirebaseDetailsViewModel) viewModel).getContainingLists();

        } else {
            viewModel =
                    ViewModelProviders.of(this,
                            viewModelFactory).get(DetailsViewModel.class);
            mediaDataLiveData = ((DetailsViewModel) viewModel).getMediaData();
            containingUserLists = ((DetailsViewModel) viewModel).getContainingLists();
        }

        mediaDataLiveData.observe(this, new Observer<Object>() {
            @Override
            public void onChanged(Object mediaData) {
                MediaData castedMediaData = ((MediaData) mediaData);

                mWatchStatus = mediaData != null ? castedMediaData.getWatchStatus() : 0;

                //get array of watch status title shorthand
                String[] watchStatusShorthandList =
                        getContext().getResources().getStringArray(R.array.watch_status_shorthand_titles);

                //get shorthand using watch status as index
                String shorthand = mediaData != null
                        ? watchStatusShorthandList[mWatchStatus]
                        : watchStatusShorthandList[0];

                //set shorthand text
                ((TextView) getView().findViewById(R.id.watch_status_shorthand_text))
                        .setText(shorthand);

                //configure user rating related UI
                int userRating = mediaData != null ? castedMediaData.getUserRating() : 0;
                if (userRating != 0) {
                    TextView userRatingTextView = getView().findViewById(R.id.user_rating_text);
                    userRatingTextView.setVisibility(View.VISIBLE);
                    userRatingTextView.setText(userRating + ".0");

                } else {
                    getView().findViewById(R.id.user_rating_text).setVisibility(View.GONE);

                }
            }
        });

        containingUserLists.observe(this, new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> listNames) {
                //get list size
                int containingListsCount = listNames != null ? listNames.size() : 0;

                //set counts text
                ((TextView) getView().findViewById(R.id.list_counts_text))
                        .setText(String.valueOf(containingListsCount));
            }
        });
    }

    private void launchQuickAction() {
        try {
            if (mQuickAction.equals(QUICK_ACTION_SHARE)) {
                launchShareWindow();

            } else if (mQuickAction.equals(QUICK_ACTION_RATE)) {
                launchRateActivity();

            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void getMediaDetails() throws NullPointerException {
        String id = mMediaData.getId();

        //build AN request
        ANRequest request = NetworkFunctions.agnosticDetailRequestById(mDetailUrl, id,
                mSearchPreferences, getContext());

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String returnedJSONString) {
                try {
                    //parse and populate retrieved data
                    if (mMediaType == MEDIA_TYPE_MOVIE) {
                        mMediaData =
                                MovieDataParser.parseDetails(returnedJSONString, ((MovieData) mMediaData));

                    } else {
                        mMediaData =
                                SeriesDataParser.parseDetails(returnedJSONString,
                                        ((SeriesData) mMediaData), getContext());

                    }

                    //todo implement get details for people data

                    setDetailViewValues();

                    configureDetailExtrasAdapter();

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError);

                    return;
                }

                //notify user of error
                Snackbar.make(getActivity().findViewById(R.id.top_layout),
                        getString(R.string.details_error_message), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void retryAfterCoolDOwn(ANError anError) {
        Log.d(FRAGMENT_KEY, "retrying details fetch");

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
                    getMediaDetails();

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

            }
        }, coolDownInMilliSecs);
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void configureDetailExtrasAdapter() throws NullPointerException {
        //remove old tabs
        mDetailExtrasTabLayout.removeAllTabs();

        //add new tabs
        String[] titleList = null;

        FragmentStatePagerAdapter extrasAdapter = null;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            titleList = getContext().getResources().getStringArray(R.array.detail_movie_extras_titles);
            extrasAdapter = new DetailMovieExtrasAdapter(getChildFragmentManager(),
                    FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, ((MovieData) mMediaData));

        } else if (mMediaType == MEDIA_TYPE_SERIES){
            titleList = getContext().getResources().getStringArray(R.array.detail_tv_extras_titles);
            extrasAdapter = new DetailSeriesExtrasAdapter(getChildFragmentManager(),
                    FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, ((SeriesData) mMediaData));
        }

        for (String title : titleList) {
            mDetailExtrasTabLayout.addTab(mDetailExtrasTabLayout.newTab().setText(title));
        }

        mDetailExtrasViewPager.setAdapter(extrasAdapter);

        //clear old listeners to avoid conflicts
        mDetailExtrasViewPager.clearOnPageChangeListeners();

        //configure new listeners
        mDetailExtrasViewPager.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(mDetailExtrasTabLayout));
        mDetailExtrasTabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mDetailExtrasViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void setBasicViewValues() throws NullPointerException {
        NetworkFunctions.loadImage(getActivity().getApplicationContext(), mMediaData.getBackdropPath(),
                ((ImageView) getView().findViewById(R.id.backdrop_image_view)));

        ((TextView) getView().findViewById(R.id.title_text_view))
                .setText(mMediaData.getFormattedTitle());

        ((TextView) getView().findViewById(R.id.date_text_view)).setText(mMediaData.getReleaseDate());

        //TODO implement maturity rating
        ((TextView) getView().findViewById(R.id.maturity_rating_text_view))
                .setText(getString(R.string.maturity_rating_placeholder));

        ((TextView) getView().findViewById(R.id.rating_text_view)).setText(mMediaData.getVoteAverage());

        try {
            //set overview text
            ((TextView) getView().findViewById(R.id.overView_text_view)).setText(
                    limitText(mMediaData.getOverview(), mOverviewCutoffIndex));

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void configureBackDropDimensions() throws NullPointerException {
        ImageView backdropImageView = getView().findViewById(R.id.backdrop_image_view);

        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int pixelWidth = displayMetrics.widthPixels;

        int masterContainerWeight;
        int detailContainerWeight;

        if (!getResources().getBoolean(R.bool.isPhoneLandscape)) {
            masterContainerWeight = getResources().getInteger(R.integer.master_fragment_layout_weight);
            detailContainerWeight = getResources().getInteger(R.integer.detail_fragment_layout_weight);

        } else {
            masterContainerWeight = getResources().getInteger(R.integer.details_main_layout_weight);
            detailContainerWeight = getResources().getInteger(R.integer.details_extras_layout_weight);

        }

        int weightTotal = masterContainerWeight + detailContainerWeight;

        int weightedWidth;

        //get total weightedWidth
        if (((MasterActivity) getActivity()).isTabletLandscape()
                || getResources().getBoolean(R.bool.isPhoneLandscape)) {

            weightedWidth = pixelWidth * detailContainerWeight/weightTotal;

        } else {
            weightedWidth = pixelWidth;

        }

        Long backdropHeight = Math.round(weightedWidth * ApiConstants.BACKDROP_HEIGHT_FACTOR);

        ConstraintLayout.LayoutParams params =
                new ConstraintLayout.LayoutParams(weightedWidth, backdropHeight.intValue());

        backdropImageView.setLayoutParams(params);
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void setDetailViewValues() throws NullPointerException {
        mReleaseStatusTextView.setText(mMediaData.getReleaseStatus());

        //set trailer button visibility
        try {
            //checks for trailer
            mMediaData.getVideos().get(0);
            getView().findViewById(R.id.trailer_button).setVisibility(View.VISIBLE);

        } catch (IndexOutOfBoundsException e) {
            getView().findViewById(R.id.trailer_button).setVisibility(View.GONE);

        }

        //set Genres
        ArrayList<String> genres = mMediaData.getGenres();

        if (genres != null) {
            try {
                ((TextView) getView().findViewById(R.id.genre_0_text_view))
                        .setText(genres.get(0));

            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();

                getView().findViewById(R.id.genre_0_text_view).setVisibility(View.GONE);

            }

            try {
                ((TextView) getView().findViewById(R.id.genre_1_text_view))
                        .setText(genres.get(1));

            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();

                getView().findViewById(R.id.genre_1_text_view).setVisibility(View.GONE);
            }
        }
    }

    //limit long text
    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private String limitText(String fullText, int cutOffIndex) throws NullPointerException {
        //get show more button
        ImageButton showMoreButton = getView().findViewById(R.id.show_more_button);
        showMoreButton.setVisibility(View.VISIBLE);

        //return reduced text if...
        //if overview length is more than cut off index
        //&& show more button tag is show less
        if (fullText.length() > cutOffIndex
                && (showMoreButton.getTag()).equals(getString(R.string.detail_overview_tag_show_less))) {
            String reducedText = fullText.subSequence(0, cutOffIndex) + "...";

            //toggle values
            showMoreButton.setImageResource(R.drawable.ic_show_more);
            showMoreButton.setTag(getString(R.string.detail_overview_tag_show_more));

            return reducedText;


        } else {//return full text
            //hide show more button
            if (fullText.length() <= cutOffIndex)showMoreButton.setVisibility(View.GONE);

            //toggle values
            showMoreButton.setImageResource(R.drawable.ic_show_less);
            showMoreButton.setTag(getString(R.string.detail_overview_tag_show_less));

            return fullText;
        }
    }

    private void launchShareWindow() throws NullPointerException {
        //noinspection ConstantConditions
        ShareCompat.IntentBuilder
                .from(getActivity())
                .setType("text/plain")
                .setChooserTitle(getString(R.string.detail_share_title))
                .setText(mMediaData.getMediaUrl(getContext(), mMediaData.getId()))
                .startChooser();
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void launchRateActivity() throws NullPointerException {
        if (mWatchStatus != WATCH_STATUS_WATCHING
                && mWatchStatus != WATCH_STATUS_WATCHED
                && mWatchStatus != WATCH_STATUS_DROPPED) {

            Snackbar.make(getActivity().findViewById(R.id.top_layout),
                    getString(R.string.rating_disallowed_message),
                    Snackbar.LENGTH_LONG).show();

            return;

        }

        Intent intent = new Intent(getActivity().getApplicationContext(), RateActivity.class);
        intent.putExtra(RateActivity.MEDIA_TYPE_KEY, mMediaType);
        intent.putExtra(RateActivity.MEDIA_ID_KEY, Parcels.wrap(mMediaData.getId()));

        startActivity(intent);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        Log.d(FRAGMENT_KEY, "detaching fragment");
    }
}
