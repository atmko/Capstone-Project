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

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.PersonData;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;

import java.util.ArrayList;
import java.util.List;

/*
 * data adapter for people data objects
 */

public class PeopleDataAdapter
        extends RecyclerView.Adapter<PeopleDataAdapter.PeopleDataAdapterViewHolder> {

    private final Fragment mFragment;
    private final List<PersonData> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private final Context mContext;

    //layout ids
    private final int STANDARD_LAYOUT_ID = 1;
    private final int NO_POSTER_LAYOUT = 2;

    public PeopleDataAdapter(OnListItemClickListener clickListener, Context context) {
        mFragment = ((Fragment) clickListener);
        mOnListItemClickListener = clickListener;
        mAdapterData = new ArrayList<>();
        mContext = context;
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
    }

    public class PeopleDataAdapterViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        final FrameLayout topFrameLayout;
        ImageView peoplePosterImageView;
        final TextView nameTextView;
        final TextView roleTextView;
        final ImageButton addToListButton;

        private PeopleDataAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            if (viewType == STANDARD_LAYOUT_ID) {
                peoplePosterImageView = itemView.findViewById(R.id.posterImageView);

            }

            topFrameLayout = itemView.findViewById(R.id.top_frame_layout);
            topFrameLayout.setLayoutParams(getPosterDimensions(topFrameLayout));
            topFrameLayout.setLayoutParams(getPosterMargins(topFrameLayout));

            nameTextView = itemView.findViewById(R.id.name_text_view);
            roleTextView = itemView.findViewById(R.id.role_text_view);
            addToListButton = itemView.findViewById(R.id.add_to_list_button);

            roleTextView.setVisibility(View.GONE);
            addToListButton.setVisibility(View.GONE);

            DisplayMetrics displayDimensions = Resources.getSystem().getDisplayMetrics();

            //set layout margins to nameTextView bottom
            float dimenToPixels =
                    displayDimensions.density * mContext.getResources()
                            .getInteger(R.integer.x1_standard_layout_margin_unscaled);
            ViewGroup.MarginLayoutParams margins =
                    (ViewGroup.MarginLayoutParams) nameTextView.getLayoutParams();

            margins.setMargins(0, 0, 0, (int) dimenToPixels);

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
        long posterHeight = Math.round(adjustedViewWidth * ApiConstants.POSTER_ASPECT_RATIO);

        //set layout params
        params.width = adjustedViewWidth;
        params.height = (int) posterHeight;

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
    public PeopleDataAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                          int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);

        int resourceId;

        if (viewType == NO_POSTER_LAYOUT) {
            resourceId = R.layout.object_cast_no_profile;

        } else {
            resourceId = R.layout.object_cast;
        }

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new PeopleDataAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull PeopleDataAdapterViewHolder adapterViewHolder, int position) {
        //get current MediaData
        PersonData currentMediaData = mAdapterData.get(position);

        adapterViewHolder.nameTextView.setText(getAdapterData().get(position).getName());

        //if item view type is no poster
        if (adapterViewHolder.getItemViewType() == STANDARD_LAYOUT_ID) {
            //load image with glide
            NetworkFunctions.loadImage(
                    mContext,
                    currentMediaData.getProfilePath(),
                    adapterViewHolder.peoplePosterImageView);
        }
    }

    @Override
    public int getItemCount() {
        return mAdapterData.size();
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

    public List<PersonData> getAdapterData() {
        return mAdapterData;
    }

    @SuppressWarnings("unchecked")
    public void addAdapterData(List mediaDataList) {
        mAdapterData.addAll(mediaDataList);
        notifyDataSetChanged();
    }
}
