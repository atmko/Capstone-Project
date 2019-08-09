package com.upkipp.onmywatch.Fragments;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.adapters.DetailMovieExtrasAdapter;
import com.upkipp.onmywatch.adapters.DetailTvExtrasAdapter;
import com.upkipp.onmywatch.database.AppDatabase;
import com.upkipp.onmywatch.models.MediaData;
import com.upkipp.onmywatch.models.MovieData;
import com.upkipp.onmywatch.models.SeriesData;
import com.upkipp.onmywatch.utils.MovieDataParser;
import com.upkipp.onmywatch.utils.SearchPreferences;
import com.upkipp.onmywatch.utils.SeriesDataParser;
import com.upkipp.onmywatch.utils.network_utils.NetworkFunctions;

import org.parceler.Parcels;

import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_PEOPLE;
import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

//import com.upkipp.onmywatch.HomeActivity;

public class DetailsFragmentTemp extends Fragment {
    public static String FRAGMENT_KEY = "details_fragment";

    //TODO consider putting mediatype as an attribute in MediaData Objects
    public static String MEDIA_TYPE_KEY = "media_type";
    public static String DETAIL_URL_KEY = "detail_url";
    public static String MEDIA_DATA_PARCELABLE_KEY = "media_data";
    public static String SEARCH_PREFERENCES_KEY = "search_preferences";

    //arbitrary default value to prevent watchStatus and in list count from accidentally triggering..
    //media data deletion from database
    private static final int ERROR_SAFEGUARD = 27;

    private int mMediaType;
    private String mDetailUrl;
    private MediaData mMediaData;
    private SearchPreferences mSearchPreferences;

    private AppDatabase mDatabase;
    private int mWatchStatus;
    private int mInListCount;

    private TabLayout detailExtrasTabLayout;
    private ViewPager detailExtrasViewPager;

    private FloatingActionButton fab;

    public DetailsFragmentTemp() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //update initialized media data
        getArguments().putParcelable(MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mMediaData));
    }

    public static DetailsFragmentTemp newInstance(int mediaType, String detailUrl, Parcelable mediaDataParcel,
                                                  Parcelable searchPreferencesParcel) {
        DetailsFragmentTemp fragment = new DetailsFragmentTemp();
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

        configureDetailExtrasViewHeight();

        defineViews();

//        observeData(savedInstanceState);

        if (savedInstanceState == null) {
            getMediaDetails();

        } else {
            configureDetailExtrasAdapter();

        }

        setViewValues();
    }

    private int configureDetailExtrasViewHeight() {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int pixelHeight = displayMetrics.heightPixels;

        return pixelHeight;
    }

    private void observeData(final Bundle savedInstanceState){
//        mDatabase = AppDatabase.getInstance(getContext());
//
//        DetailsViewModelFactory detailsViewModelFactory =
//                new DetailsViewModelFactory(mDatabase, mMediaType, mMediaData.getId());
//
//        final DetailsViewModel viewModel = ViewModelProviders.of(getActivity(), detailsViewModelFactory)
//                .get(DetailsViewModel.class);
//
//        viewModel.getWatchStatus().observe(getActivity(), new Observer<Integer>() {
//            @Override
//            public void onChanged(final Integer watchStatus) {
//                if (watchStatus != null) {
//                    mMediaData.setWatchStatus(watchStatus);
//                    Log.d(FRAGMENT_KEY, "1.setting "+mWatchStatus+" watch status");
//
//                } else {
//                    mMediaData.setWatchStatus(MediaData.WATCH_STATUS_NONE);
//                    Log.d(FRAGMENT_KEY, "2.setting "+mWatchStatus+" watch status");
//
//                }
//
//                //showWatchStatus(watchStatus);
//            }
//        });
//
//        viewModel.getContainingLists().observe(getActivity(), new Observer<List<String>>() {
//            @Override
//            public void onChanged(List<String> listNames) {
//                if (listNames != null) {
//                    mInListCount = listNames.size();
//
//                } else {
//                    mInListCount = 0;
//
//                }
//
//                //showInListsCount(listCount);
//
//                Log.d(FRAGMENT_KEY, "mInListCount: " + mInListCount);
//
//                //-------------------------------------------------
//            }
//        });
    }

    private void defineViews() {
        detailExtrasTabLayout = getView().findViewById(R.id.detail_extras_tab_layout);
        detailExtrasViewPager = getView().findViewById(R.id.details_extra_view_pager);








//        950   1344

        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int density = displayMetrics.densityDpi;


//        int pixel = (x * density) / 160;



        Rect rectangle = new Rect();
        Window window = getActivity().getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int pixelStatusBarHeight = rectangle.top;

        Toast.makeText(getContext(), ""+density, Toast.LENGTH_SHORT).show();





        final ConstraintLayout includeDetailsExtras = getView().findViewById(R.id.include_details_main);
//

//
//        LinearLayout.LayoutParams detailExtrasParams =
//                new LinearLayout.LayoutParams(includeDetailsExtras.getWidth(), configureDetailExtrasViewHeight() + height);
//
//        includeDetailsExtras.setLayoutParams(detailExtrasParams);











        fab = getView().findViewById(R.id.add_to_list_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().findViewById(R.id.popup_container).setVisibility(View.VISIBLE);

                Parcelable mMediaDataParcelable = Parcels.wrap(mMediaData);

                AddToListFragment addToListFragment =
                        AddToListFragment.newInstance(mMediaType, mMediaDataParcelable);

                getActivity().getSupportFragmentManager().beginTransaction()
//                        .addToBackStack(FRAGMENT_KEY)
                        .add(R.id.popup_container, addToListFragment, AddToListFragment.FRAGMENT_KEY)
                        .commit();

                Toast.makeText(getContext(), "yip", Toast.LENGTH_SHORT).show();

//                Intent intent = new Intent(getActivity().getApplicationContext(), com.upkipp.onmywatch.AddToListFragment.class);
//
//                intent.putExtra(com.upkipp.onmywatch.AddToListFragment.MEDIA_TYPE_KEY, mMediaType);
//                intent.putExtra(com.upkipp.onmywatch.AddToListFragment.MEDIA_DATA_KEY, mMediaDataParcelable);
//
//                startActivity(intent);
            }
        });
    }

    private void getMediaDetails() {
        String id = mMediaData.getId();

        //build AN request
        ANRequest request = NetworkFunctions.agnosticDetailRequestById(mDetailUrl, id,  mSearchPreferences, getContext());

        Log.d("............YYY",Boolean.toString(mMediaType == MEDIA_TYPE_SERIES));
        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String returnedJSONString) {
                    //parse and populate retrieved data

                if (mMediaType == MEDIA_TYPE_MOVIE) {
                    mMediaData =
                            MovieDataParser.parseDetails(returnedJSONString, ((MovieData) mMediaData));

                } else if (mMediaType == MEDIA_TYPE_SERIES) {
                    mMediaData =
                        SeriesDataParser.parseDetails(returnedJSONString, ((SeriesData) mMediaData));

                } else if (mMediaType == MEDIA_TYPE_PEOPLE) {
//                    mMediaData =
//                            PersonDataParser.parseDetails(returnedJSONString, ((PersonData) mMediaData));

                }

                configureDetailExtrasAdapter();

//                    //if two pane
//                    //and if this is first time loading fragment i.e if SearchFragment's saved instance state == null)
//                    if (mIsTwoPane && isFirstInit) {
//                        loadDetailFragment();
//                    }


            }

            @Override
            public void onError(ANError anError) {
                //notify error
//                Snackbar.make(mRootView.findViewById(R.id.topLayout),
//                        anError.getErrorDetail(), Snackbar.LENGTH_LONG).show();
                Log.d("............MMM",Boolean.toString(mMediaType == MEDIA_TYPE_SERIES));

                Toast.makeText(getContext(), String.valueOf(anError.getErrorCode()), Toast.LENGTH_SHORT).show();

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
            extrasAdapter = new DetailTvExtrasAdapter(getFragmentManager(),
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

    private void setViewValues() {
        NetworkFunctions.loadImage(getContext(), mMediaData.getBackdropPath(),
                ((ImageView) getView().findViewById(R.id.backdrop_image_view)));

        ((TextView) getView().findViewById(R.id.title_text_view)).setText(mMediaData.getTitle());

        ((TextView) getView().findViewById(R.id.date_text_view)).setText(mMediaData.getReleaseDate());

        //TODO
//        getView().findViewById(R.id.maturity_rating_text_view);

        if (mMediaData.getGenreByIndex(0) != null){
            ((TextView) getView().findViewById(R.id.genre_1_text_view))
                    .setText(mMediaData.getGenreByIndex(0));

        } else  {
            getView().findViewById(R.id.genre_1_text_view).setVisibility(View.GONE);
        }

        if (mMediaData.getGenreByIndex(1) != null){
            ((TextView) getView().findViewById(R.id.genre_2_text_view))
                    .setText(mMediaData.getGenreByIndex(1));
        } else  {
            getView().findViewById(R.id.genre_1_text_view).setVisibility(View.GONE);

        }

        ((TextView) getView().findViewById(R.id.rating_text_view)).setText(mMediaData.getVoteAverage());

        ((TextView) getView().findViewById(R.id.overView_text_view)).setText(mMediaData.getOverview());
    }
}
