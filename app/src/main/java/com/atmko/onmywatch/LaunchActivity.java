package com.atmko.onmywatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class LaunchActivity extends AppCompatActivity {
    public static final String APPLICATION_PREFERENCE_KEY = "application";
    public static final String AGREEMENT_KEY = "agreement";

    private AppCompatCheckBox checkBox;
    private TextView agreementErrorTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        if (isAgreementAcknowledged()) {
            startMasterActivity();

        } else {
            defineViews();
        }
    }

    private void defineViews() {
        agreementErrorTextView = findViewById(R.id.agreement_error_text_view);

        TextView termsTextView = findViewById(R.id.terms_text_view);
        SpannableString spannableString = new SpannableString(termsTextView.getText().toString());
        ClickableSpan termsClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                Toast.makeText(LaunchActivity.this, "launch terms", Toast.LENGTH_SHORT).show();
            }
        };
        ClickableSpan privacyClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                Toast.makeText(LaunchActivity.this, "launch privacy", Toast.LENGTH_SHORT).show();
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

        Button buttonContinue = findViewById(R.id.button_continue);
        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkBox.isChecked()) {
                    updateAgreementPreference();
                    startMasterActivity();

                } else {
                    showMissingAgreements();
                }
            }
        });
    }

    private boolean isAgreementAcknowledged() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(APPLICATION_PREFERENCE_KEY, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(AGREEMENT_KEY, false);
    }

    private void showMissingAgreements() {
        agreementErrorTextView.setVisibility(View.VISIBLE);
    }

    private void updateAgreementPreference() {
        SharedPreferences.Editor editor =
                getSharedPreferences(APPLICATION_PREFERENCE_KEY, Context.MODE_PRIVATE).edit();
        editor.putBoolean(AGREEMENT_KEY, checkBox.isChecked());
        editor.apply();
    }

    private void startMasterActivity() {
        Intent masterActivityIntent = new Intent(getApplicationContext(), MasterActivity.class);
        startActivity(masterActivityIntent);
        finish();
    }
}
