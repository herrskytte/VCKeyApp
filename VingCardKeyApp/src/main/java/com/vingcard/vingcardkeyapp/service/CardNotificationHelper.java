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
import com.vingcard.vingcardkeyapp.service.KeySyncService.KeySyncConstants;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.KeyCardDB;
import com.vingcard.vingcardkeyapp.ui.MainActivity;

public class CardNotificationHelper {

	private static int NOTIFICATION_ID = 333;

	public static void notifyNewKey(Context context, KeyCard newKeyCard){

		boolean isAppRunning = isActivityFound(context);

		if (isAppRunning) {
			broadcastInternalNewKey(context, newKeyCard);

		} else {
			showNewKeyNotification(context, newKeyCard);
		}
	}

	private static void broadcastInternalNewKey(Context context,
			KeyCard newKeyCard) {
		// Broadcasts the Intent for new key to receivers in this app.
		Intent localIntent = new Intent(KeySyncConstants.BROADCAST_ACTION);
		localIntent.addCategory(Intent.CATEGORY_DEFAULT);
		localIntent.putExtra(KeySyncConstants.DATA_NEW_KEY, newKeyCard.id);

		// Broadcasts the Intent to receivers in this app.
		LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
		Log.e("CardNotificationHelper", "Broadcast sent");
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

		if (services.get(0).topActivity.getPackageName().toString()
				.equalsIgnoreCase(context.getPackageName().toString())) {
			return true;
		}
		return false;
	}

	private static void showNewKeyNotification(Context context,	KeyCard newKeyCard) {
		Uri newKeyCardUri = KeyCardDB.buildKeyCardUri(newKeyCard.id);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setSmallIcon(R.drawable.ic_notification);
		mBuilder.setContentTitle("New key has arrived");
		mBuilder.setContentText(newKeyCard.label);
		mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
		mBuilder.setLargeIcon(bm);  

		// Creates an explicit intent for an Activity in your app
		Intent newCardIntent = new Intent(context, MainActivity.class);
		newCardIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		newCardIntent.setData(newKeyCardUri);


		PendingIntent newCardPendingIntent = 
				PendingIntent.getActivity(context, 0, newCardIntent, 0);

		mBuilder.setContentIntent(newCardPendingIntent);
		mBuilder.setAutoCancel(true);
		mBuilder.setLights(Color.BLUE, 500, 500);

		NotificationManager mNotificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Id allows you to update the notification later on.
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
		Log.e("CardNotificationHelper", "Notification sent");
	}
}
