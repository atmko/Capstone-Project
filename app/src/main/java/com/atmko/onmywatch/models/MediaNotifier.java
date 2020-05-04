package com.atmko.onmywatch.models;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.room.ColumnInfo;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.utils.NotificationHandler;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;

public abstract class MediaNotifier {
    //notification channel ids
    private static final String RELEASE_CHANNEL_ID = "Release Channel";

    public static final String NOTIFICATIONS_KEY = "notification";

    public static final String CONDITION_KEY = "condition";

    public static final String IS_ACTIVE_KEY = "is_active";

    public static final int CONDITION_ON_RELEASE = 0;

    public final static String NOTIFIER_ID_KEY = "notifier_id";

    @NonNull
    @ColumnInfo(name = "media_id") public String mId = "";
    //condition under which notifications should be triggered
    @ColumnInfo(name = "condition") int mCondition;
    //tells whether there is an active alarm accompanying
    //useful when no release data available and notification alarm cant be yet set
    @ColumnInfo(name = "is_active") boolean mIsActive;

    //TODO consider using a LinkedHashSet seeing as titles are used as though they are unique
    public int getCondition() {
        return mCondition;
    }

    public String getMediaId() {
        return mId;
    }

    public boolean getIsActive() {
        return mIsActive;
    }

    public void setIsActive(boolean isActive) {
        this.mIsActive = isActive;
    }

    int getNotificationCode() {
        return Integer.parseInt(getCondition() + mId);
    }

    public static void createReleaseNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.notification_channel_releases);
            String description = context.getString(R.string.notification_channel_releases_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(RELEASE_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public abstract Notification createReleaseNotification(Context context,
                                                           MediaData mediaData,
                                                           int source);

    Notification buildReleaseNotification(Context context, String contentTitle,
                                          String contentText, PendingIntent pendingIntent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RELEASE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        return builder.build();
    }

    //creates pending intent to house notification
    public PendingIntent createPendingIntent(Context context, int mediaType,
                                             String mediaId, Notification notification) {
        Intent intent = new Intent(context, NotificationHandler.AlarmReceiver.class);
        intent.putExtra(MediaData.MEDIA_TYPE_KEY, mediaType);
        intent.putExtra(ApiConstants.ID_KEY, mediaId);
        intent.putExtra(CONDITION_KEY, mCondition);
        intent.putExtra(MediaNotifier.NOTIFICATIONS_KEY, notification);
        return PendingIntent
                .getBroadcast(context, getNotificationCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //creates pending intent without notification (for canceling alarm)
    public PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, NotificationHandler.AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, getNotificationCode(), intent, 0);
    }
}