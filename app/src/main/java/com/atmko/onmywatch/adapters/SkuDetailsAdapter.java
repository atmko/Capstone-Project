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
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.SkuDetails;
import com.atmko.onmywatch.R;

import java.util.ArrayList;
import java.util.List;

/*
 * data adapter for Media data objects
 */

public class SkuDetailsAdapter
        extends RecyclerView.Adapter<SkuDetailsAdapter.SkuDetailsAdapterViewHolder> {

    private final List<SkuDetails> mAdapterData;
    private final OnListItemClickListener mOnListItemClickListener;
    private final OnCheckPurchaseStateListener mOnCheckPurchaseStateListener;
    private final Context mContext;

    //layout ids
    private final int ONE_TIME_LAYOUT_ID = 1;
    private final int SUBSCRIPTION_LAYOUT_ID = 2;

    public SkuDetailsAdapter(OnListItemClickListener clickListener, Context context) {
        mOnListItemClickListener = clickListener;
        mOnCheckPurchaseStateListener = ((OnCheckPurchaseStateListener) context);
        mAdapterData = new ArrayList<>();
        mContext = context;
    }

    public interface OnCheckPurchaseStateListener {
        void onPurchaseStateCheck(String sku, AppCompatCheckBox checkBox);
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
    }

    public class SkuDetailsAdapterViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        final TextView titleTextView;
        final TextView priceTextView;
        final TextView descriptionTextView;
        ImageView iconImageView;
        final AppCompatCheckBox checkBox;

        private SkuDetailsAdapterViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            titleTextView = itemView.findViewById(R.id.title_text_view);
            priceTextView = itemView.findViewById(R.id.price_text_view);
            descriptionTextView = itemView.findViewById(R.id.description_text_view);
            checkBox = itemView.findViewById(R.id.checkbox_view);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            mOnListItemClickListener.onItemClick(position);
        }
    }

    @NonNull
    @Override
    public SkuDetailsAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                          int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);

        int resourceId = 0;

        if (viewType == ONE_TIME_LAYOUT_ID) {
            resourceId = R.layout.object_one_time_layout;
        }
        else if (viewType == SUBSCRIPTION_LAYOUT_ID) {
            resourceId = R.layout.object_one_time_layout;
        }

        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new SkuDetailsAdapterViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull SkuDetailsAdapterViewHolder adapterViewHolder, int position) {
        //get current SkuDetails
        SkuDetails currentSkuDetails = mAdapterData.get(position);

        //set title instead of poster image
        adapterViewHolder.titleTextView.setText(currentSkuDetails.getTitle());
        adapterViewHolder.priceTextView.setText(currentSkuDetails.getPrice());
        adapterViewHolder.descriptionTextView.setText(currentSkuDetails.getDescription());
        mOnCheckPurchaseStateListener
                .onPurchaseStateCheck(currentSkuDetails.getSku(), adapterViewHolder.checkBox);
    }

    @Override
    public int getItemCount() {
        return mAdapterData.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mAdapterData.get(position).getType().equals(BillingClient.SkuType.INAPP)) {
            return ONE_TIME_LAYOUT_ID;

        } else {
            return SUBSCRIPTION_LAYOUT_ID;
        }
    }

    public List<SkuDetails> getAdapterData() {
        return mAdapterData;
    }

    public void addAdapterData(List<SkuDetails> skuDetails) {
        mAdapterData.addAll(skuDetails);
        notifyDataSetChanged();
    }
}
