package com.atmko.onmywatch.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.atmko.onmywatch.MasterActivity;

import static com.atmko.onmywatch.widget.ListRemoteViewFactoryService.APP_WIDGET_ID_KEY;

public class ListWidgetService extends JobIntentService {
    public static final String ACTION_SWITCH_MEDIA_TYPE = "switch_media_type";

    public static final String MEDIA_TYPE_KEY = "media_type";

    private static final int JOB_ID = 9;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, ListWidgetService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (action.equals(ACTION_SWITCH_MEDIA_TYPE)) {
            int currentMediaType = extras.getInt(MEDIA_TYPE_KEY, 0);
            int appWidgetId = extras.getInt(APP_WIDGET_ID_KEY, 0);
            switchMediaType(currentMediaType, appWidgetId);
        }
    }

    private void switchMediaType(int currentMediaType, int appWidgetId) {
        int newMediaType;

        if (currentMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            newMediaType = MasterActivity.MEDIA_TYPE_SERIES;

        } else {
            newMediaType = MasterActivity.MEDIA_TYPE_MOVIE;

        }


        ListWidgetProviderConfigureActivity
                .saveMediaTypePref(getApplicationContext(), appWidgetId, newMediaType);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        ListWidgetProvider.updateAppWidget(getApplicationContext(), appWidgetManager, appWidgetId);
    }
}
