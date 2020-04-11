/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.utils.GeneralUtils;

/*
 * data adapter for WatchList objects
 */

public class WatchListsAdapter extends ListsAdapter {
    public WatchListsAdapter(OnListItemClickListener listItemClickListener) {
        super(listItemClickListener);
    }

    private class WatchListsAdapterViewHolder extends ListsAdapterViewHolder {
        private WatchListsAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            if (viewType == EMPTY_ADAPTER_ID) return;

            optionsSpinner.setVisibility(View.GONE);
            checkBox.setVisibility(View.GONE);
        }
    }

    @NonNull
    @Override
    public ListsAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        int resourceId;

        if (viewType == EMPTY_ADAPTER_ID) {
            resourceId = R.layout.item_list_placeholder;

        } else {
            resourceId = R.layout.object_list_model;
        }

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new WatchListsAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final ListsAdapterViewHolder adapterViewHolder,
                                 int position) {
        if (adapterViewHolder.getItemViewType() == EMPTY_ADAPTER_ID) return;

        //get current listData
        ListModel currentListModel = mAdapterData.get(position);

        adapterViewHolder.listNameTextView.setText(
                GeneralUtils.convertToDisplayText(currentListModel.getName()));
        adapterViewHolder.itemCountTextView.
                setText(String.valueOf(currentListModel.getItemCount()));
    }
}
