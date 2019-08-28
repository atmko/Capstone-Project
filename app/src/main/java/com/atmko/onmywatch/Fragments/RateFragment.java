package com.atmko.onmywatch.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.view_models.RateViewModel;
import com.atmko.onmywatch.view_models.RateViewModelFactory;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;

public class RateFragment extends Fragment {
    public static final String FRAGMENT_KEY = "rate_fragment";

    // fragment initialization parameters
    private static final String MEDIA_TYPE_KEY = "media_type";
    private static final String MEDIA_ID_KEY = "media_id";

    private static final int MAX_RATING = 10;
    private int mMediaType;
    private String mMediaId;

    //post initialization parameters
    private static final String USER_RATING_KEY = "user_rating";

    private Bundle mSavedInstanceState;
    private AppDatabase mDatabase;
    private MediaData databaseMediaData;
    private SeekBar mRatingSeekBar;
    private TextView mRatingText;
    private Button mSaveButton;

    //interfaces
    private AddToListFragment.OnSavePressedActionListener mSaveActionListener;

    public RateFragment() {
        // Required empty public constructor
    }

    public static RateFragment newInstance(int mediaType, String mediaId) {
        RateFragment fragment = new RateFragment();
        Bundle args = new Bundle();
        args.putInt(MEDIA_TYPE_KEY, mediaType);
        args.putString(MEDIA_ID_KEY, mediaId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMediaType = getArguments().getInt(MEDIA_TYPE_KEY);
            mMediaId = getArguments().getString(MEDIA_ID_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rate, container, false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(USER_RATING_KEY, mRatingSeekBar.getProgress());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        defineViews();

        //set up and observe view model
        mDatabase = AppDatabase.getInstance(getContext());
        RateViewModelFactory rateViewModelFactory =
                new RateViewModelFactory(mDatabase, mMediaType, mMediaId);

        final RateViewModel viewModel =
                ViewModelProviders.of(this, rateViewModelFactory)
                .get(RateViewModel.class);

        observeData(viewModel);
    }

    private void defineViews() {
        mRatingText = getView().findViewById(R.id.rate_text_view);
        mRatingSeekBar = getView().findViewById(R.id.rating_seek_bar);
        mRatingSeekBar.setMax(MAX_RATING);
        mRatingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //ensure zero value is impossible
                if (progress == 0) {
                    seekBar.setProgress(progress + 1);
                    return;
                }

                //set user rating text
                mRatingText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //configure save button action
        mSaveButton = getView().findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        saveUserRating();

                    }
                });

                //exit fragment
                mSaveActionListener.onSavePressed();
            }
        });
    }

    private void observeData(RateViewModel viewModel) {
        viewModel.getMediaData().observe(this, new Observer() {
            @Override
            public void onChanged(Object mediaData) {
                //if media exists
                //media data is a/ways not null because rating only possible...
                //with saved data(watching, watched or dropped)
                if (mediaData != null) {
                    databaseMediaData = ((MediaData) mediaData);

                }

                //restore seekBar progress
                if (mSavedInstanceState != null) {
                    mRatingSeekBar.setProgress(mSavedInstanceState.getInt(USER_RATING_KEY));
                    return;

                }

                //setup original seekBar value in first initialization(mSavedInstanceState == nulll)
                int userRating = ((MediaData) mediaData).getUserRating();
                if (userRating == 0) {
                    mRatingSeekBar.setProgress(MAX_RATING / 2);

                } else {
                    mRatingSeekBar.setProgress(userRating);

                }
            }
        });
    }

    private void saveUserRating() {
        //apply user rating to media data to be saved to database
        databaseMediaData.setUserRating(mRatingSeekBar.getProgress());

        //update media data with new user rating
        if (mMediaType == MEDIA_TYPE_MOVIE) {
            mDatabase.movieDataDao().updateMovieData(((MovieData) databaseMediaData));

        } else {
            mDatabase.seriesDataDao().updateSeriesData((SeriesData) databaseMediaData);

        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AddToListFragment.OnSavePressedActionListener) {
            mSaveActionListener = (AddToListFragment.OnSavePressedActionListener) context;
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
}
