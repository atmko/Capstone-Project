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

@Entity(tableName = "movie_notifiers",
        primaryKeys = {"media_id", "condition"},
        foreignKeys =
                {@ForeignKey(entity = MovieData.class, parentColumns = "id", childColumns = "media_id",
                        onDelete = ForeignKey.CASCADE)}
)

public class MovieNotifier extends MediaNotifier {
    public MovieNotifier(@NonNull String id, int condition, boolean isActive) {
        this.mId = id;
        this.mCondition = condition;
        this.mIsActive = isActive;
    }

    @Override
    public Notification createReleaseNotification(Context context, MediaData mediaData, int source) {
        String contentTitle = context.getString(R.string.notification_new_release_title);
        String contentText = String.format(
                context.getString(R.string.notification_new_release_content_general),
                mediaData.getTitle());

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
        Map<String, Object> movieDataRecordMap = new HashMap<>();
        movieDataRecordMap.put(NOTIFIER_ID_KEY, getMediaId());
        movieDataRecordMap.put(CONDITION_KEY, getCondition());
        movieDataRecordMap.put(IS_ACTIVE_KEY, getIsActive());

        return movieDataRecordMap;
    }
}