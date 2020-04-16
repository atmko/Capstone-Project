package com.atmko.onmywatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LaunchActivity extends AppCompatActivity {
    public static final String AGREEMENT_KEY = "agreement";

    private static final int REQUEST_GOOGLE_SIGN_IN = 0;

    private AppCompatCheckBox checkBox;
    private TextView agreementErrorTextView;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        defineViews();
        defineValues();

        if (MasterActivity.getCurrentUser() != null) {
            startMasterActivity(false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GOOGLE_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);

            } else {
                showSnackBarMessage(getString(R.string.log_in_failed_message));
            }
        }
    }

    private void defineViews() {
        TextView disclaimerSuffixTextView = findViewById(R.id.no_streaming_prefix_text_view);
        SpannableString disclaimerSpannableString = new SpannableString(disclaimerSuffixTextView
                .getText().toString());
        ClickableSpan disclaimerSuffixSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                launchBrowserIntent(getString(R.string.streaming_disclaimer_suffix));
            }
        };
        int[] disclaimerClickableSpans = getResources().getIntArray(R.array.disclaimer_suffix_clickable_spans);
        disclaimerSpannableString.setSpan(disclaimerSuffixSpan, disclaimerClickableSpans[0],
                disclaimerClickableSpans[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        disclaimerSuffixTextView.setText(disclaimerSpannableString);
        disclaimerSuffixTextView.setMovementMethod(LinkMovementMethod.getInstance());
        disclaimerSuffixTextView.setHighlightColor(Color.TRANSPARENT);

        agreementErrorTextView = findViewById(R.id.agreement_error_text_view);
        TextView termsTextView = findViewById(R.id.terms_text_view);
        SpannableString spannableString = new SpannableString(termsTextView.getText().toString());
        ClickableSpan termsClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                launchBrowserIntent(getString(R.string.terms_url));
            }
        };
        ClickableSpan privacyClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                launchBrowserIntent(getString(R.string.privacy_url));
            }
        };
        int[] clickableSpans = getResources().getIntArray(R.array.terms_clickable_spans);
        spannableString.setSpan(termsClickableSpan, clickableSpans[0],
                clickableSpans[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyClickableSpan, clickableSpans[2],
                clickableSpans[3], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsTextView.setText(spannableString);
        termsTextView.setMovementMethod(LinkMovementMethod.getInstance());
        termsTextView.setHighlightColor(Color.TRANSPARENT);

        checkBox = findViewById(R.id.i_understand_checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isAgreed) {
                if (isAgreed) {
                    agreementErrorTextView.setVisibility(View.GONE);
                }
            }
        });

        Button googleContinue = findViewById(R.id.google_continue);
        googleContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkBox.isChecked()) {
                    continueWithGoogle();

                } else {
                    showMissingAgreements();
                }
            }
        });
    }

    private void continueWithGoogle() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, options);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, REQUEST_GOOGLE_SIGN_IN);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) linkedFirebaseLogIn(account);

        } catch (ApiException e) {
            showSnackBarMessage(getString(R.string.log_in_failed_message));
            signOutFromGoogle();
        }
    }

    private void linkedFirebaseLogIn(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        updateAgreementPreference();
                        startMasterActivity(true);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showSnackBarMessage(getString(R.string.log_in_failed_message));
                signOutFromGoogle();
            }
        });
    }

    private void signOutFromGoogle() {
        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();
        GoogleSignIn.getClient(this, gso).signOut();
    }

    private void defineValues() {
        sharedPreferences = getSharedPreferences(getString(R.string.application_shared_prefs_key),
                Context.MODE_PRIVATE);

        if (isAgreementAcknowledged()) {
            checkBox.setChecked(true);
            findViewById(R.id.i_understand_linear_layout).setVisibility(View.GONE);
            findViewById(R.id.agreement_error_text_view).setVisibility(View.GONE);
        }
    }

    private boolean isAgreementAcknowledged() {
        return sharedPreferences.getBoolean(AGREEMENT_KEY, false);
    }

    private void showMissingAgreements() {
        agreementErrorTextView.setVisibility(View.VISIBLE);
    }

    private void updateAgreementPreference() {
        sharedPreferences.edit()
                .putBoolean(AGREEMENT_KEY, checkBox.isChecked())
                .apply();
    }

    private void launchBrowserIntent(String url) {
        Uri webPage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webPage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else  {
            ConstraintLayout topView = findViewById(R.id.top_layout);
            Snackbar.make(topView, R.string.no_browser_error_message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void startMasterActivity(Boolean isLoggingIn) {
        Intent masterActivityIntent = new Intent(getApplicationContext(), MasterActivity.class);
        masterActivityIntent.putExtra(MasterActivity.IS_LOGGING_IN_KEY, isLoggingIn);
        startActivity(masterActivityIntent);
        finish();
    }

    private void showSnackBarMessage(String string) {
        if (string == null || string.equals("")) return;
        Snackbar.make(findViewById(R.id.top_layout), string, Snackbar.LENGTH_LONG).show();
    }
}
