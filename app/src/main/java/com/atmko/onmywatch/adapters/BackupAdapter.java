package com.atmko.onmywatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.Backup;

import java.util.ArrayList;
import java.util.List;

public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.BackupAdapterViewHolder> {
    private final List<Backup> mAdapterData;
    private final BackupAdapter.OnListItemClickListener mOnListItemClickListener;
    private final Context mContext;

    public BackupAdapter(Context context) {
        mOnListItemClickListener = ((OnListItemClickListener) context);
        mAdapterData = new ArrayList<>();
        mContext = context;
    }

    public interface OnListItemClickListener {
        void onItemClick(int position);
    }

    public class BackupAdapterViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        final TextView timeStringTextView;

        private BackupAdapterViewHolder(@NonNull View itemView) {
            super(itemView);

            timeStringTextView = itemView.findViewById(R.id.time_string_text_view);

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
    public BackupAdapter.BackupAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                                          int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);

        int resourceId = R.layout.object_backup;
        View view = layoutInflater.inflate(resourceId, viewGroup, false);

        return new BackupAdapter.BackupAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BackupAdapter.BackupAdapterViewHolder adapterViewHolder, int position) {
        //get current Backup
        Backup currentBackup = mAdapterData.get(position);
        adapterViewHolder.timeStringTextView.setText(currentBackup.getTimeString());
    }

    @Override
    public int getItemCount() {
        return mAdapterData.size();
    }

    public List<Backup> getAdapterData() {
        return mAdapterData;
    }

    public void addAdapterData(List<Backup> backupList) {
        mAdapterData.addAll(backupList);
        notifyDataSetChanged();
    }
}
