/*
 * Copyright (C) 2019 Aayat Mimiko
 */
package com.upkipp.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.models.UserListModel;

import java.util.ArrayList;
import java.util.List;

public class AddToListAdapter extends RecyclerView.Adapter<AddToListAdapter.VisualMediaAdapterViewHolder> {

    private final List<UserListModel> mAdapterData;
    private String mMediaId;
    private final OnListItemClickListener mOnListItemClickListener;
    private final OnListCheckListener mOnListCheckListener;

    //layout ids
    @SuppressWarnings("FieldCanBeLocal")
    private final int STANDARD_LAYOUT_ID = 1;
//    private final int NO_POSTER_LAYOUT = 2;

    public AddToListAdapter(OnListItemClickListener clickListener, String mediaId) {
        mOnListItemClickListener = clickListener;
        mOnListCheckListener = ((OnListCheckListener) clickListener);
        mAdapterData = new ArrayList<>();
        mMediaId = mediaId;
    }

    public interface OnListItemClickListener {
        void onItemClick(UserListModel userListModel, AppCompatCheckBox checkBox);
    }

    public interface OnListCheckListener {
        void onCheckDatabaseRecords(AppCompatCheckBox checkBox, String listName);
    }


   public class VisualMediaAdapterViewHolder extends RecyclerView.ViewHolder
             implements View.OnClickListener{

        TextView listNameTextView;
        AppCompatCheckBox checkBox;

        private VisualMediaAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

//            if (viewType == NO_POSTER_LAYOUT) {
                listNameTextView = itemView.findViewById(R.id.list_name_text_view);
                checkBox = itemView.findViewById(R.id.checkbox_view);
                checkBox.setClickable(false);
//        } else {
//            }

            itemView.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            mOnListItemClickListener.onItemClick(mAdapterData.get(getAdapterPosition()), checkBox);
        }
    }

    @NonNull
    @Override
    public AddToListAdapter.VisualMediaAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        int resourceId = R.layout.object_add_to_list_model;


//        if (viewType == NO_POSTER_LAYOUT) {
//            resourceId = R.layout.object_list_model;

//        } else {
//            resourceId = R.layout.visualmedia;
//        }

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new VisualMediaAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final VisualMediaAdapterViewHolder adapterViewHolder, int position) {
//        Context context = adapterViewHolder.topFrameLayout.getContext();

        //get current VisualMediaData
        UserListModel currentUserListModel = mAdapterData.get(position);

//        //if item view type is no poster
//        if (adapterViewHolder.getItemViewType() == NO_POSTER_LAYOUT) {
//            //set title instead of poster image

        adapterViewHolder.listNameTextView.setText(currentUserListModel.getName());


        mOnListCheckListener
                .onCheckDatabaseRecords(adapterViewHolder.checkBox,
                        mAdapterData.get(position).getName());
//
//        } else {
//            //load image with glide
//            NetworkFunctions.loadImage(
//                    context,
//                    currentListModel.getPosterPath(),
//                    adapterViewHolder.moviePosterImageView);
//        }

//        AppDatabase.getInstance(adapterViewHolder.checkBox.getContext()).movieDataRecordsDao()
//                .getRecordByIds(mMediaId, adapterViewHolder.listNameTextView.getText().toString())
//                .observe(((AddToListFragment) mOnListItemClickListener), new Observer<MovieDataRecord>() {
//                    @Override
//                    public void onChanged(MovieDataRecord movieDataRecord) {
//                        Toast.makeText(adapterViewHolder.checkBox.getContext(), "add to list", Toast.LENGTH_SHORT).show();
//                        if (movieDataRecord != null) {
//                            adapterViewHolder.checkBox.setChecked(true);
//                        } else {
//                            adapterViewHolder.checkBox.setChecked(false);
//                        }
//
//                    }
//                });
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
//        //if poster path != null
//        boolean hasPoster = mAdapterData.get(position).getPosterPath() != null;

//        if (hasPoster) {
            return STANDARD_LAYOUT_ID;

//        } else {
//            return NO_POSTER_LAYOUT;
//        }
    }

//    public boolean isEmpty() {
//        return getItemCount() == 0;
//    }

    public List<UserListModel> getAdapterData() {
        return mAdapterData;
    }

    public void addAdapterData(List<UserListModel> visualMediaDataList) {
        mAdapterData.addAll(visualMediaDataList);
        notifyDataSetChanged();
    }

    public UserListModel getListModel(int index) {
        return mAdapterData.get(index);
    }

        //clears and updates adapterData
//    public void clearData() {
//        mAdapterData.clear();
//        notifyDataSetChanged();
//    }
}
