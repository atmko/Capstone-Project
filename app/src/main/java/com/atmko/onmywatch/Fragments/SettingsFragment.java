/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.RestoreService;

import static com.atmko.onmywatch.ConfirmationActivity.ACTION_RESTORE;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final int REQUEST_RESTORE = 1;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_settings);

        Preference preference = findPreference(getString(R.string.settings_key_restore));
        if (preference != null) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MasterActivity.launchConfirmationActivity(
                            SettingsFragment.this, REQUEST_RESTORE, ACTION_RESTORE);
                    return true;
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RESTORE && resultCode == Activity.RESULT_OK) {
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    //restore backup;
                    Intent intent = new Intent(getContext(), RestoreService.class);
                    intent.putExtra(RestoreService.FOLDER_KEY, RestoreService.BACKUP_FOLDER_NAME);
                    intent.putExtra(RestoreService.FILENAME_KEY, RestoreService.BACKUP_FILE_NAME);
                    RestoreService.enqueueWork(getContext(), intent);
                }
            });
        }
    }
}
