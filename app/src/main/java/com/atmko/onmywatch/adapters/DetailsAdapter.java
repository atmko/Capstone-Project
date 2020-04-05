/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.R;

import org.parceler.Parcel;

import java.util.HashMap;
import java.util.Map;

/*
 * data adapter for Detail objects
 */

public class DetailsAdapter
        extends RecyclerView.Adapter<DetailsAdapter.MediaDataAdapterViewHolder> {

    private final Map<Integer, DetailObject> mAdapterData;
    private Context mContext;

    public DetailsAdapter(Context context) {
        mContext = context;
        mAdapterData = new HashMap<>();
    }

    @Parcel
    public static class DetailObject {
        public static final int ID_WATCH_STATUS = 0;
        public static final int ID_LIST_COUNT = 1;
        public static final int ID_RELEASE_STATUS = 2;
        public static final int ID_COUNTDOWN = 3;
        public static final int ID_NETWORK = 4;

        private int id;
        public int index;
        public String text;
        private Integer imageId;
        public Integer imageVisibility;

        //constructor for parceler
        public DetailObject() {
        }

        public DetailObject(int id, String text) {
            this.id = id;
            this.text = text;
        }

        public DetailObject(int id, String text, Integer imageId, int imageVisibility) {
            this.id = id;
            this.text = text;
            this.imageId = imageId;
            this.imageVisibility = imageVisibility;
        }
    }

    static class MediaDataAdapterViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView image;

        private MediaDataAdapterViewHolder(@NonNull View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.text);
            image = itemView.findViewById(R.id.image);
        }
    }

    @NonNull
    @Override
    public MediaDataAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                         int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);

        int resourceId = R.layout.item_detail;

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new MediaDataAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaDataAdapterViewHolder adapterViewHolder, int position) {
        //get current DetailObject
        DetailObject currentDetailObject = mAdapterData.get(position);
        if (currentDetailObject != null) {
            adapterViewHolder.text.setText(currentDetailObject.text);
            if (currentDetailObject.imageId != null) {
                adapterViewHolder.image
                        .setImageDrawable(mContext.getResources().getDrawable(currentDetailObject.imageId));
                adapterViewHolder.image.setVisibility(currentDetailObject.imageVisibility);
            } else {
                adapterViewHolder.image.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mAdapterData == null) {
            return 0;
        } else {
            return mAdapterData.size();
        }
    }

    public DetailObject getDetailObject(int id) {
        return mAdapterData.get(id);
    }

    public Map<Integer, DetailObject> getAdapterData() {
        return mAdapterData;
    }

    public void addAdapterData(Map<Integer, DetailObject> detailObjects) {
        for (DetailObject detailObject: detailObjects.values()) {
            mAdapterData.put(detailObject.id, detailObject);
        }

        notifyDataSetChanged();
    }

    public void addDetailObjectData(DetailObject detailObject) {
        detailObject.index = mAdapterData.size();
        mAdapterData.put(detailObject.id, detailObject);

        notifyDataSetChanged();
    }
}
