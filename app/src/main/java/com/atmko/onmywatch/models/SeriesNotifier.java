package com.atmko.onmywatch.models;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;

@Entity(tableName = "series_notifiers",
        primaryKeys = {"media_id", "condition"},
        foreignKeys =
                {@ForeignKey(entity = SeriesData.class, parentColumns = "id", childColumns = "media_id",
                        onDelete = ForeignKey.CASCADE)}
)

public class SeriesNotifier extends MediaNotifier {
    public static final int CONDITION_NEW_EPISODE = 1;

    public SeriesNotifier(@NonNull String id, @NonNull int condition) {
        this.mId = id;
        this.mCondition = condition;
    }

    public Notification createNewEpisodeNotification(Context context, String mediaTitle) {
        String contentTitle = context.getString(R.string.notification_new_episode_title);
        String contentText = context.getString(R.string.notification_new_episode_content_prefix)
                + " " + mediaTitle
                + context.getString(R.string.notification_new_episode_content_suffix);

        //create intent to launch activity on click
        Intent intent = new Intent(context, MasterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RELEASE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_rate)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        return builder.build();
    }
}