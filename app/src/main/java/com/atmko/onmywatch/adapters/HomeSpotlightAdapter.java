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

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.utils.network_utils.NetworkFunctions;

import java.util.ArrayList;
import java.util.List;

/*
 * data adapter for media data objects in HomeSpotlightFragment within HomeFragment
 */

public class HomeSpotlightAdapter extends RecyclerView.Adapter<HomeSpotlightAdapter.HomeSpotlightAdapterViewHolder> {
    private final Fragment mFragment;
    private final List<MediaData> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private Context mContext;

    //layout ids
    @SuppressWarnings("FieldCanBeLocal")
    private final int STANDARD_LAYOUT_ID = 1;
    private final int NO_POSTER_LAYOUT = 2;

    public HomeSpotlightAdapter(OnListItemClickListener clickListener, Context context) {
        mFragment = ((Fragment) clickListener);
        mOnListItemClickListener = clickListener;
        mAdapterData = new ArrayList<>();
        mContext = context;
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
    }

    public class HomeSpotlightAdapterViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        FrameLayout topFrameLayout;
        ImageView moviePosterImageView;
        TextView posterReplacementTextView;
        ImageButton addButton;

        private HomeSpotlightAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            topFrameLayout = itemView.findViewById(R.id.top_frame_layout);

            itemView.setOnClickListener(this);

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

    private ViewGroup.MarginLayoutParams getPosterMargins(View view) {
        ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) view.getLayoutParams();

        int searchImageSpacing = (int) mFragment.getResources().getDimension(R.dimen.search_image_spacing);

        margins.setMargins(searchImageSpacing/2, 0, 0, searchImageSpacing);
        return margins;
    }

    @NonNull
    @Override
    public HomeSpotlightAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);

        int resourceId;

        if (viewType == NO_POSTER_LAYOUT) {
            resourceId = R.layout.object_spotlight_no_poster_layout;

        } else {
            resourceId = R.layout.object_spotlight;
        }

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new HomeSpotlightAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeSpotlightAdapterViewHolder adapterViewHolder, int position) {
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
                    currentMediaData.getSpotlightPosterPath(),
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
