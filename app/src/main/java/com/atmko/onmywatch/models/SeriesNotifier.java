package com.atmko.onmywatch.models;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.fragments.DetailsFragment;

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

    public SeriesNotifier(@NonNull String id, int condition, boolean isActive) {
        this.mId = id;
        this.mCondition = condition;
        this.mIsActive = isActive;
    }

    public Notification createNewEpisodeNotification(Context context,
                                                     MediaData mediaData,
                                                     int source) {
        String contentTitle = context.getString(R.string.notification_new_episode_title);
        String contentText;
        if (source == ScheduledMedia.SOURCE_TRAKT) {
            contentText = context.getString(R.string.notification_new_episode_content_prefix)
                    + " " + mediaData.getTitle()
                    + " " + context.getString(R.string.notification_new_episode_specific_content_suffix);
        } else {
            contentText = context.getString(R.string.notification_new_episode_content_prefix)
                    + " " + mediaData.getTitle()
                    + " " + context.getString(R.string.notification_new_episode_general_content_suffix);
        }

        //create intent to launch activity on click
        Intent detailsIntent = new Intent(context, MasterActivity.class);
        detailsIntent.setAction(DetailsFragment.ACTION_LAUNCH_DETAILS);
        detailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        detailsIntent.putExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mediaData));

        Bundle detailsExtras = new Bundle();
        detailsIntent.putExtras(detailsExtras);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, getNotificationCode(),
                detailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return buildReleaseNotification(context, contentTitle, contentText, pendingIntent);
    }

    @Override
    public Notification createReleaseNotification(Context context, MediaData mediaData, int source) {
        String contentTitle = context.getString(R.string.notification_new_release_title);
        String contentText;
        if (source == ScheduledMedia.SOURCE_TRAKT) {
            contentText = mediaData.getTitle() + " "
                    + context.getString(R.string.notification_new_release_specific_content_suffix);

        } else {
            contentText = mediaData.getTitle() + " "
                    + context.getString(R.string.notification_new_release_content_general_suffix);
        }

        //create intent to launch activity on click
        Intent detailsIntent = new Intent(context, MasterActivity.class);
        detailsIntent.setAction(DetailsFragment.ACTION_LAUNCH_DETAILS);
        detailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        detailsIntent.putExtra(DetailsFragment.MEDIA_DATA_PARCELABLE_KEY, Parcels.wrap(mediaData));

        Bundle detailsExtras = new Bundle();
        detailsIntent.putExtras(detailsExtras);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, getNotificationCode(),
                detailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return buildReleaseNotification(context, contentTitle, contentText, pendingIntent);
    }

    public Map<String, Object> parseNotifierToDataMap() {
        Map<String, Object> seriesDataRecordMap = new HashMap<>();
        seriesDataRecordMap.put(NOTIFIER_ID_KEY, getMediaId());
        seriesDataRecordMap.put(CONDITION_KEY, getCondition());
        seriesDataRecordMap.put(IS_ACTIVE_KEY, getIsActive());

        return seriesDataRecordMap;
    }
}