package com.atmko.onmywatch;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.atmko.onmywatch.models.Review;

import org.parceler.Parcels;

public class ReviewActivity extends AppCompatActivity {
    public static final String REVIEW_KEY = "review";
    private Review mReview;
    private TextView authorTextView;
    private TextView contentTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        defineViews();
        setValues(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(REVIEW_KEY, Parcels.wrap(mReview));
    }

    private void defineViews() {
        authorTextView = findViewById(R.id.authorTextView);
        contentTextView = findViewById(R.id.contentTextView);
    }

    private void setValues(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent == null) return;
            if (intent.hasExtra(REVIEW_KEY)) {
                mReview = Parcels.unwrap(intent.getParcelableExtra(REVIEW_KEY));
            }

        } else {
            if (savedInstanceState.containsKey(REVIEW_KEY)) {
                mReview = savedInstanceState.getParcelable(REVIEW_KEY);
            }
        }

        if (mReview != null) {
            authorTextView.setText(mReview.getAuthor());
            contentTextView.setText(mReview.getContent());
        }
    }
}