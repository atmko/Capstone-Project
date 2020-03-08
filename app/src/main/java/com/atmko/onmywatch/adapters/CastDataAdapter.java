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
import com.atmko.onmywatch.models.CastData;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;

import java.util.ArrayList;
import java.util.List;

/*
 * data adapter for cast objects in CastFragment within DetailsFragment
 */

public class CastDataAdapter extends RecyclerView.Adapter<CastDataAdapter.CastDataAdapterViewHolder> {
    private final Fragment mFragment;
    private final List<CastData> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private boolean mInPlaceholderMode;
    private final Context mContext;
    private final int[] mParams;

    //layout ids
    @SuppressWarnings("FieldCanBeLocal")
    private final int EMPTY_ADAPTER_ID = -1;
    private final int STANDARD_LAYOUT_ID = 1;
    private final int NO_POSTER_LAYOUT = 2;

    public CastDataAdapter(OnListItemClickListener clickListener, Context context, int[] params) {
        mFragment = ((Fragment) clickListener);
        mOnListItemClickListener = clickListener;
        mAdapterData = new ArrayList<>();
        mContext = context;
        mParams = params;
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
    }

    public class CastDataAdapterViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        final FrameLayout topFrameLayout;
        ImageView castPosterImageView;
        TextView nameTextView;
        TextView roleTextView;
        ImageButton addToListButton;

        private CastDataAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            if (viewType == STANDARD_LAYOUT_ID) {
                castPosterImageView = itemView.findViewById(R.id.posterImageView);

            }

            topFrameLayout = itemView.findViewById(R.id.top_frame_layout);
            topFrameLayout.setLayoutParams(getPosterDimensions(topFrameLayout));
            topFrameLayout.setLayoutParams(getPosterMargins(topFrameLayout));

            nameTextView = itemView.findViewById(R.id.name_text_view);
            roleTextView = itemView.findViewById(R.id.role_text_view);
            addToListButton = itemView.findViewById(R.id.add_to_list_button);

            addToListButton.setVisibility(View.GONE);

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

        //set layout params
        params.width = mParams[0];
        params.height = mParams[1];

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
    public CastDataAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);

        int resourceId;

        if (viewType == NO_POSTER_LAYOUT) {
            resourceId = R.layout.object_cast_no_profile;

        } else {
            resourceId = R.layout.object_cast;
        }

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new CastDataAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull CastDataAdapterViewHolder adapterViewHolder, int position) {
        if (adapterViewHolder.getItemViewType() == EMPTY_ADAPTER_ID) return;

        //get current MediaData
        CastData currentMediaData = mAdapterData.get(position);

        adapterViewHolder.nameTextView.setText(getAdapterData().get(position).getName());
        adapterViewHolder.roleTextView.setText(getAdapterData().get(position).getCharacter());

        //if item view type is no poster
        if (adapterViewHolder.getItemViewType() == STANDARD_LAYOUT_ID) {
            //load image with glide
            NetworkFunctions.loadImage(
                    mContext,
                    currentMediaData.getProfilePath(),
                    adapterViewHolder.castPosterImageView);
        }
    }

    @Override
    public int getItemCount() {
        return mAdapterData.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mInPlaceholderMode) return EMPTY_ADAPTER_ID;

        //if poster path != null
        boolean hasPoster = mAdapterData.get(position).getProfilePath() != null;

        if (hasPoster) {
            return STANDARD_LAYOUT_ID;

        } else {
            return NO_POSTER_LAYOUT;
        }
    }

    public List<CastData> getAdapterData() {
        return mAdapterData;
    }

    public void addAdapterData(List<CastData> mediaDataList) {
        mAdapterData.addAll(mediaDataList);
        notifyDataSetChanged();
    }

    public void setInPlaceholderMode(boolean inPlaceholderMode) {
        mInPlaceholderMode = inPlaceholderMode;

        if (mInPlaceholderMode) {
            mAdapterData.clear();
            notifyDataSetChanged();
        }
    }
}
