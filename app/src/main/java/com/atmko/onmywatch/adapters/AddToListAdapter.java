/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.UserListModel;

import java.util.ArrayList;
import java.util.List;

/*
 * data adapter for list objects in AddToListActivity
 */

public class AddToListAdapter extends RecyclerView.Adapter<AddToListAdapter.AddToListViewHolder> {
    private final List<UserListModel> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private final OnListCheckListener mOnListCheckListener;

    //layout ids
    @SuppressWarnings("FieldCanBeLocal")

    public AddToListAdapter(OnListItemClickListener clickListener, String mediaId) {
        mOnListItemClickListener = clickListener;
        mOnListCheckListener = ((OnListCheckListener) clickListener);
        mAdapterData = new ArrayList<>();
    }

    public interface OnListItemClickListener {
        void onItemClick(UserListModel userListModel, AppCompatCheckBox checkBox);
    }

    public interface OnListCheckListener {
        void onCheckDatabaseRecords(AppCompatCheckBox checkBox, UserListModel listName);
    }

    public class AddToListViewHolder extends RecyclerView.ViewHolder
             implements View.OnClickListener{

        TextView listNameTextView;
        AppCompatCheckBox checkBox;

        private AddToListViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            listNameTextView = itemView.findViewById(R.id.list_name_text_view);
            checkBox = itemView.findViewById(R.id.checkbox_view);
            checkBox.setClickable(false);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mOnListItemClickListener.onItemClick(mAdapterData.get(getAdapterPosition()), checkBox);
        }
    }

    @NonNull
    @Override
    public AddToListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        int resourceId = R.layout.object_add_to_list_model;

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new AddToListViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final AddToListViewHolder adapterViewHolder, int position) {
        //get current user list
        UserListModel currentUserListModel = mAdapterData.get(position);

        adapterViewHolder.listNameTextView.setText(currentUserListModel.getName());

        mOnListCheckListener
                .onCheckDatabaseRecords(adapterViewHolder.checkBox,
                        mAdapterData.get(position));
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

    public void addAdapterData(List<UserListModel> dataList) {
        mAdapterData.addAll(dataList);
        notifyDataSetChanged();
    }
}
