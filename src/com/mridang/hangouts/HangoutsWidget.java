package com.mridang.hangouts;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

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

		try {

			PackageManager pkgManager = getPackageManager();
			ittApplication = pkgManager.getLaunchIntentForPackage("com.google.android.talk");
			ittApplication.addCategory(Intent.CATEGORY_LAUNCHER);

		} catch (Exception e) {
			BugSenseHandler.sendException(e);
		}

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

		Log.d("HangoutsWidget", "Checking if the device is rooted and access has been granted");	
		if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {

			try {

				Log.d("HangoutsWidget", "Checking if necessary libraries are installed");
				if (!RootTools.exists("/system/lib/libncurses.so")) {

					Log.d("HangoutsWidget", "Installing the libraries");
					RootTools.installBinary(getApplicationContext(), R.raw.libncurses, "libncurses.so", "644");
					RootTools.copyFile("/data/data/com.mridang.hangouts/files/libncurses.so", "/system/lib/libncurses.so", true, true);
					Log.d("HangoutsWidget", "Installed");

				}

				Log.d("HangoutsWidget", "Checking if the contacts database exists");
				if (RootTools.exists("/data/data/com.google.android.talk/databases/babel1.db")) {

					BugSenseHandler.clearCrashExtraData();

					Log.d("HangoutsWidget", "Reading unread messages from the databases");
					Command cmdSystem = new Command(0, "sqlite3 /data/data/com.google.android.talk/databases/babel1.db \"SELECT author_full_name, COUNT(*) FROM message_notifications_view WHERE notified_for_failure = 0 AND type = 2;\"") {

						@Override
						public void output(int id, String strLine) {

							Log.v("HangoutsWidget", strLine);
							try {

								BugSenseHandler.addCrashExtraData("System " + Integer.toString(BugSenseHandler.getCrashExtraData().size()), strLine);

								if (!strLine.trim().isEmpty()) {

									edtInformation.status(Integer.toString((edtInformation.status() == null ? 0 : Integer.parseInt(edtInformation.status())) + Integer.parseInt(strLine.split("\\|")[1]))); 

									if (edtInformation.expandedBody() == null || !edtInformation.expandedBody().contains(strLine)) {
										edtInformation.expandedBody((edtInformation.expandedBody() == null ? "" : edtInformation.expandedBody() + "\n") + strLine.split("\\|")[0]);
									}

								}

							} catch (Exception e) {
								setExitCode(-1);
							}

						}

					};
					RootTools.getShell(true).add(cmdSystem).waitForFinish();

					if (cmdSystem.exitCode() == -1) {

						Command cmdCustom = new Command(0, "/data/data/com.mridang.hangouts/files/sqlite3 /data/data/com.google.android.talk/databases/babel1.db \"SELECT author_full_name FROM message_notifications_view WHERE notified_for_failure = 0 AND type = 2;\"") {

							@Override
							public void output(int id, String strLine) {

								Log.v("HangoutsWidget", strLine);
								try {

									BugSenseHandler.addCrashExtraData("Custom " + Integer.toString(BugSenseHandler.getCrashExtraData().size()), strLine);

									if (!strLine.trim().isEmpty()) {

										edtInformation.status(Integer.toString((edtInformation.status() == null ? 0 : Integer.parseInt(edtInformation.status())) + Integer.parseInt(strLine.split("\\|")[1]))); 

										if (edtInformation.expandedBody() == null || !edtInformation.expandedBody().contains(strLine)) {
											edtInformation.expandedBody((edtInformation.expandedBody() == null ? "" : edtInformation.expandedBody() + "\n") + strLine.split("\\|")[0]);
										}

									}

								} catch (Exception e) {
									setExitCode(-1);
								}

							}

						};
						RootTools.getShell(true).add(cmdCustom).waitForFinish();

						if (cmdSystem.exitCode() == -1) {

							Command cmdInstalled = new Command(1, "sqlite3 -version") {

								@Override
								public void output(int arg0, String strLine) {
									BugSenseHandler.addCrashExtraData("Installed", strLine);							
								}

							};
							RootTools.getShell(true).add(cmdInstalled).waitForFinish();

							RootTools.installBinary(getApplicationContext(), R.raw.sqlite3, "sqlite3", "755");

							Command cmdPackaged = new Command(1, "/data/data/com.mridang.hangouts/files/sqlite3 -version") {

								@Override
								public void output(int arg0, String strLine) {
									BugSenseHandler.addCrashExtraData("Packaged", strLine);							
								}

							};
							RootTools.getShell(true).add(cmdPackaged).waitForFinish();

							BugSenseHandler.sendException(new Exception("Error Parsing response"));
							return;

						}

					}
					BugSenseHandler.clearCrashExtraData();

					Integer intMessages = Integer.parseInt(edtInformation.status() == null ? "0" : edtInformation.status());
					edtInformation.status(getResources().getQuantityString(R.plurals.message, intMessages, intMessages));
					edtInformation.visible(intMessages > 0);
					if (ittApplication != null)
						edtInformation.clickIntent(ittApplication);
					Log.d("HangoutsWidget", (edtInformation.expandedBody() == null ? 0 : edtInformation.expandedBody().split("\n").length) + " unread");		

				} else {
					Log.w("HangoutsWidget", "Contacts database doesn't exist");
					Toast.makeText(getApplicationContext(), R.string.database_missing, Toast.LENGTH_LONG).show();
				}	

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
			Log.w("HangoutsWidget", "The device is not rooted");
			Toast.makeText(getApplicationContext(), R.string.unrooted_error, Toast.LENGTH_LONG).show();
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