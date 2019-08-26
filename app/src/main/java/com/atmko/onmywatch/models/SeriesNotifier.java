package com.atmko.onmywatch.models;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity(tableName = "series_notifiers")
public class SeriesNotifier extends MediaNotifier{
    @Ignore private Context mContext;

    @Ignore
    public SeriesNotifier(Context context, @NonNull String id) {
        this.mContext = context;
        this.mId = id;

        this.mConditionTitles = new ArrayList<>();
        List<String> unconvertedTitles = Arrays.asList(mContext.getResources()
                .getStringArray(R.array.notifier_series_condition_titles));
        mConditionTitles.addAll(unconvertedTitles);

        SparseBooleanArray conditionValues = new SparseBooleanArray();
        for (int index = 0; index < mConditionTitles.size(); index++) {
            conditionValues.put(index, false);
        }

        this.mConditionValues = conditionValues;
    }

    public SeriesNotifier(@NonNull String id) {
        this.mId = id;
    }

    @Override
    public void prepareNotification(Context context, MediaData oldMediaData, MediaData newMediaData,
                       int conditionKey) {
        String contentTitle;
        String contentText;

        if (conditionKey == MediaNotifier.CONDITION_ON_RELEASE_KEY) {
            //if new and old release status don't match
            if (!newMediaData.getReleaseStatus().equals(oldMediaData.getReleaseStatus())) {
                //if the new  release status is "released"
                if (newMediaData.getReleaseStatus().equals(MediaNotifier.RELEASED)) {
                    contentTitle = context.getString(R.string.notification_new_release_title);
                    contentText = newMediaData.getTitle()
                            + context.getString(R.string.notification_new_release_content_suffix);

                    notify(context, contentTitle, contentText);

                }
            }

        } else if (conditionKey == MediaNotifier.CONDITION_NEW_TRAILER_KEY) {
//            contentTitle = context.getString(R.string.notification_new_trailer_title);
//            contentText = newMediaData.getTitle()
//                    + context.getString(R.string.notification_new_trailer_content_suffix);

        }
    }

    @Override
    public void notify(Context context, String notificationTitle, String notificationContext) {
        //create intent to launch activity on click
        Intent intent = new Intent(mContext, MasterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, RELEASE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_rate)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContext)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        //and unique id and show notification
        notificationManager.notify(createID(), builder.build());
    }
}
