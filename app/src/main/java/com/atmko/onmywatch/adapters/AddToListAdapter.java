/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.UserListModel;

/*
 * data adapter for list objects in AddToListActivity
 */

public class AddToListAdapter extends ListsAdapter {
    private final OnListCheckListener mOnListCheckListener;

    public AddToListAdapter(OnListItemClickListener clickListener) {
        super(clickListener);
        mOnListCheckListener = ((OnListCheckListener) clickListener);
    }

    public interface OnListCheckListener {
        void onCheckDatabaseRecords(AppCompatCheckBox checkBox, UserListModel listName);
    }

    public class AddToListViewHolder extends ListsAdapterViewHolder {

        private AddToListViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            if (viewType == EMPTY_ADAPTER_ID) return;

            optionsSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ListsAdapterViewHolder adapterViewHolder, int position) {
        super.onBindViewHolder(adapterViewHolder, position);

        if (adapterViewHolder.getItemViewType() == EMPTY_ADAPTER_ID) return;

        mOnListCheckListener
                .onCheckDatabaseRecords(adapterViewHolder.checkBox,
                        ((UserListModel) mAdapterData.get(position)));
    }

    @NonNull
    @Override
    public AddToListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        int resourceId;

        if (viewType == EMPTY_ADAPTER_ID) {
            resourceId = R.layout.item_list_placeholder_marginless;

        } else {
            resourceId = R.layout.object_list_model_marginless;
        }

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new AddToListViewHolder(view, viewType);
    }
}
