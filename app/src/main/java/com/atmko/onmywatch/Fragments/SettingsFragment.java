/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.atmko.onmywatch.BackupActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;

public class SettingsFragment extends PreferenceFragmentCompat {
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
    }
}
