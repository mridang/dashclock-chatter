package com.mridang.hangouts;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;

/*
 * This class is the main class that provides the widget
 */
@SuppressLint("SdCardPath")
public class HangoutsWidget extends DashClockExtension {

	/* This is the launch intent using for starting the Hangouts application */
	private Intent ittApplication;

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d("HangoutsWidget", "Created");
		BugSenseHandler.initAndStartSession(this, "3279a667");

		PackageManager pkgManager = getPackageManager();
		ittApplication = pkgManager.getLaunchIntentForPackage("com.google.android.talk");
		ittApplication.addCategory(Intent.CATEGORY_LAUNCHER);

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int arg0) {

		setUpdateWhenScreenOn(true);

		Log.d("HangoutsWidget", "Checking for unread Hangouts messages");
		final ExtensionData edtInformation = new ExtensionData();
		edtInformation.visible(false);

		Log.d("HangoutsWidget", "Checking if the device is rooted");	
		if (RootTools.isRootAvailable()) {

			Log.d("HangoutsWidget", "Checking if the conversation database exists");
			if (RootTools.exists("/data/data/com.google.android.talk/databases/babel1.db")) {

				Log.d("HangoutsWidget", "Checking if Sqlite is installed");
				if (RootTools.exists("/system/xbin/sqlite3") || RootTools.exists("/system/bin/sqlite3")) {

					Log.d("HangoutsWidget", "Reading unread messages from the databases");
					try {

						Command command = new Command(0, "cd  /data/data/com.google.android.talk/databases/", "sqlite3 babel1.db \"SELECT author_full_name FROM message_notifications_view WHERE notified_for_failure = 0 AND type = 2;\"") {

							@Override
							public void output(int id, String strLine) {

								try {

									edtInformation.status(Integer.toString((edtInformation.status() == null ? 0 : Integer.parseInt(edtInformation.status())) + 1)); 

									if (edtInformation.expandedBody() == null || !edtInformation.expandedBody().contains(strLine)) {
										edtInformation.expandedBody((edtInformation.expandedBody() == null ? "" : edtInformation.expandedBody() + "\n") + strLine);
									}

								} catch (Exception e) {
									BugSenseHandler.sendException(e);
								}

							}

						};
						RootTools.getShell(true).add(command).waitForFinish();

						Integer intMessages = Integer.parseInt(edtInformation.status() == null ? "0" : edtInformation.status());
						edtInformation.status(getResources().getQuantityString(R.plurals.message, intMessages, intMessages));
						edtInformation.visible(intMessages > 0);
						edtInformation.clickIntent(ittApplication);
						Log.d("HangoutsWidget", intMessages + " unread");

					} catch (InterruptedException e) {
						Log.w("HangoutsWidget", "Command execution interrupted", e);
					} catch (IOException e) {
						Log.w("HangoutsWidget", "Input output error", e);
					} catch (TimeoutException e) {
						Log.w("HangoutsWidget", "Command timed out", e);
					} catch (RootDeniedException e) {
						Log.w("HangoutsWidget", "Root access denied", e);
					} catch (Exception e) {
						Log.e("HangoutsWidget", "Encountered an error", e);
						BugSenseHandler.sendException(e);
					}

				} else {
					Log.w("HangoutsWidget", "Sqlite executable doesn't seem to be installed");
				}	

			} else {
				Log.d("HangoutsWidget", "Conversation database doesn't exist");
			}			

		} else {
			Log.d("HangoutsWidget", "The device is not rooted");
		}

		Log.d("HangoutsWidget", "Publishing update");
		edtInformation.icon(R.drawable.ic_dashclock);
		publishUpdate(edtInformation);
		Log.d("HangoutsWidget", "Done");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onDestroy()
	 */
	public void onDestroy() {

		super.onDestroy();
		Log.d("HangoutsWidget", "Destroyed");
		BugSenseHandler.closeSession(this);

	}

}