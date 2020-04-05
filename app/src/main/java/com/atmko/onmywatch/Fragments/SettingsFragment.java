/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.atmko.onmywatch.ConfirmationActivity;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.RestoreActivity;
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
                                Intent intent = new Intent(getContext(), RestoreActivity.class);
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

        Preference privacyPreference = findPreference(getString(R.string.settings_key_privacy));
        if (privacyPreference != null) {
            privacyPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    launchBrowserIntent(getString(R.string.privacy_url));
                    return true;
                }
            });
        }

        Preference termsPreference = findPreference(getString(R.string.settings_key_terms));
        if (termsPreference != null) {
            termsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    launchBrowserIntent(getString(R.string.terms_url));
                    return true;
                }
            });
        }
    }

    private void launchBrowserIntent(String url) {
        Uri webPage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webPage);
        if (getActivity() != null) {
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);

            } else {
                Toast.makeText(getContext(), getString(R.string.no_browser_error_message),
                        Toast.LENGTH_SHORT).show();
            }
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
