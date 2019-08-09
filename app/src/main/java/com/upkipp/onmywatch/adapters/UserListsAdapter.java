/*
 * Copyright (C) 2019 Aayat Mimiko
 */
package com.upkipp.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.models.UserListModel;

import java.util.ArrayList;
import java.util.List;

public class UserListsAdapter extends RecyclerView.Adapter<UserListsAdapter.VisualMediaAdapterViewHolder> {

    private final List<UserListModel> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private final OnSpinnerItemClickListener mOnSpinnerItemClickListener;

    //layout ids
    @SuppressWarnings("FieldCanBeLocal")
    private final int STANDARD_LAYOUT_ID = 1;
//    private final int NO_POSTER_LAYOUT = 2;

    public UserListsAdapter(OnListItemClickListener listItemClickListener) {
        mOnListItemClickListener = listItemClickListener;
        mOnSpinnerItemClickListener = ((OnSpinnerItemClickListener) listItemClickListener);
        mAdapterData = new ArrayList<>();
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
    }

    public interface OnSpinnerItemClickListener {
        void onEditClick();
        void onDeleteClick(UserListModel userListModel);
    }


   public class VisualMediaAdapterViewHolder extends RecyclerView.ViewHolder
             implements View.OnClickListener{

        TextView listNameTextView;
        TextView itemCountTextView;
        Spinner optionsSpinner;

        private VisualMediaAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

//            if (viewType == NO_POSTER_LAYOUT) {
                listNameTextView = itemView.findViewById(R.id.list_name_text_view);
                itemCountTextView = itemView.findViewById(R.id.item_count_text_view);
                optionsSpinner = itemView.findViewById(R.id.options_spinner);
//        } else {
//            }

            itemView.setOnClickListener(this);


            final Context context = itemView.getContext();

            final String[] optionsTitles = context.getResources().getStringArray(R.array.options_spinner_titles);
            SpinnerListOptionsAdapter spinnerAdapter = new SpinnerListOptionsAdapter(optionsTitles, context);

            optionsSpinner.setAdapter(spinnerAdapter);

            //prevents initial se,ection of spinner
            optionsSpinner.setSelection(optionsTitles.length - 1);

            optionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        mOnSpinnerItemClickListener.onEditClick();
                        optionsSpinner.setSelection(optionsTitles.length - 1, false);

                    } else if (position == 1) {
                        mOnSpinnerItemClickListener.onDeleteClick(mAdapterData.get(getAdapterPosition()));
                        optionsSpinner.setSelection(optionsTitles.length - 1, false);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            mOnListItemClickListener.onItemClick(position);
        }
    }

    @NonNull
    @Override
    public UserListsAdapter.VisualMediaAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        int resourceId = R.layout.object_list_model;


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
        final Context context = adapterViewHolder.listNameTextView.getContext();

        //get current VisualMediaData
        UserListModel currentUserListModel = mAdapterData.get(position);

//        //if item view type is no poster
//        if (adapterViewHolder.getItemViewType() == NO_POSTER_LAYOUT) {
//            //set title instead of poster image

        adapterViewHolder.listNameTextView.setText(currentUserListModel.getName());
        adapterViewHolder.itemCountTextView.setText(String.valueOf(currentUserListModel.getItemCount()));


//
//        } else {
//            //load image with glide
//            NetworkFunctions.loadImage(
//                    context,
//                    currentUserListModel.getPosterPath(),
//                    adapterViewHolder.moviePosterImageView);
//        }
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

    public void addAdapterData(List visualMediaDataList) {
        mAdapterData.addAll(visualMediaDataList);
        notifyDataSetChanged();
    }

    public UserListModel getUserListModel(int index) {
        return mAdapterData.get(index);
    }

        //clears and updates adapterData
//    public void clearData() {
//        mAdapterData.clear();
//        notifyDataSetChanged();
//    }
}
