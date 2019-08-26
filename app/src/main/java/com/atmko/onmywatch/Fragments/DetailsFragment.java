package com.atmko.onmywatch.Fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.res.Resources;
import android.os.Bundle;
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
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.view_models.DetailsViewModel;
import com.atmko.onmywatch.view_models.DetailsViewModelFactory;
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

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

//import com.upkipp.onmywatch.HomeActivity;

public class DetailsFragment extends Fragment {
    public static String FRAGMENT_KEY = "details_fragment";

    //TODO consider putting media type as an attribute in MediaData Class
    public static String MEDIA_TYPE_KEY = "media_type";
    private static String DETAIL_URL_KEY = "detail_url";
    private static String MEDIA_DATA_PARCELABLE_KEY = "media_data";
    private static String SEARCH_PREFERENCES_KEY = "search_preferences";

    private int mMediaType;
    private String mDetailUrl;
    private MediaData mMediaData;
    private SearchPreferences mSearchPreferences;

    private Bundle mSavedInstanceState;

    private FloatingActionButton fab;
    private ImageButton shareButton;

    //details views
    private TabLayout detailExtrasTabLayout;
    private ViewPager detailExtrasViewPager;

    private TextView releaseStatusTextView;


    public DetailsFragment() {
        // Required empty public constructor
    }

    public static DetailsFragment newInstance(int mediaType, String detailUrl, Parcelable mediaDataParcel,
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_details, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //save saveInstanceState value for onCreateAnimator to check if this is the first instance
        mSavedInstanceState = savedInstanceState;

        defineViews();

        observeViewModel();

        if (savedInstanceState == null) {
            //startup code moved to onCreateAnimator

        } else {
            setDetailViewValues();
            configureDetailExtrasAdapter();

        }

        //basic values as opposed to values retrieved by getting details
        setBasicViewValues();
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
                    getMediaDetails();
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
        getArguments().putParcelable(MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mMediaData));

        outState.putString(ApiConstants.RELEASE_STATUS_KEY, releaseStatusTextView.getText().toString());
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


    private void defineViews() {
        //if mIsTablet landscape (2-pane), remove up navigation button from details layout
        ImageButton upNavigationButton = getView().findViewById(R.id.up_navigation_button);
        if (((MasterActivity) getActivity()).isTabletLandscape()) {
            upNavigationButton.setVisibility(View.GONE);
        }

        fab = getView().findViewById(R.id.add_to_list_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().findViewById(R.id.popup_container).setVisibility(View.VISIBLE);

                Parcelable mMediaDataParcelable = Parcels.wrap(mMediaData);

                AddToListFragment addToListFragment =
                        AddToListFragment.newInstance(mMediaType, mMediaDataParcelable);

                getActivity().getSupportFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(R.id.popup_container, addToListFragment, AddToListFragment.FRAGMENT_KEY)
                        .commit();
            }
        });

        //configure share button
        shareButton = getView().findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareCompat.IntentBuilder
                        .from(getActivity())
                        .setType("text/plain")
                        .setChooserTitle(getString(R.string.detail_share_title))
                        .setText(mMediaData.getMediaUrl(getContext(), mMediaData.getId()))
                        .startChooser();
            }
        });

        //define views dependent on retrieving details
        detailExtrasTabLayout = getView().findViewById(R.id.detail_extras_tab_layout);
        detailExtrasViewPager = getView().findViewById(R.id.details_extra_view_pager);

        configureDetailExtrasSize();

        releaseStatusTextView = getView().findViewById(R.id.release_status_text);

    }

    private void configureDetailExtrasSize() {
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
    private void observeViewModel() {
        AppDatabase database = AppDatabase.getInstance(getContext());
        DetailsViewModelFactory viewModelFactory =
                new DetailsViewModelFactory(database, mMediaType, mMediaData.getId());

        DetailsViewModel viewModel =
                ViewModelProviders.of(this, viewModelFactory)
                .get(DetailsViewModel.class);


        LiveData<Integer> watchStatus = viewModel.getWatchStatus();
        watchStatus.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer watchStatusValue) {
                //get array of watch status title shorthand
                String[] watchStatusShorthandList =
                        getContext().getResources().getStringArray(R.array.watch_status_shorthand_titles);

                //get shorthand using watch status as index
                String shorthand = watchStatusValue != null
                        ? watchStatusShorthandList[watchStatusValue]
                        : watchStatusShorthandList[0];

                //set shorthand text
                ((TextView) getView().findViewById(R.id.watch_status_shorthand_text))
                        .setText(shorthand);
            }
        });

        LiveData<List<String>> containingUserLists = viewModel.getContainingLists();
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

    private void getMediaDetails() {
        String id = mMediaData.getId();

        //build AN request
        ANRequest request = NetworkFunctions.agnosticDetailRequestById(mDetailUrl, id,  mSearchPreferences, getContext());

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String returnedJSONString) {
                //parse and populate retrieved data
                if (mMediaType == MEDIA_TYPE_MOVIE) {
                    mMediaData =
                            MovieDataParser.parseDetails(returnedJSONString, ((MovieData) mMediaData));

                } else {
                    mMediaData =
                        SeriesDataParser.parseDetails(returnedJSONString, ((SeriesData) mMediaData));

                }

                //todo implement get details for people data

                setDetailViewValues();

                configureDetailExtrasAdapter();
            }

            @Override
            public void onError(ANError anError) {
                //TODO implement different error messages based upon received error codes
                //prepareNotification error
                Snackbar.make(getActivity().findViewById(R.id.top_layout),
                        getString(R.string.details_error_message), Snackbar.LENGTH_LONG).show();

            }
        });
    }

    private void configureDetailExtrasAdapter() {
        //remove old tabs
        detailExtrasTabLayout.removeAllTabs();

        //add new tabs
        String[] titleList = null;

        FragmentStatePagerAdapter extrasAdapter = null;

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            titleList = getContext().getResources().getStringArray(R.array.detail_movie_extras_titles);
            extrasAdapter = new DetailMovieExtrasAdapter(getFragmentManager(),
                    FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, ((MovieData) mMediaData));

        } else if (mMediaType == MEDIA_TYPE_SERIES){
            titleList = getContext().getResources().getStringArray(R.array.detail_tv_extras_titles);
            extrasAdapter = new DetailSeriesExtrasAdapter(getFragmentManager(),
                    FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, ((SeriesData) mMediaData));
        }

        for (String title : titleList) {
            detailExtrasTabLayout.addTab(detailExtrasTabLayout.newTab().setText(title));
        }

        detailExtrasViewPager.setAdapter(extrasAdapter);

        //clear old listeners to avoid conflicts
        detailExtrasViewPager.clearOnPageChangeListeners();

        //configure new listeners
        detailExtrasViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(detailExtrasTabLayout));
        detailExtrasTabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                detailExtrasViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void setBasicViewValues() {
        NetworkFunctions.loadImage(getContext(), mMediaData.getBackdropPath(),
                ((ImageView) getView().findViewById(R.id.backdrop_image_view)));

        ((TextView) getView().findViewById(R.id.title_text_view)).setText(mMediaData.getTitle());

        ((TextView) getView().findViewById(R.id.date_text_view)).setText(mMediaData.getReleaseDate());

        //TODO implement maturity rating
        ((TextView) getView().findViewById(R.id.maturity_rating_text_view))
                .setText(getString(R.string.maturity_rating_placeholder));

        ((TextView) getView().findViewById(R.id.rating_text_view)).setText(mMediaData.getVoteAverage());

        ((TextView) getView().findViewById(R.id.overView_text_view)).setText(mMediaData.getOverview());
    }

    private void setDetailViewValues() {
        releaseStatusTextView.setText(mMediaData.getReleaseStatus());

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

    @Override
    public void onDetach() {
        super.onDetach();

        Log.d(FRAGMENT_KEY, "detaching fragment");
    }
}
