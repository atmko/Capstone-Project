/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.Fragments.DetailsFragment;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.Review;

import java.util.ArrayList;

/*
 * data adapter for review objects ReviewsFragment
 */

public final class ReviewDataAdapter
        extends RecyclerView.Adapter<ReviewDataAdapter.ReviewDataAdapterViewHolder> {

    private final ArrayList<Review> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;

    public ReviewDataAdapter(OnListItemClickListener clickListener) {
        mOnListItemClickListener = clickListener;
        mAdapterData = new ArrayList<>();
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
    }

    public class ReviewDataAdapterViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        final TextView authorTextVIew;
        final TextView contentTextView;

        private ReviewDataAdapterViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextVIew = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);

            itemView.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            mOnListItemClickListener.onItemClick(position);
        }
    }

    @NonNull
    @Override
    public ReviewDataAdapter.ReviewDataAdapterViewHolder onCreateViewHolder(
            @NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        int resourceId = R.layout.object_review;

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new ReviewDataAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewDataAdapterViewHolder adapterViewHolder,
                                 int position) {
        //get current reviewData
        Review currentReviewData = mAdapterData.get(position);
        String author = currentReviewData.getAuthor();
        String content = currentReviewData.getContent();

        adapterViewHolder.authorTextVIew.setText(author);
        adapterViewHolder.contentTextView.setTag(content);
        adapterViewHolder.contentTextView
                .setText(limitText(content, DetailsFragment.REVIEW_CUT_OFF_INDEX));

    }

    @Override
    public int getItemCount() {
        return mAdapterData.size();
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public void addAdapterData(ArrayList<Review> reviews) {
        mAdapterData.addAll(reviews);
        notifyDataSetChanged();
    }

    public Review getReviewData(int index) {
        return mAdapterData.get(index);
    }

    //clears and updates adapterData
    public void clearData() {
        mAdapterData.clear();
        notifyDataSetChanged();
    }

    //truncates long text (reviews)
    private String limitText(String fullText, int cutOffIndex) {
        if (fullText.length() > cutOffIndex) {
            return fullText.substring(0, cutOffIndex) + "...";

        } else {
            return fullText;
        }
    }
}

