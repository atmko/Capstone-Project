package com.atmko.onmywatch.models;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.room.ColumnInfo;

import com.atmko.onmywatch.Fragments.DetailsFragment;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.utils.NotificationHandler;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;

import org.parceler.Parcels;

public abstract class MediaNotifier {
    //notification channel ids
    public static final String RELEASE_CHANNEL_ID = "Release Channel";

    public static final String NOTIFICATIONS_KEY = "notification";

    public static final String CONDITION_KEY = "condition";

    public static final int CONDITION_ON_RELEASE = 0;

    @NonNull
    @ColumnInfo(name = "media_id") public String mId;
    //condition under which notifications should be triggered
    @NonNull
    @ColumnInfo(name = "condition") int mCondition;

    //TODO consider using a LinkedHashSet seeing as titles are used as though they are unique
    public int getCondition() {
        return mCondition;
    }

    public String getMediaId() {
        return mId;
    }

    public int getNotificationCode() {
        return Integer.valueOf(getCondition() + mId);
    }

    public static void createReleaseNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.notification_channel_releases);
            String description = context.getString(R.string.notification_channel_releases_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(RELEASE_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public Notification createReleaseNotification(Context context, MediaData mediaData) {
        String contentTitle = context.getString(R.string.notification_new_release_title);
        String contentText = mediaData.getTitle() + " "
                        + context.getString(R.string.notification_new_release_content_suffix);

        //create intent to launch activity on click
        Intent detailsIntent = new Intent(context, MasterActivity.class);
        detailsIntent.setAction(DetailsFragment.ACTION_LAUNCH_DETAILS);
        detailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        detailsIntent.putExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mediaData));

        Bundle detailsExtras = new Bundle();
        detailsIntent.putExtras(detailsExtras);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, detailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RELEASE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_rate)
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
        return PendingIntent.getBroadcast(context, getNotificationCode(), intent, 0);
    }

    //creates pending intent without notification (for canceling alarm)
    public PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, NotificationHandler.AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, getNotificationCode(), intent, 0);
    }
}