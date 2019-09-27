/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.atmko.onmywatch.R;

public class SpinnerListOptionsAdapter implements SpinnerAdapter {
    private String[] mOptionsTitles;
    private Context mContext;

    SpinnerListOptionsAdapter(String[] optionsTitles, Context context) {
        this.mOptionsTitles = optionsTitles;
        this.mContext = context;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);

        int resourceId;
        View view;

        if (position == mOptionsTitles.length - 1) {
            resourceId = R.layout.spinner_empty_object;
            view = layoutInflater.inflate(resourceId, viewGroup, false);

        } else {
            resourceId = R.layout.spinner_options_object;
            view = layoutInflater.inflate(resourceId, viewGroup, false);

            TextView ingredientHeadingTextView = view.findViewById(R.id.option_text_view);

            String optionTitle = mOptionsTitles[position];

            ingredientHeadingTextView.setText(optionTitle);
        }

        return view;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        if (mOptionsTitles != null) return mOptionsTitles.length;

        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);

        int resourceId = R.layout.spinner_empty_object;
        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        TextView ingredientHeadingTextView = view.findViewById(R.id.option_text_view);

        ingredientHeadingTextView.setVisibility(View.GONE);

        return view;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return  (mOptionsTitles == null || mOptionsTitles.length == 0);
    }
}
