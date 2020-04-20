/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.ListModel;

import java.util.ArrayList;
import java.util.List;

/*
 * data adapter for ListData objects
 */

public abstract class ListsAdapter extends RecyclerView.Adapter<ListsAdapter.ListsAdapterViewHolder> {
    //layout ids
    final int EMPTY_ADAPTER_ID = -1;

    final List<ListModel> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private boolean mInPlaceholderMode;

    ListsAdapter(OnListItemClickListener listItemClickListener) {
        mOnListItemClickListener = listItemClickListener;
        mAdapterData = new ArrayList<>();
    }

    public interface OnListItemClickListener {
        void onItemClick(ListModel listModel, AppCompatCheckBox checkBox);
    }

    public class ListsAdapterViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        final TextView listNameTextView;
        final TextView itemCountTextView;
        final Spinner optionsSpinner;
        final AppCompatCheckBox checkBox;

        ListsAdapterViewHolder(@NonNull View itemView) {
            super(itemView);

            listNameTextView = itemView.findViewById(R.id.list_name_text_view);
            itemCountTextView = itemView.findViewById(R.id.item_count_text_view);
            optionsSpinner = itemView.findViewById(R.id.options_spinner);
            checkBox = itemView.findViewById(R.id.checkbox_view);
            if (checkBox != null) {
                checkBox.setClickable(false);
            }

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            mOnListItemClickListener.onItemClick(mAdapterData.get(position), checkBox);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ListsAdapterViewHolder adapterViewHolder,
                                 int position) {
        if (adapterViewHolder.getItemViewType() == EMPTY_ADAPTER_ID) return;

        //get current listData
        ListModel currentListModel = mAdapterData.get(position);

        adapterViewHolder.listNameTextView.setText(currentListModel.getName());
        adapterViewHolder.itemCountTextView.
                setText(String.valueOf(currentListModel.getItemCount()));
    }

    @Override
    public int getItemCount() {
        return mAdapterData.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mInPlaceholderMode) return EMPTY_ADAPTER_ID;

        return super.getItemViewType(position);
    }

    public List getAdapterData() {
        return mAdapterData;
    }

    @SuppressWarnings("unchecked")
    public void addAdapterData(List listDataList) {
        mAdapterData.addAll(listDataList);
        notifyDataSetChanged();
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
}
