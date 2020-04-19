/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.models.UserListModel;

/*
 * data adapter for UserList objects
 */

public class UserListsAdapter extends ListsAdapter {
    private final OnSpinnerItemClickListener mOnSpinnerItemClickListener;

    public UserListsAdapter(OnListItemClickListener listItemClickListener) {
        super(listItemClickListener);
        mOnSpinnerItemClickListener = ((OnSpinnerItemClickListener) listItemClickListener);
    }

    public interface OnSpinnerItemClickListener {
        void onEditClick(ListModel listModel);
        void onDeleteClick(ListModel listModel);
    }

    private class UserListsAdapterViewHolder extends ListsAdapterViewHolder {
        private UserListsAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            if (viewType == EMPTY_ADAPTER_ID) return;

            final String[] optionsTitles =
                    itemView.getContext().getResources().getStringArray(R.array.options_spinner_titles);
            final Context context = itemView.getContext();

            SpinnerListOptionsAdapter spinnerAdapter = new SpinnerListOptionsAdapter(optionsTitles, context);
            optionsSpinner.setAdapter(spinnerAdapter);

            //prevents initial selection of spinner
            optionsSpinner.setSelection(optionsTitles.length - 1);

            optionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    UserListModel userListModel = ((UserListModel) mAdapterData.get(getAdapterPosition()));

                    if (position == 0) {
                        mOnSpinnerItemClickListener.onEditClick(userListModel);
                        optionsSpinner.setSelection(optionsTitles.length - 1, false);

                    } else if (position == 1) {
                        mOnSpinnerItemClickListener.onDeleteClick(userListModel);
                        optionsSpinner.setSelection(optionsTitles.length - 1, false);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
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

        return new UserListsAdapterViewHolder(view, viewType);
    }
}
