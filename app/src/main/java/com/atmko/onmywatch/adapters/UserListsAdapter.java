/*
 * Copyright (C) 2019 Aayat Mimiko
 */
package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.UserListModel;

import java.util.ArrayList;
import java.util.List;

public class UserListsAdapter extends RecyclerView.Adapter<UserListsAdapter.UserListsAdapterViewHolder> {

    private final List<UserListModel> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private final OnSpinnerItemClickListener mOnSpinnerItemClickListener;

    //layout ids
    @SuppressWarnings("FieldCanBeLocal")

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

    public class UserListsAdapterViewHolder extends RecyclerView.ViewHolder
             implements View.OnClickListener{

        TextView listNameTextView;
        TextView itemCountTextView;
        Spinner optionsSpinner;

        private UserListsAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            listNameTextView = itemView.findViewById(R.id.list_name_text_view);
            itemCountTextView = itemView.findViewById(R.id.item_count_text_view);
            optionsSpinner = itemView.findViewById(R.id.options_spinner);

            itemView.setOnClickListener(this);

            final Context context = itemView.getContext();

            final String[] optionsTitles = context.getResources().getStringArray(R.array.options_spinner_titles);
            SpinnerListOptionsAdapter spinnerAdapter = new SpinnerListOptionsAdapter(optionsTitles, context);

            optionsSpinner.setAdapter(spinnerAdapter);

            //prevents initial selection of spinner
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
    @Override public UserListsAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        int resourceId = R.layout.object_list_model;

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new UserListsAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final UserListsAdapterViewHolder adapterViewHolder, int position) {
        //get current UserList
        UserListModel currentUserListModel = mAdapterData.get(position);

        adapterViewHolder.listNameTextView.setText(currentUserListModel.getName());
        adapterViewHolder.itemCountTextView.setText(String.valueOf(currentUserListModel.getItemCount()));
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
        return super.getItemViewType(position);
    }

    public List<UserListModel> getAdapterData() {
        return mAdapterData;
    }

    public void addAdapterData(List userListList) {
        mAdapterData.addAll(userListList);
        notifyDataSetChanged();
    }
}
