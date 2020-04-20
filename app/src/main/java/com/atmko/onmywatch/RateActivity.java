/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.view_models.RateViewModel;
import com.atmko.onmywatch.view_models.RateViewModelFactory;

import org.parceler.Parcels;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;

public class RateActivity extends AppCompatActivity {
    public static final String MEDIA_TYPE_KEY = "media_type";
    public static final String MEDIA_ID_KEY = "media_id";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate);

        Intent intent = getIntent();
        mMediaType = intent.getIntExtra(MEDIA_TYPE_KEY, 0);
        mMediaId = Parcels.unwrap(intent.getParcelableExtra(MEDIA_ID_KEY));

        //save saveInstanceState value for onCreateAnimator and mNewContainingLists to check if
        // this is the first instance

        mSavedInstanceState = savedInstanceState;

        defineViews();

        //set up and observe view model
        observeData();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(USER_RATING_KEY, mRatingSeekBar.getProgress());
    }

    private void defineViews() {
        mRatingText = findViewById(R.id.rate_text_view);
        mRatingSeekBar = findViewById(R.id.rating_seek_bar);
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
        Button mSaveButton = findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        saveUserRating();

                    }
                });

                //exit activity
                finish();
            }
        });
    }

    private void observeData() {
        mDatabase = AppDatabase.getInstance(this);

        RateViewModelFactory viewModelFactory =
                new RateViewModelFactory(mDatabase, mMediaType, mMediaId);
        RateViewModel viewModel =
                ViewModelProviders.of(this,
                        viewModelFactory).get(RateViewModel.class);
        final LiveData<MediaData> mediaDataLiveData = viewModel.getMediaData();

        mediaDataLiveData.observe(this, new Observer<MediaData>() {
            @Override
            public void onChanged(MediaData mediaData) {
                //if media exists
                //media data is a/ways not null because rating only possible...
                //with saved data(watching, watched or dropped)
                if (mediaData != null) {
                    databaseMediaData = mediaData;

                }

                //restore seekBar progress
                if (mSavedInstanceState != null) {
                    mRatingSeekBar.setProgress(mSavedInstanceState.getInt(USER_RATING_KEY));
                    return;

                }

                //setup original seekBar value in first initialization(mSavedInstanceState == null)
                if (mediaData != null) {
                    int userRating = mediaData.getUserRating();
                    if (userRating == 0) {
                        mRatingSeekBar.setProgress(MAX_RATING / 2);

                    } else {
                        mRatingSeekBar.setProgress(userRating);

                    }
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
}
