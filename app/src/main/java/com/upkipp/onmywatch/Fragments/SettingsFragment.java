package com.upkipp.onmywatch.Fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.upkipp.onmywatch.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_settings);
    }
}
