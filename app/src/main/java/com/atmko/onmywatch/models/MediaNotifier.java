package com.atmko.onmywatch.models;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
import com.google.android.gms.common.util.Strings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class MediaNotifier {
    //notification channel ids
    public static final String RELEASE_CHANNEL_ID = "Release Channel";
    public static final String VIDEO_CHANNEL_ID = "Videos Channel";

    //notifier condition keys must correspond to order defined in arrays resource file
    public static final int CONDITION_ON_RELEASE_KEY = 0;
    public static final int CONDITION_NEW_TRAILER_KEY = 1;

    public static final String RELEASED = "Released";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id") public String mId;
    @ColumnInfo(name = "condition_titles") public ArrayList<String> mConditionTitles;
    @ColumnInfo(name = "condition_values") public SparseBooleanArray mConditionValues;

    public abstract void prepareNotification(Context context, MediaData oldMediaData, MediaData newMediaData,
                                int conditionKey);

    public abstract void notify(Context context, String notificationTitle,
                                String notificationContext);

    //TODO consider using a LinkedHashSet seeing as titles are used as though they are unique
    public List<String> getConditionTitles() {
        return mConditionTitles;
    };

    public SparseBooleanArray getConditionValues() {
        return mConditionValues;
    };

    public void setConditionValue(int conditionKey, boolean conditionValue) {
        mConditionValues.put(conditionKey, conditionValue);
    }

    //source:https://stackoverflow.com/questions/25713157/generate-int-unique-id-as-android-notification-id
    //user:Adetunji Mohammed
    //date:Aug 29 '16
    public int createID(){
        Date now = new Date();
        return Integer.parseInt(new SimpleDateFormat("ddHHmmss",  Locale.US).format(now));
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

    public static void createVideoNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.notification_channel_videos);
            String description = context.getString(R.string.notification_channel_videos_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(RELEASE_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    //returns true if any condition value is true
    public boolean notificationItemsExist() {
        for (int index = 0; index < mConditionValues.size(); index++) {
            if (mConditionValues.valueAt(index)) {
                return true;
            }
        }

        return false;
    }
}
