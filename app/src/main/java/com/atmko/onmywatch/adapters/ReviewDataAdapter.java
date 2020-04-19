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
    public static final int REVIEW_CUT_OFF_INDEX = 100;

    private final ArrayList<Review> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private boolean mInPlaceholderMode;

    //layout ids
    private final int EMPTY_ADAPTER_ID = -1;
    @SuppressWarnings("FieldCanBeLocal")
    private final int STANDARD_LAYOUT_ID = 1;

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
        if (adapterViewHolder.getItemViewType() == EMPTY_ADAPTER_ID) return;

        //get current reviewData
        Review currentReviewData = mAdapterData.get(position);
        String author = currentReviewData.getAuthor();
        String content = currentReviewData.getContent();

        adapterViewHolder.authorTextVIew.setText(author);
        adapterViewHolder.contentTextView.setTag(content);
        adapterViewHolder.contentTextView
                .setText(limitText(content));

    }

    @Override
    public int getItemCount() {
        return mAdapterData.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mInPlaceholderMode) return EMPTY_ADAPTER_ID;

        return STANDARD_LAYOUT_ID;
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public ArrayList<Review> getAdapterData() {
        return mAdapterData;
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
    private String limitText(String fullText) {
        if (fullText.length() > REVIEW_CUT_OFF_INDEX) {
            return fullText.substring(0, REVIEW_CUT_OFF_INDEX) + "...";

        } else {
            return fullText;
        }
    }

    public void setInPlaceholderMode(boolean inPlaceholderMode) {
        mInPlaceholderMode = inPlaceholderMode;

        if (mInPlaceholderMode) {
            mAdapterData.clear();
            notifyDataSetChanged();
        }
    }
}

