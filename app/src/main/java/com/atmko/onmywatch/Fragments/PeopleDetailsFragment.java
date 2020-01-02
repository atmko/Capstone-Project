/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.DetailPeopleExtrasAdapter;
import com.atmko.onmywatch.models.PersonData;
import com.atmko.onmywatch.utils.PersonDataParser;
import com.atmko.onmywatch.utils.SearchPreferences;
import com.atmko.onmywatch.utils.UpdateMediaWorker;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.NetworkFunctions;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.parceler.Parcels;

import java.util.ArrayList;

import static com.atmko.onmywatch.MasterActivity.sDetailsHistory;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;

public class PeopleDetailsFragment extends Fragment {
    public static final String FRAGMENT_KEY = "people_details_fragment";

    //TODO consider putting media type as an attribute in MediaData Class
    private static final String PERSON_DATA_PARCELABLE_KEY = "media_data";
    private static final String SEARCH_PREFERENCES_KEY = "search_preferences";

    private static final int COOL_DOWN_REQUEST_TMDB_ID = 0;

    private PersonData mPersonData;
    private SearchPreferences mSearchPreferences;

    private Bundle mSavedInstanceState;

    //details views
    private TabLayout mDetailExtrasTabLayout;
    private ViewPager mDetailExtrasViewPager;

    //values
    private int mOverviewCutoffIndex;

    public PeopleDetailsFragment() {
        // Required empty public constructor
    }

    public static PeopleDetailsFragment newInstance(PersonData personData,
                                                    SearchPreferences searchPreferences) {
        PeopleDetailsFragment fragment = new PeopleDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(PERSON_DATA_PARCELABLE_KEY, Parcels.wrap(personData));
        args.putParcelable(SEARCH_PREFERENCES_KEY, Parcels.wrap(searchPreferences));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPersonData = Parcels.unwrap(getArguments().getParcelable(PERSON_DATA_PARCELABLE_KEY));
            mSearchPreferences = Parcels.unwrap(getArguments().getParcelable(SEARCH_PREFERENCES_KEY));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_people_details, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //save saveInstanceState value for onCreateAnimator to check if this is the first instance
        mSavedInstanceState = savedInstanceState;

        try {
            defineViews();
            defineValues();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }


        if (savedInstanceState == null) {
            //startup code moved to onCreateAnimator

        } else {
            //check if details value exists.
            //If so set detail values and configure extras adapter, otherwise get detail values
            if (mPersonData.getKnownForMovies() != null) {
                try {
                    setDetailViewValues();
                    configureDetailExtrasAdapter();

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

            } else {
                getPersonDetails();
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
                                    .hideBackgroundFragment(PeopleDetailsFragment.this);
                        }

                        getPersonDetails();

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
        getArguments().putParcelable(PERSON_DATA_PARCELABLE_KEY, Parcels.wrap(mPersonData));
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

        //define views dependent on retrieving details
        mDetailExtrasTabLayout = getView().findViewById(R.id.detail_extras_tab_layout);
        mDetailExtrasViewPager = getView().findViewById(R.id.details_extra_view_pager);

        try {
            //configure show more overview button
            getView().findViewById(R.id.show_more_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((TextView) getView().findViewById(R.id.overView_text_view))
                            .setText(limitText(mPersonData.getOverview(getContext()), mOverviewCutoffIndex));
                }
            });

            configureDetailExtrasSize();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
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

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void getPersonDetails() throws NullPointerException {
        String id = mPersonData.getId();

        String detailUrl = getString(R.string.person_details_url);

        //build AN request
        ANRequest request = NetworkFunctions.agnosticDetailRequestById(detailUrl, id,
                mSearchPreferences, getContext());

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String returnedJSONString) {
                try {
                    //parse and populate retrieved data
                    mPersonData =
                                PersonDataParser.parseDetails(returnedJSONString, mPersonData);

                    setDetailViewValues();

                    configureDetailExtrasAdapter();

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() == ApiConstants.TOO_MANY_REQUESTS) {
                    retryAfterCoolDOwn(anError, COOL_DOWN_REQUEST_TMDB_ID);

                    return;
                }

                //notify user of error
                Snackbar.make(getActivity().findViewById(R.id.top_layout),
                        getString(R.string.details_error_message), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void retryAfterCoolDOwn(ANError anError, final int coolDownRequestId) {
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
                    if (coolDownRequestId == COOL_DOWN_REQUEST_TMDB_ID) {
                        getPersonDetails();

                    }

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
        String[] titleList = getContext().getResources().getStringArray(R.array.list_media_types);
        FragmentStatePagerAdapter extrasAdapter = new DetailPeopleExtrasAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, mPersonData);

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
        //set profile image
        NetworkFunctions.loadImage(getActivity().getApplicationContext(), mPersonData.getProfilePath(),
                ((ImageView) getView().findViewById(R.id.profile_image_view)));

        //set nme
        ((TextView) getView().findViewById(R.id.name_text_view))
                .setText(mPersonData.getName());
    }

    @SuppressWarnings("ConstantConditions")
    private void setDetailViewValues() {
        //set profession
        ((TextView) getView().findViewById(R.id.profession_text_view))
                .setText(mPersonData.getProfession(getContext()));

        //set date of birth
        ((TextView) getView().findViewById(R.id.date_of_birth_text_view))
                .setText(mPersonData.getDateOfBirth(getContext()));

        //set overview text
        ((TextView) getView().findViewById(R.id.overView_text_view)).setText(
                limitText(mPersonData.getOverview(getContext()), mOverviewCutoffIndex));
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

    void stackHistory() {
        if (sDetailsHistory == null) {
            sDetailsHistory = new ArrayList<>();
        }

        //TODO: history list is meant to hold media data and people data
        //noinspection unchecked
        sDetailsHistory.add(mPersonData);
    }

    public void popHistory() {
        if (getActivity() != null) {
            ((MasterActivity) getActivity())
                    .launchPeopleDetailsFragment(((PersonData) sDetailsHistory.get(sDetailsHistory.size() - 1)));

            sDetailsHistory.remove(sDetailsHistory.size() - 1);
        }

        if (sDetailsHistory.size() == 0) {
            sDetailsHistory = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        Log.d(FRAGMENT_KEY, "detaching fragment");
    }
}