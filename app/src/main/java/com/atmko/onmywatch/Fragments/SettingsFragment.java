/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.atmko.onmywatch.BackupActivity;
import com.atmko.onmywatch.ConfirmationActivity;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.BackupService;

import static android.app.Activity.RESULT_OK;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final int REQUEST_CREATE_BACKUP = 1;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_settings);

        Preference restorePreference = findPreference(getString(R.string.settings_key_restore));
        if (restorePreference != null) {
            restorePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (getContext() != null) {
                                Intent intent = new Intent(getContext(), BackupActivity.class);
                                getContext().startActivity(intent);
                            }
                        }
                    });

                    return true;
                }
            });
        }

        Preference createBackupPreference = findPreference(getString(R.string.settings_key_create_backup));
        if (createBackupPreference != null) {
            createBackupPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            MasterActivity.launchConfirmationActivity(
                                    SettingsFragment.this, REQUEST_CREATE_BACKUP,
                                    ConfirmationActivity.ACTION_CREATE_BACKUP);
                        }
                    });

                    return true;
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_BACKUP) {
            if (resultCode == RESULT_OK) {
                if (getActivity() != null) {
                    getActivity().findViewById(R.id.progress_layout).setVisibility(View.VISIBLE);
                    Intent intent = new Intent(getContext(), BackupService.class);
                    BackupService.enqueueWork(getContext(), intent);
                }
            }
        }
    }
}
