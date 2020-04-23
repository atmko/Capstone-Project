/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.MediaLog;
import com.atmko.onmywatch.models.SeriesLog;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;

import java.util.ArrayList;
import java.util.List;

/*
 * data adapter for Media data objects
 */

public class MediaLogAdapter
        extends RecyclerView.Adapter<MediaLogAdapter.MediaLogAdapterViewHolder> {

    private final Fragment mFragment;
    private final List<MediaLog> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private int mPlaceHolderCapacity;
    private int mPlaceHolderCount;
    private final Context mContext;
    private final int[] mParams;

    //layout ids
    private final int STANDARD_LAYOUT_ID = 1;
    private final int NO_POSTER_LAYOUT = 2;
    private final int PLACEHOLDER_ID = -1;

    private static final int UNIQUE_PLACEHOLDER_COUNT = 3;

    public MediaLogAdapter(OnListItemClickListener clickListener, Context context, int[] params) {
        mFragment = ((Fragment) clickListener);
        mOnListItemClickListener = clickListener;
        mAdapterData = new ArrayList<>();
        mPlaceHolderCapacity = 1;
        mContext = context;
        mParams = params;
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
        void onAddButtonClick(int position);
    }

    public boolean inPlaceholderMode(int position) {
        return position >= getPlaceholdersStartingIndex();
    }

    public void setPlaceHolderCapacity(int placeholderCount) {
        this.mPlaceHolderCapacity = placeholderCount;
    }

    public void setPlaceholders() {
        mPlaceHolderCount = mPlaceHolderCapacity - mAdapterData.size();
        for (int i = 0; i < mPlaceHolderCount; i++) {
            mAdapterData.add(null);
        }

        notifyDataSetChanged();
    }

    private int getPlaceholdersStartingIndex() {
        return mAdapterData.size() - mPlaceHolderCount;
    }

    public class MediaLogAdapterViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        final FrameLayout topFrameLayout;
        ImageView moviePosterImageView;
        TextView posterReplacementTextView;
        ImageButton addButton;
        TextView typeTextView;
        final TextView countDownTextView;

        private MediaLogAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            topFrameLayout = itemView.findViewById(R.id.top_frame_layout);
            topFrameLayout.setLayoutParams(getPosterDimensions(topFrameLayout));
            topFrameLayout.setLayoutParams(getPosterMargins(topFrameLayout));

            countDownTextView = itemView.findViewById(R.id.count_down_text);

            itemView.setOnClickListener(this);

            if (viewType == PLACEHOLDER_ID) {
                moviePosterImageView = itemView.findViewById(R.id.posterImageView);
                return;
            }

            if (viewType == NO_POSTER_LAYOUT) {
                posterReplacementTextView = itemView.findViewById(R.id.poster_replacement_text_view);
            } else {
                moviePosterImageView = itemView.findViewById(R.id.posterImageView);
            }

            addButton =
                    itemView.findViewById(R.id.add_to_list_button);
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnListItemClickListener.onAddButtonClick(getAdapterPosition());
                }
            });
            typeTextView = itemView.findViewById(R.id.type_text_view);
        }

        @Override
        public void onClick(View v) {
            mOnListItemClickListener.onItemClick(getAdapterPosition());
        }
    }

    private ViewGroup.LayoutParams getPosterDimensions(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();

        //set layout params
        params.width = mParams[0];
        params.height = mParams[1];

        return params;
    }

    private ViewGroup.MarginLayoutParams getPosterMargins(View view) {
        ViewGroup.MarginLayoutParams margins =
                (ViewGroup.MarginLayoutParams) view.getLayoutParams();

        int searchImageSpacing = (int) mFragment.getResources().getDimension(R.dimen.search_image_spacing);

        margins.setMargins(searchImageSpacing/2, 0, 0, searchImageSpacing);
        return margins;
    }

    @NonNull
    @Override
    public MediaLogAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                         int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);

        int resourceId;

        if (viewType == STANDARD_LAYOUT_ID) {
            resourceId = R.layout.object_media_log;

        } else if (viewType == NO_POSTER_LAYOUT) {
            resourceId = R.layout.object_media_log_no_poster;

        } else if (viewType == PLACEHOLDER_ID) {
            resourceId = R.layout.object_media_log_placeholder;

        } else {
            resourceId = R.layout.object_media_log;
        }

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new MediaLogAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaLogAdapterViewHolder adapterViewHolder, int position) {
        if (adapterViewHolder.getItemViewType() == PLACEHOLDER_ID
                && position >= getPlaceholdersStartingIndex()) {

            int normalizer = position % UNIQUE_PLACEHOLDER_COUNT;
            if (normalizer == 0) {
                adapterViewHolder.moviePosterImageView.setBackground(mContext.getResources().getDrawable(R.drawable.placeholder_img_1));

            } else  if (normalizer == 1) {
                adapterViewHolder.moviePosterImageView.setBackground(mContext.getResources().getDrawable(R.drawable.placeholder_img_2));

            } else if (normalizer == 2) {
                adapterViewHolder.moviePosterImageView.setBackground(mContext.getResources().getDrawable(R.drawable.placeholder_img_3));
            }

            return;
        }

        //get current SeriesLog
        MediaLog currentMediaLog = mAdapterData.get(position);

        //if item view type is no poster
        if (adapterViewHolder.getItemViewType() == NO_POSTER_LAYOUT) {
            //set title instead of poster image
            adapterViewHolder.posterReplacementTextView.setText(currentMediaLog.title);

        } else {
            //load image with glide
            NetworkFunctions.loadImage(
                    mContext,
                    currentMediaLog.posterPath,
                    adapterViewHolder.moviePosterImageView);
        }

        if (currentMediaLog instanceof SeriesLog) {
            adapterViewHolder.typeTextView.setText(((SeriesLog) currentMediaLog).getTypeString());

        } else {
            adapterViewHolder.typeTextView.setVisibility(View.GONE);
        }

        if (currentMediaLog.condition == MediaLog.CONDITION_UNDATED) {
            adapterViewHolder.countDownTextView.setVisibility(View.GONE);

        } else {
            adapterViewHolder.countDownTextView.setText(currentMediaLog.getCountdown());
        }
    }

    @Override
    public int getItemCount() {
        return mAdapterData.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (inPlaceholderMode(position)) return PLACEHOLDER_ID;

        //if poster path != null
        boolean hasPoster = mAdapterData.get(position).posterPath != null;

        if (hasPoster) {
            return STANDARD_LAYOUT_ID;

        } else {
            return NO_POSTER_LAYOUT;
        }
    }

    public List<MediaLog> getAdapterData() {
        return mAdapterData;
    }

    public void addAdapterData(List<MediaLog> mediaLogList) {
        mAdapterData.addAll(mediaLogList);
        notifyDataSetChanged();
    }
}
