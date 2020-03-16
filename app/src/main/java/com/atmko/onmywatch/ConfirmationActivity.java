package com.atmko.onmywatch;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.parceler.Parcels;

public class ConfirmationActivity extends AppCompatActivity {
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_LOG_OUT = "log_out";
    public static final String ACTION_RESTORE = "restore";

    public static final String ACTION_KEY = "action";
    public static final String CONFIRMATION_MESSAGE_KEY = "confirmation_message";
    public static final String SELECTED_DATA_KEY = "selected_data";

    //post initialization parameters
    private String action;
    private Object selectedData;
    private String confirmationMessage;

    private TextView confirmationTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        //configure percentage of display dialog activity takes
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int height = displayMetrics.heightPixels *
                getResources().getInteger(R.integer.rate_activity_popup_screen_percent) / 100;

        int width = displayMetrics.widthPixels *
                getResources().getInteger(R.integer.rate_activity_popup_screen_percent) / 100;

        getWindow().setLayout(width, height);

        defineViews();
        setValues(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ACTION_KEY, action);
        outState.putParcelable(SELECTED_DATA_KEY, Parcels.wrap(selectedData));
        outState.putString(CONFIRMATION_MESSAGE_KEY, confirmationMessage);
    }

    private void defineViews() {
        confirmationTextView = findViewById(R.id.confirmation_text_view);
        Button confirmationButton = findViewById(R.id.confirmation_button);
        confirmationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnConfirmationResult();
            }
        });
        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnCancelResult();
            }
        });
    }

    private void setValues(Bundle savedInstanceState) {
        //set values for first config initialization, otherwise restore values
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent != null) {
                action = intent.getAction();
                selectedData = Parcels.unwrap(intent.getParcelableExtra(SELECTED_DATA_KEY));
            } else {
                returnCancelResult();
            }

        } else {
            action = savedInstanceState.getString(ACTION_KEY);
            selectedData = Parcels.unwrap(savedInstanceState.getParcelable(SELECTED_DATA_KEY));
        }

        //set confirmation message if action and selected data exists, otherwise return cancel result
        if (action != null) {
            switch (action) {
                case ACTION_DELETE:
                    confirmationMessage = getString(R.string.delete_confirmation_message);

                    break;
                case ACTION_LOG_OUT:
                    confirmationMessage = getString(R.string.log_out_confirmation_message);

                    break;
                case ACTION_RESTORE:
                    confirmationMessage = getString(R.string.restore_confirmation_message);
                    break;
            }

            confirmationTextView.setText(confirmationMessage);
        } else {
            returnCancelResult();
        }
    }

    private void returnConfirmationResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(SELECTED_DATA_KEY, Parcels.wrap(selectedData));
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void returnCancelResult() {
        Intent resultIntent = new Intent();
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }
}
