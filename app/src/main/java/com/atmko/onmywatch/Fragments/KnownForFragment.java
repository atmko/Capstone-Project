/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.adapters.CustomParams;
import com.atmko.onmywatch.adapters.MediaDataAdapter;
import com.atmko.onmywatch.models.MediaData;

import org.parceler.Parcels;

import java.util.List;

public class KnownForFragment extends Fragment implements MediaDataAdapter.OnListItemClickListener {
    public static final String FRAGMENT_KEY = "known_for_fragment";

    //TODO consider putting media type as an attribute in MediaData Class
    private static final String MEDIA_DATA_LIST_KEY = "media_data_list";

    private MediaDataAdapter mAdapter;
    private List<MediaData> mMediaDataList;

    public KnownForFragment() {
        // Required empty public constructor
    }

    public static KnownForFragment newInstance(Parcelable mediaDataListParcel) {
        KnownForFragment fragment = new KnownForFragment();

        Bundle args = new Bundle();
        args.putParcelable(MEDIA_DATA_LIST_KEY, mediaDataListParcel);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMediaDataList = Parcels.unwrap(getArguments().getParcelable(MEDIA_DATA_LIST_KEY));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_known_for, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineValues(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //update initialized media data
        assert getArguments() != null;
        outState.putParcelable(MEDIA_DATA_LIST_KEY, Parcels.wrap(mMediaDataList));
    }

    // TODO: NullPointerException handled in caller
    @SuppressWarnings("ConstantConditions")

    private void defineValues(Bundle savedInstanceState) {
        RecyclerView recyclerView = getView().findViewById(R.id.known_for_recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());

        mAdapter = new MediaDataAdapter(this, getActivity().getApplicationContext(),
                CustomParams.getDetailExtrasParams(this));

        if (savedInstanceState == null) {
            mAdapter.addAdapterData(mMediaDataList);

        } else {
            mAdapter.addAdapterData((List) Parcels.unwrap(savedInstanceState.getParcelable(MEDIA_DATA_LIST_KEY)));
        }

        recyclerView.setAdapter(mAdapter);
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.detail_extras_column_span));

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    @Override
    public void onItemClick(int position) {
        MediaData selectedData = mAdapter.getAdapterData().get(position);
        //do nothing if selecting stack placeholder
        if (selectedData.getId() == null) return;

        if (getParentFragment() != null && getParentFragment().getActivity() != null) {
            ((MasterActivity) getParentFragment().getActivity()).launchDetailsFragment(selectedData, null);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        Log.d(FRAGMENT_KEY, "detaching fragment");
    }
}
