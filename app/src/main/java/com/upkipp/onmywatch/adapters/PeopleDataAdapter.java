/*
 * Copyright (C) 2019 Aayat Mimiko
 */
package com.upkipp.onmywatch.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.models.PersonData;
import com.upkipp.onmywatch.utils.network_utils.ApiConstants;
import com.upkipp.onmywatch.utils.network_utils.NetworkFunctions;

import java.util.ArrayList;
import java.util.List;

public class PeopleDataAdapter extends RecyclerView.Adapter<PeopleDataAdapter.VisualMediaAdapterViewHolder> {

    private final Fragment mFragment;
    private final List<PersonData> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;

    //layout ids
    @SuppressWarnings("FieldCanBeLocal")
    private final int STANDARD_LAYOUT_ID = 1;
    private final int NO_POSTER_LAYOUT = 2;

    public PeopleDataAdapter(OnListItemClickListener clickListener) {
        mFragment = ((Fragment) clickListener);
        mOnListItemClickListener = clickListener;
        mAdapterData = new ArrayList<>();
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
    }

   public class VisualMediaAdapterViewHolder extends RecyclerView.ViewHolder
             implements View.OnClickListener{

        FrameLayout topFrameLayout;
        ImageView peoplePosterImageView;
        TextView posterReplacementTextView;

        private VisualMediaAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            if (viewType == NO_POSTER_LAYOUT) {
                posterReplacementTextView = itemView.findViewById(R.id.poster_replacement_text_view);
            } else {
                peoplePosterImageView = itemView.findViewById(R.id.posterImageView);
            }

            topFrameLayout = itemView.findViewById(R.id.top_frame_layout);
            topFrameLayout.setLayoutParams(getPosterDimensions(topFrameLayout));
            topFrameLayout.setLayoutParams(getPosterMargins(topFrameLayout));

            itemView.setOnClickListener(this);

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

        //get search fragment layout weight
        int searchFragmentLayoutWeight = mFragment.getResources()
                .getInteger(R.integer.master_fragment_layout_weight);

        //get detail fragment layout weight
        int detailFragmentLayoutWeight = mFragment.getResources()
                .getInteger(R.integer.detail_fragment_layout_weight);

        //get weight total
        int weightTotal = searchFragmentLayoutWeight + detailFragmentLayoutWeight;

        //get search fragment pixel width
        int searchFragmentPixelWidth =
                displayDimensions.widthPixels * searchFragmentLayoutWeight/weightTotal;

        //get single image pixel width: (searchFragmentPixelWidth/num of columns)
        int singleImgPixelWidth =
                searchFragmentPixelWidth / mFragment.getResources().getInteger(R.integer.search_column_span);

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
    public PeopleDataAdapter.VisualMediaAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        int resourceId;

        if (viewType == NO_POSTER_LAYOUT) {
            resourceId = R.layout.no_poster_layout;

        } else {
            resourceId = R.layout.object_visualmedia;
        }

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new VisualMediaAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull VisualMediaAdapterViewHolder adapterViewHolder, int position) {
        Context context = adapterViewHolder.topFrameLayout.getContext();

        //get current VisualMediaData
        PersonData currentVisualMediaData = mAdapterData.get(position);

        //if item view type is no poster
        if (adapterViewHolder.getItemViewType() == NO_POSTER_LAYOUT) {
            //set title instead of poster image
            adapterViewHolder.posterReplacementTextView.setText(currentVisualMediaData.getName());

        } else {
            //load image with glide
            NetworkFunctions.loadImage(
                    context,
                    currentVisualMediaData.getProfilePath(),
                    adapterViewHolder.peoplePosterImageView);
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
        //if poster path != null
        boolean hasPoster = mAdapterData.get(position).getProfilePath() != null;

        if (hasPoster) {
            return STANDARD_LAYOUT_ID;

        } else {
            return NO_POSTER_LAYOUT;
        }
    }

//    public boolean isEmpty() {
//        return getItemCount() == 0;
//    }

    public List<PersonData> getAdapterData() {
        return mAdapterData;
    }

    public void addAdapterData(List<PersonData> visualMediaDataList) {
        mAdapterData.addAll(visualMediaDataList);
        notifyDataSetChanged();
    }

    public PersonData getVisualMediaData(int index) {
        return mAdapterData.get(index);
    }

    public List<PersonData> getVisualMediaDataList() {
        return mAdapterData;
    }

        //clears and updates adapterData
//    public void clearData() {
//        mAdapterData.clear();
//        notifyDataSetChanged();
//    }
}
