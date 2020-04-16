/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.atmko.onmywatch.ConfirmationActivity;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.RestoreActivity;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.BackupService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.GoogleAuthProvider;

import static android.app.Activity.RESULT_OK;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final int REQUEST_CREATE_BACKUP = 1;
    private static final int UPGRADE_TO_PERMANENT_ID = 2;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_settings);

        Preference linkPreference = findPreference(getString(R.string.settings_key_link));
        if (linkPreference != null) {
            linkPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    if (MasterActivity.getCurrentUser().isAnonymous()) {
                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                            @Override
                            public void run() {
                                if (getActivity() != null) {
                                    googleSignInToPermanentAccount();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(getContext(), "Account Already Linked",
                                Toast.LENGTH_SHORT).show();
                    }

                    return true;
                }
            });
        }

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
        } else if (requestCode == UPGRADE_TO_PERMANENT_ID) {
            if (resultCode == RESULT_OK) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);

            } else {
                Toast.makeText(getContext(), "Failed To Link Account", Toast.LENGTH_SHORT).show();
                signOutFromGoogle();
            }
        }
    }

    private void googleSignInToPermanentAccount() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        if (getContext() == null) return;
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getContext(), options);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, UPGRADE_TO_PERMANENT_ID);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) convertAccountToPermanent(account);

        } catch (ApiException e) {
            Toast.makeText(getContext(), "Failed To Sign In", Toast.LENGTH_SHORT).show();
            signOutFromGoogle();
        }
    }

    private void convertAccountToPermanent(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        if (MasterActivity.getCurrentUser() != null && getActivity() != null) {
            MasterActivity.getCurrentUser().linkWithCredential(credential)
                    .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "Account Successfully Linked.",
                                        Toast.LENGTH_SHORT).show();
                                signOutFromGoogle();

                            } else {
                                if (task.getException() != null) {
                                    Toast.makeText(getContext(), task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }

                                signOutFromGoogle();
                            }
                        }
                    });
        }
    }

    private void signOutFromGoogle() {
        if (getActivity() != null) {
            GoogleSignInOptions gso =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .build();
            GoogleSignIn.getClient(getActivity(), gso).signOut();
        }
    }
}
