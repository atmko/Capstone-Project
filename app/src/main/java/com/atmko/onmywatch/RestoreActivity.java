package com.atmko.onmywatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atmko.onmywatch.adapters.BackupAdapter;
import com.atmko.onmywatch.models.Backup;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.RestoreService;
import com.atmko.onmywatch.view_models.RestoreActivityViewModel;
import com.google.android.material.snackbar.Snackbar;

import org.parceler.Parcels;

import java.util.List;

public class RestoreActivity extends AppCompatActivity implements BackupAdapter.OnListItemClickListener,
        RestoreService.OnRestoreCompleteListener {
    private static final int REQUEST_RESTORE = 1;

    private BackupAdapter adapter;
    private FrameLayout progressLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore);

        //configure percentage of display dialog activity takes
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int height = displayMetrics.heightPixels *
                getResources().getInteger(R.integer.add_to_list_activity_popup_screen_percent) / 100;

        int width = displayMetrics.widthPixels *
                getResources().getInteger(R.integer.add_to_list_activity_popup_screen_percent) / 100;

        getWindow().setLayout(width, height);

        defineViews();
        observeData();
    }

    private void defineViews() {
        //configure recycler view
        RecyclerView backupRecyclerView = findViewById(R.id.backups_recycler_view);
        progressLayout = findViewById(R.id.progress_layout);
        adapter = new BackupAdapter(this);
        backupRecyclerView.setLayoutManager(configureLayoutManager());
        backupRecyclerView.setAdapter(adapter);
    }

    private void observeData() {
        RestoreActivityViewModel restoreActivityViewModel =
                ViewModelProviders.of(this).get(RestoreActivityViewModel.class);

        restoreActivityViewModel.getBackupsLiveData().observe(this, new Observer<List<Backup>>() {
            @Override
            public void onChanged(List<Backup> backups) {
                adapter.getAdapterData().clear();
                adapter.addAdapterData(backups);
            }
        });
    }

    private LinearLayoutManager configureLayoutManager() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RESTORE && resultCode == Activity.RESULT_OK) {
            progressLayout.setVisibility(View.VISIBLE);
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    if (data != null) {
                        Backup backup = Parcels.unwrap(
                                data.getParcelableExtra(ConfirmationActivity.SELECTED_DATA_KEY));

                        //restore backup
                        Intent intent = new Intent(RestoreActivity.this, RestoreService.class);
                        intent.putExtra(RestoreService.FOLDER_KEY, RestoreService.BACKUP_FOLDER_NAME);
                        intent.putExtra(RestoreService.FILENAME_KEY, backup.getFileName());
                        RestoreService.enqueueWork(RestoreActivity.this, intent);
                    }
                }
            });
        }
    }

    private void showSnackBarMessage(String string) {
        if (string == null || string.equals("")) return;
        Snackbar.make(findViewById(R.id.top_layout), string, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onItemClick(int position) {
        MasterActivity.launchConfirmationActivity(this,
                adapter.getAdapterData().get(position), REQUEST_RESTORE,
                ConfirmationActivity.ACTION_RESTORE);
    }

    @Override
    public void onRestoreComplete() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressLayout.setVisibility(View.GONE);
                showSnackBarMessage(getString(R.string.restore_completed_message));
            }
        });
    }

    @Override
    public void onRestoreFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressLayout.setVisibility(View.GONE);
                showSnackBarMessage(getString(R.string.restore_failed_message));
            }
        });
    }
}
