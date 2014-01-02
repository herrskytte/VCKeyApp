package com.vingcard.vingcardkeyapp.service;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.vingcard.vingcardkeyapp.R;
import com.vingcard.vingcardkeyapp.model.KeyCard;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.KeyCardDB;
import com.vingcard.vingcardkeyapp.ui.MainActivity;
import com.vingcard.vingcardkeyapp.util.AppConstants;

public class CardNotificationHelper {

	private static final int NOTIFICATION_ID = 333;

	public static void notifyKeyUpdate(Context context, KeyCard keyCard, String action){

		boolean isAppRunning = isActivityFound(context);

		if (isAppRunning) {
			broadcastInternalKeyUpdate(context, keyCard, action);

		} else {
			showKeyUpdateNotification(context, keyCard, action);
		}
	}

	private static void broadcastInternalKeyUpdate(Context context, KeyCard keyCard, String action) {
		// Broadcasts the Intent for new key to receivers in this app.
		Intent localIntent = new Intent(AppConstants.KeySync.BROADCAST_ACTION);
		localIntent.addCategory(Intent.CATEGORY_DEFAULT);
		localIntent.putExtra(AppConstants.KeySync.DATA_KEY, keyCard.id);
        localIntent.putExtra(AppConstants.KeySync.DATA_ACTION, action);

		// Broadcasts the Intent to receivers in this app.
		LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
		Log.e("CardNotificationHelper", "Broadcast sent");
	}

	private static void showKeyUpdateNotification(Context context, KeyCard keyCard, String action) {
		Uri keyCardUri = KeyCardDB.buildKeyCardUri(keyCard.id);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        switch (action) {
            case AppConstants.KeySync.ACTION_NEW_KEY:
                mBuilder.setContentText(context.getText(R.string.key_notification_new));
                break;
            case AppConstants.KeySync.ACTION_UPDATED_KEY:
                mBuilder.setContentText(context.getText(R.string.key_notification_update));
                break;
            case AppConstants.KeySync.ACTION_REVOKED_KEY:
                mBuilder.setContentText(context.getText(R.string.key_notification_revoke));
                break;
        }
        mBuilder.setSmallIcon(R.drawable.ic_notification);
        mBuilder.setContentTitle(context.getText(R.string.key_notification_header));
		mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
		mBuilder.setLargeIcon(bm);  

		// Creates an explicit intent for an Activity in your app
		Intent cardIntent = new Intent(context, MainActivity.class);
		cardIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		cardIntent.setData(keyCardUri);
        cardIntent.putExtra(AppConstants.KeySync.DATA_ACTION, action);


		PendingIntent cardPendingIntent =
				PendingIntent.getActivity(context, 0, cardIntent, 0);

		mBuilder.setContentIntent(cardPendingIntent);
		mBuilder.setAutoCancel(true);
		mBuilder.setLights(Color.BLUE, 500, 500);

		NotificationManager mNotificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Id allows you to update the notification later on.
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
		Log.e("CardNotificationHelper", "Notification sent");
	}


    /**
     * Using this approach finds out if the application is currently running.
     * However it does not know if an appropriate broadcast receiver is active.
     * Currently a receiver is only implemented in CardsOverviewFragment.
     * Need a different method if more screens are added to the application!
     */
    private static boolean isActivityFound(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> services = activityManager.getRunningTasks(1);
        return services.get(0).topActivity.getPackageName().equalsIgnoreCase(context.getPackageName());
    }
}
