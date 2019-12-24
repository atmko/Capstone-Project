/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.CastDataAdapter;
import com.atmko.onmywatch.adapters.CustomParams;
import com.atmko.onmywatch.models.CastData;

import org.parceler.Parcels;

import java.util.ArrayList;

public class CastFragment extends Fragment implements CastDataAdapter.OnListItemClickListener{
    public static String FRAGMENT_KEY = "cast_fragment";

    private static final String CAST_PARCELABLE_KEY = "cast_parcelable";

    private ArrayList<CastData> mCastList;
    private RecyclerView.Adapter mAdapter;

    public CastFragment() {
        // Required empty public constructor
    }

    public static CastFragment newInstance(Parcelable castParcel) {
        CastFragment fragment = new CastFragment();
        Bundle args = new Bundle();
        args.putParcelable(CAST_PARCELABLE_KEY, castParcel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCastList = Parcels.unwrap(getArguments().getParcelable(CAST_PARCELABLE_KEY));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cast, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            loadSearch();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")
    private void loadSearch() throws NullPointerException{
        RecyclerView recyclerView = getView().findViewById(R.id.cast_recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());

        mAdapter = new CastDataAdapter(this, getActivity().getApplicationContext(),
                CustomParams.getDetailExtrasParams(this));
        recyclerView.setAdapter(mAdapter);

        ((CastDataAdapter) mAdapter).addAdapterData(mCastList);
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.detail_extras_column_span));

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    //TODO implement cast details page launcher
    @Override
    public void onItemClick(int position) {

    }
}