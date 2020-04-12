/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.atmko.onmywatch.Fragments.DetailsFragment;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.utils.GeneralUtils;

import static com.atmko.onmywatch.widget.ListRemoteViewFactoryService.APP_WIDGET_ID_KEY;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link ListWidgetProviderConfigureActivity NewAppWidgetConfigureActivity}
 */
public class ListWidgetProvider extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        String widgetListName =
                ListWidgetProviderConfigureActivity.loadTitlePref(context, appWidgetId);
        int widgetMediaType =
                ListWidgetProviderConfigureActivity.loadMediaTypePref(context, appWidgetId);
        String widgetMediaTypeText =
                MediaData.getMediaTypeTitle(widgetMediaType, context);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_list_provider);

        views.setTextViewText(R.id.list_title_text,
                GeneralUtils.convertToDisplayText(widgetListName));
        views.setTextViewText(R.id.media_type_text_view,
                GeneralUtils.convertToDisplayText(widgetMediaTypeText));

        //create RemoteViewsService intent
        Intent widgetFactoryIntent = new Intent(context, ListRemoteViewFactoryService.class);

        //create intent extras
        Bundle intentExtras = new Bundle();
        //put media type value
        intentExtras.putInt(APP_WIDGET_ID_KEY, appWidgetId);

        //set intent extras
        widgetFactoryIntent.putExtras(intentExtras);

        //set remote adapter
        views.setRemoteAdapter(R.id.list_item_list_view, widgetFactoryIntent);


        //configure widget list item pending intent template
        Intent listItemIntent = new Intent(context, MasterActivity.class);
        listItemIntent.setAction(DetailsFragment.ACTION_LAUNCH_DETAILS);
        PendingIntent masterActivityPendingIntent = PendingIntent.getActivity(context, 0 ,
                listItemIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.list_item_list_view, masterActivityPendingIntent);


        int[] appWidgetIds =
                appWidgetManager.getAppWidgetIds(new ComponentName(context, ListWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_item_list_view);

        //configure settings button
        Intent widgetSettingsIntent = new Intent(context, ListWidgetProviderConfigureActivity.class);

        Bundle widgetSettingsBundle = new Bundle();
        widgetSettingsBundle.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        widgetSettingsIntent.putExtras(widgetSettingsBundle);

        PendingIntent widgetSettingsPendingIntent =
                PendingIntent.getActivity(context, 0, widgetSettingsIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.widget_settings_button, widgetSettingsPendingIntent);

        //configure switch media button
        Intent switchMediaIntent = new Intent(context, ListWidgetProvider.class);
        Bundle extras = new Bundle();
        extras.putInt(ListWidgetService.MEDIA_TYPE_KEY, widgetMediaType);
        extras.putInt(APP_WIDGET_ID_KEY, appWidgetId);
        switchMediaIntent.putExtras(extras);
        switchMediaIntent.setAction(ListWidgetService.ACTION_SWITCH_MEDIA_TYPE);

        PendingIntent switchMediaPendingIntent =
                PendingIntent.getBroadcast(context, 1, switchMediaIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.media_type_text_view, switchMediaPendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            ListWidgetProviderConfigureActivity.deleteTitlePref(context, appWidgetId);
            ListWidgetProviderConfigureActivity.deleteListTypePref(context, appWidgetId);
            ListWidgetProviderConfigureActivity.deleteMediaTypePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        ListWidgetService.enqueueWork(context, intent);
    }

    public static void updateWidgets(Context context) {
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, ListWidgetProvider.class));
        Intent intent = new Intent(context, ListWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }
}

