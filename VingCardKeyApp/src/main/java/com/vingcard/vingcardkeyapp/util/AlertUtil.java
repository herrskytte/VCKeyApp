package com.vingcard.vingcardkeyapp.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;


public class AlertUtil {

	public static AlertDialog showErrorMessage(Context context, String title, String message) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				context);

		alertDialogBuilder.setTitle(title);

		alertDialogBuilder.setMessage(message)
				.setCancelable(false)
				.setPositiveButton("OK", new OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// lukker vinduet
						dialog.dismiss();						
					}
				});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
		return alertDialog;
	}
	
	public static AlertDialog showQuestionMessage(Context context, 
												  String title, 
												  String message,
												  String positiveText,
												  String negativeText,
												  OnClickListener positiveListener,
												  OnClickListener negativeListener) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				context);

		alertDialogBuilder.setTitle(title);
		alertDialogBuilder.setMessage(message);
		alertDialogBuilder.setCancelable(false);
		alertDialogBuilder.setPositiveButton(
				positiveText != null ? positiveText : "OK", 
				positiveListener != null ? positiveListener : 
										   new OnClickListener() {
												public void onClick(DialogInterface dialog, int id) {
													// lukker vinduet
													dialog.dismiss();						
												}
											});
		alertDialogBuilder.setNegativeButton(
				negativeText != null ? negativeText : "Cancel", 
				negativeListener != null ? negativeListener : 
										   new OnClickListener() {
												public void onClick(DialogInterface dialog, int id) {
													// lukker vinduet
													dialog.dismiss();						
												}
											});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
		return alertDialog;
	}
}
