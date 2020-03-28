/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.atmko.onmywatch.utils.network_utils.BackupService;
import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends AppCompatActivity implements BackupService.OnBackupCompleteListener {
    private FrameLayout progressLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        defineViews();
    }

    private void defineViews() {
        progressLayout = findViewById(R.id.progress_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private void showSnackBarMessage(String string) {
        if (string == null || string.equals("")) return;
        Snackbar.make(findViewById(R.id.top_layout), string, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onLogOutBackupComplete() {

    }

    @Override
    public void onBackupComplete() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressLayout.setVisibility(View.GONE);
                showSnackBarMessage(getString(R.string.backup_completed_message));
            }
        });
    }

    @Override
    public void onBackupFailure() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressLayout.setVisibility(View.GONE);
                showSnackBarMessage(getString(R.string.backup_failure_message));
            }
        });
    }
}
