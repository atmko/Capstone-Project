package com.upkipp.onmywatch.Fragments;

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

import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.adapters.CastDataAdapter;
import com.upkipp.onmywatch.models.CastData;

import org.parceler.Parcels;

import java.util.ArrayList;

public class CastFragment extends Fragment implements CastDataAdapter.OnListItemClickListener{
    public static String FRAGMENT_KEY = "cast_fragment";

    public static String CAST_PARCELABLE_KEY = "cast_parcelable";

    private ArrayList<CastData> mCastList;

    private RecyclerView.Adapter mediaDataAdapter;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cast, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        loadSearch();
    }

    private void loadSearch() {
        RecyclerView recyclerView = getView().findViewById(R.id.cast_recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());

        mediaDataAdapter = new CastDataAdapter(this);
        recyclerView.setAdapter(mediaDataAdapter);

        ((CastDataAdapter) mediaDataAdapter).addAdapterData(mCastList);
    }

    private GridLayoutManager configureLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.cast_column_span));

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    @Override
    public void onItemClick(int position) {

    }
}