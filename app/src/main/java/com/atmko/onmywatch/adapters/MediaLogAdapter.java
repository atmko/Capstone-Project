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
import com.atmko.onmywatch.models.SeriesLog;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;

import java.util.ArrayList;
import java.util.List;

import static com.atmko.onmywatch.models.SeriesLog.TYPE_SEASON;

/*
 * data adapter for Media data objects
 */

public class MediaLogAdapter
        extends RecyclerView.Adapter<MediaLogAdapter.MediaLogAdapterViewHolder> {

    private final Fragment mFragment;
    private final List<SeriesLog> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private boolean mInPlaceholderMode;
    private int mPlaceHolderCapacity;
    private int mPlaceHolderCount;
    private final Context mContext;
    private final int[] mParams;

    //layout ids
    private final int STANDARD_LAYOUT_ID = 1;
    private final int NO_POSTER_LAYOUT = 2;
    private final int PLACEHOLDER_ID = -1;

    private final String SEASON_SHORTHAND = "S";
    private final String EPISODE_SHORTHAND = "E";


    public MediaLogAdapter(OnListItemClickListener clickListener, Context context, int[] params) {
        mFragment = ((Fragment) clickListener);
        mOnListItemClickListener = clickListener;
        mAdapterData = new ArrayList<>();
        mInPlaceholderMode = false;
        mPlaceHolderCapacity = 1;
        mContext = context;
        mParams = params;
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
        void onAddButtonClick(int position);
    }

    public boolean inPlaceholderMode() {
        return mInPlaceholderMode;
    }

    public int getPlaceHolderCapacity() {
        return mPlaceHolderCapacity;
    }

    public void setPlaceHolderCapacity(int placeholderCount) {
        this.mPlaceHolderCapacity = placeholderCount;
    }

    public void setInPlaceholderMode(boolean inPlaceholderMode) {
        mInPlaceholderMode = inPlaceholderMode;

        if (mInPlaceholderMode) {
            mPlaceHolderCount = mPlaceHolderCapacity - mAdapterData.size();
            for (int i = 0; i < mPlaceHolderCount; i++) {
                mAdapterData.add(null);
            }

            notifyDataSetChanged();
        }
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

        private MediaLogAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            topFrameLayout = itemView.findViewById(R.id.top_frame_layout);
            topFrameLayout.setLayoutParams(getPosterDimensions(topFrameLayout));
            topFrameLayout.setLayoutParams(getPosterMargins(topFrameLayout));

            itemView.setOnClickListener(this);

            if (viewType == PLACEHOLDER_ID) return;

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
            resourceId = R.layout.no_poster_layout;

        } else if (viewType == PLACEHOLDER_ID) {
            resourceId = R.layout.item_empty_list;

        } else {
            resourceId = R.layout.object_media_data;

        }

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new MediaLogAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaLogAdapterViewHolder adapterViewHolder, int position) {
        if (adapterViewHolder.getItemViewType() == PLACEHOLDER_ID) return;

        //get current SeriesLog
        SeriesLog currentMediaLog = mAdapterData.get(position);

        //if item view type is no poster
        if (adapterViewHolder.getItemViewType() == NO_POSTER_LAYOUT) {
            //set title instead of poster image
            adapterViewHolder.posterReplacementTextView.setText(currentMediaLog.title);

        } else {
            String string;

            if (currentMediaLog.type.equals(TYPE_SEASON)) {
                string = TYPE_SEASON + " " + currentMediaLog.seasonNumber;

            } else {
                string = SEASON_SHORTHAND +
                        currentMediaLog.seasonNumber +
                        EPISODE_SHORTHAND +
                        currentMediaLog.episodeNumber;
            }

            adapterViewHolder.typeTextView.setText(string);

            //load image with glide
            NetworkFunctions.loadImage(
                    mContext,
                    currentMediaLog.posterPath,
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
        if (mInPlaceholderMode && (position >= getPlaceholdersStartingIndex())) return PLACEHOLDER_ID;

        //if poster path != null
        boolean hasPoster = mAdapterData.get(position).posterPath != null;

        if (hasPoster) {
            return STANDARD_LAYOUT_ID;

        } else {
            return NO_POSTER_LAYOUT;
        }
    }

    public List<SeriesLog> getAdapterData() {
        return mAdapterData;
    }

    public void addAdapterData(List mediaLogList) {
        mAdapterData.addAll(mediaLogList);
        notifyDataSetChanged();
    }
}
