package com.atmko.onmywatch.models;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import com.atmko.onmywatch.Fragments.DetailsFragment;
import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;

import org.parceler.Parcels;

import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "series_notifiers",
        primaryKeys = {"media_id", "condition"},
        foreignKeys =
                {@ForeignKey(entity = SeriesData.class, parentColumns = "id", childColumns = "media_id",
                        onDelete = ForeignKey.CASCADE)}
)

public class SeriesNotifier extends MediaNotifier {
    public static final int CONDITION_NEW_EPISODE = 1;

    public SeriesNotifier(@NonNull String id, @NonNull int condition, boolean isActive) {
        this.mId = id;
        this.mCondition = condition;
        this.mIsActive = isActive;
    }

    public Notification createNewEpisodeNotification(Context context,  MediaData mediaData) {
        String contentTitle = context.getString(R.string.notification_new_episode_title);
        String contentText = context.getString(R.string.notification_new_episode_content_prefix)
                + " " + mediaData.getTitle()
                + context.getString(R.string.notification_new_episode_content_suffix);

        //create intent to launch activity on click
        Intent detailsIntent = new Intent(context, MasterActivity.class);
        detailsIntent.setAction(DetailsFragment.ACTION_LAUNCH_DETAILS);
        detailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        detailsIntent.putExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mediaData));

        Bundle detailsExtras = new Bundle();
        detailsIntent.putExtras(detailsExtras);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, getNotificationCode(),
                detailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RELEASE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_rate)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        return builder.build();
    }

    public Map<String, Object> parseNotifierToDataMap() {
        //todo: refactor to series data map instead of movie
        Map<String, Object> movieDataRecordMap = new HashMap<>();
        movieDataRecordMap.put(NOTIFIER_ID_KEY, getMediaId());
        movieDataRecordMap.put(CONDITION_KEY, getCondition());
        movieDataRecordMap.put(IS_ACTIVE_KEY, getIsActive());

        return movieDataRecordMap;
    }
}