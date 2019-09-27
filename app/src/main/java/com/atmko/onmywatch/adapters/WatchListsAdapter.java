/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.WatchListModel;

import java.util.ArrayList;
import java.util.List;

/*
 * data adapter for WatchList objects
 */

public class WatchListsAdapter extends RecyclerView.Adapter<WatchListsAdapter.WatchListsAdapterViewHolder> {

    private final List<WatchListModel> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;

    //layout ids
    @SuppressWarnings("FieldCanBeLocal")

    public WatchListsAdapter(OnListItemClickListener listItemClickListener) {
        mOnListItemClickListener = listItemClickListener;
        mAdapterData = new ArrayList<>();
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
    }

   public class WatchListsAdapterViewHolder extends RecyclerView.ViewHolder
             implements View.OnClickListener{

        TextView listNameTextView;
        TextView itemCountTextView;
        Spinner optionsSpinner;

        private WatchListsAdapterViewHolder(@NonNull View itemView, int viewType) {
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

        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            mOnListItemClickListener.onItemClick(position);
        }
    }

    @NonNull
    @Override
    public WatchListsAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        int resourceId = R.layout.object_list_model;

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new WatchListsAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final WatchListsAdapterViewHolder adapterViewHolder, int position) {
        final Context context = adapterViewHolder.listNameTextView.getContext();

        //get current WatchListData
        WatchListModel currentWatchListModel = mAdapterData.get(position);

        adapterViewHolder.listNameTextView.setText(currentWatchListModel.getName());
        adapterViewHolder.itemCountTextView.setText(String.valueOf(currentWatchListModel.getItemCount()));
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

    public List<WatchListModel> getAdapterData() {
        return mAdapterData;
    }

    public void addAdapterData(List watchListDataList) {
        mAdapterData.addAll(watchListDataList);
        notifyDataSetChanged();
    }
}
