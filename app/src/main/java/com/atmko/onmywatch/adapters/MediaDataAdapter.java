/*
 * Copyright (C) 2019 Aayat Mimiko
 */
package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.NetworkFunctions;

import java.util.ArrayList;
import java.util.List;

public class MediaDataAdapter extends RecyclerView.Adapter<MediaDataAdapter.MediaDataAdapterViewHolder> {

    private final Fragment mFragment;
    private final List<MediaData> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private boolean mInPlaceholderMode;
    Context mContext;

    //layout ids
    @SuppressWarnings("FieldCanBeLocal")
    private final int STANDARD_LAYOUT_ID = 1;
    private final int NO_POSTER_LAYOUT = 2;
    private final int EMPTY_ADAPTER_ID = 3;

    public MediaDataAdapter(OnListItemClickListener clickListener, Context context) {
        mFragment = ((Fragment) clickListener);
        mOnListItemClickListener = clickListener;
        mAdapterData = new ArrayList<>();
        mInPlaceholderMode = false;
        mContext = context;
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
    }

    public boolean inPlaceholderMode() {
        return mInPlaceholderMode;
    }

    public void setInPlaceholderMode(boolean inPlaceholderMode) {
        mInPlaceholderMode = inPlaceholderMode;

        if (mInPlaceholderMode) {
            mAdapterData.clear();
            mAdapterData.add(null);
            notifyDataSetChanged();
        }
    }

    public class MediaDataAdapterViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        FrameLayout topFrameLayout;
        ImageView moviePosterImageView;
        TextView posterReplacementTextView;
        ImageButton addButton;

        private MediaDataAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            topFrameLayout = itemView.findViewById(R.id.top_frame_layout);
            topFrameLayout.setLayoutParams(getPosterDimensions(topFrameLayout));
            topFrameLayout.setLayoutParams(getPosterMargins(topFrameLayout));

            itemView.setOnClickListener(this);

            if (viewType == EMPTY_ADAPTER_ID) return;

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
                    ((MasterActivity) mFragment.getActivity())
                            .launchAddToListActivity(mAdapterData.get(getAdapterPosition()));

                }
            });
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            mOnListItemClickListener.onItemClick(position);
        }
    }

    private ViewGroup.LayoutParams getPosterDimensions(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();

        DisplayMetrics displayDimensions = Resources.getSystem().getDisplayMetrics();

        int masterRatio;
        int detailRatio;

        int imageColumnSpan;

        //get layout weights
        masterRatio = mFragment.getResources().getInteger(R.integer.master_fragment_layout_weight);
        detailRatio = mFragment.getResources().getInteger(R.integer.detail_fragment_layout_weight);

        imageColumnSpan = mFragment.getResources().getInteger(R.integer.search_column_span);

        //get weight total
        int weightTotal = masterRatio + detailRatio;

        //get search fragment pixel width
        int searchFragmentPixelWidth =
                displayDimensions.widthPixels * masterRatio/weightTotal;

        //get single image pixel width: (searchFragmentPixelWidth/num of columns)
        int singleImgPixelWidth =
                searchFragmentPixelWidth / imageColumnSpan;

        //convert spacing between images to pixels
        int imageSpacing = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                mFragment.getResources().getInteger(R.integer.search_image_spacing),
                mFragment.getResources().getDisplayMetrics());

        //new image width now that spacing is applied
        int adjustedViewWidth = singleImgPixelWidth - imageSpacing;

        //get poster height
        Long posterHeight = Math.round(adjustedViewWidth * ApiConstants.POSTER_ASPECT_RATIO);

        //set layout params
        params.width = adjustedViewWidth;
        params.height = posterHeight.intValue();

        return params;
    }

    private ViewGroup.MarginLayoutParams getPosterMargins(View view) {
        ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) view.getLayoutParams();

        int searchImageSpacing = (int) mFragment.getResources().getDimension(R.dimen.search_image_spacing);

        margins.setMargins(searchImageSpacing/2, 0, 0, searchImageSpacing);
        return margins;
    }

    @NonNull
    @Override
    public MediaDataAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);

        int resourceId;

        if (viewType == STANDARD_LAYOUT_ID) {
            resourceId = R.layout.object_media_data;

        } else if (viewType == NO_POSTER_LAYOUT) {
            resourceId = R.layout.no_poster_layout;

        } else if (viewType == EMPTY_ADAPTER_ID) {
            resourceId = R.layout.item_empty_list;

        } else {
            resourceId = R.layout.object_media_data;

        }

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new MediaDataAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaDataAdapterViewHolder adapterViewHolder, int position) {
        if (adapterViewHolder.getItemViewType() == EMPTY_ADAPTER_ID) return;

        //get current MediaData
        MediaData currentMediaData = mAdapterData.get(position);

        //if item view type is no poster
        if (adapterViewHolder.getItemViewType() == NO_POSTER_LAYOUT) {
            //set title instead of poster image
            adapterViewHolder.posterReplacementTextView.setText(currentMediaData.getTitle());

        } else {
            //load image with glide
            NetworkFunctions.loadImage(
                    mContext,
                    currentMediaData.getPosterPath(),
                    adapterViewHolder.moviePosterImageView);
        }
    }

    @Override
    public int getItemCount() {
        if (mAdapterData == null) {
            return 0;
        } else {
            return mAdapterData.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mInPlaceholderMode) return EMPTY_ADAPTER_ID;

        //if poster path != null
        boolean hasPoster = mAdapterData.get(position).getPosterPath() != null;

        if (hasPoster) {
            return STANDARD_LAYOUT_ID;

        } else {
            return NO_POSTER_LAYOUT;
        }
    }

    public List<MediaData> getAdapterData() {
        return mAdapterData;
    }

    public void addAdapterData(List mediaDataList) {
        mAdapterData.addAll(mediaDataList);
        notifyDataSetChanged();
    }
}
