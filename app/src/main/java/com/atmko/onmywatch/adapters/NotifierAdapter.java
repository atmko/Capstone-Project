/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.R;

import java.util.ArrayList;
import java.util.List;

/*
 * data adapter for notification items in Notifier Activity
 */

public class NotifierAdapter extends RecyclerView.Adapter<NotifierAdapter.NotifierViewHolder> {
    private final List<String> mConditionTitles;
    private final List<Boolean> mConditionValues;
    private final OnListItemClickListener mOnListItemClickListener;

    //layout ids
    @SuppressWarnings("FieldCanBeLocal")

    public NotifierAdapter(OnListItemClickListener clickListener, String mediaId) {
        mOnListItemClickListener = clickListener;
        mConditionTitles = new ArrayList<>();
        mConditionValues = new ArrayList<>();
    }

    public interface OnListItemClickListener {
        void onItemClick(int position, AppCompatCheckBox checkBox);
    }

    public class NotifierViewHolder extends RecyclerView.ViewHolder
             implements View.OnClickListener{

        TextView conditionNameTextView;
        AppCompatCheckBox checkBox;

        private NotifierViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            conditionNameTextView = itemView.findViewById(R.id.condition_name_text_view);
            checkBox = itemView.findViewById(R.id.checkbox_view);
            checkBox.setClickable(false);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mOnListItemClickListener.onItemClick(getAdapterPosition(), checkBox);
        }
    }

    @NonNull
    @Override
    public NotifierViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        int resourceId = R.layout.object_notifier_condition;

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new NotifierViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final NotifierViewHolder adapterViewHolder, int position) {
        //get current condition title
        String currentConditionTitle = mConditionTitles.get(position);
        //get current condition value
        boolean currentConditionValue = mConditionValues.get(position);

        adapterViewHolder.conditionNameTextView.setText(currentConditionTitle);
        adapterViewHolder.checkBox.setChecked(currentConditionValue);

    }

    @Override
    public int getItemCount() {
        if (mConditionTitles == null) {
            return 0;
        } else {
            return mConditionTitles.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public void addAdapterData(List<String> conditionTitles, SparseBooleanArray conditionValues) {
        for (int index = 0; index < conditionValues.size(); index++) {
            mConditionTitles.add(conditionTitles.get(index));
        }

//        mConditionTitles.addAll(conditionTitles);

        for (int index = 0; index < conditionValues.size(); index++) {
            mConditionValues.add(conditionValues.get(index));
        }

        notifyDataSetChanged();
    }

    public void clearAdapterData() {
        mConditionTitles.clear();
        mConditionValues.clear();
    }
}
