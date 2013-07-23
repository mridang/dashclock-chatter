package com.mridang.hangouts;

import java.io.File;
import java.util.ArrayList;

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
	/* This is the number of possible Hangouts accounts that the user has */
	private ArrayList<String> lstDatabases = new ArrayList<String>();

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onInitialize(boolean)
	 */
	@Override
	protected void onInitialize(boolean booReconnect) {

		setUpdateWhenScreenOn(true);

		if (lstDatabases.isEmpty()) {

			if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {

				Log.d("HangoutsWidget", "Checking if Hangouts is installed and getting the intent");
				try {

					PackageManager pkgManager = getPackageManager();
					ittApplication = pkgManager.getLaunchIntentForPackage("com.google.android.talk");
					ittApplication.addCategory(Intent.CATEGORY_LAUNCHER);

				} catch (Exception e) {
					Log.e("HangoutsWidget", "Error getting the launch intent for Hangouts", e);
					return;
				}

				try {

					Log.d("HangoutsWidget", "Checking and extracting libraries");
					new File(getApplicationContext().getFilesDir(), "libncurses.so").delete();
					RootTools.installBinary(getApplicationContext(), R.raw.libncurses, "libncurses.so", "755");
					Log.d("HangoutsWidget", "Installed libncurses");

					Log.d("HangoutsWidget", "Checking and extracting binaries");
					new File(getApplicationContext().getFilesDir(), "sqlite3").delete();
					RootTools.installBinary(getApplicationContext(), R.raw.sqlite3, "sqlite3", "755");
					Log.d("HangoutsWidget", "Installed sqlite3");

				} catch (Exception e) {
					Log.e("HangoutsWidget", "Error extracting libraries and binairies", e);
					return;
				}

				Log.d("HangoutsWidget", "Fetching all the Hangouts databases");
				try {

					for (Integer intNumber = 0; intNumber < 3; intNumber++) {

						if (RootTools.exists("/data/data/com.google.android.talk/databases/babel" + intNumber + ".db")) {

							lstDatabases.add("/data/data/com.google.android.talk/databases/babel" + intNumber + ".db");

						}

					}

					Log.d("HangoutsWidget", "Found " + lstDatabases.size() + (lstDatabases.size() > 1 ? " databases" : " database"));
					if (lstDatabases.size() == 0) {

						Log.e("HangoutsWidget", "Something is wrong. No databases found.");
						Toast.makeText(getApplicationContext(), R.string.database_missing, Toast.LENGTH_LONG).show();	

					}

				} catch (Exception e) {
					e.printStackTrace();
					return;
				}

			} else {

				Log.w("HangoutsWidget", "The device is not rooted or root access was denied");
				Toast.makeText(getApplicationContext(), R.string.unrooted_error, Toast.LENGTH_LONG).show();

			}

		}

		super.onInitialize(booReconnect);		

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d("HangoutsWidget", "Created");
		BugSenseHandler.initAndStartSession(this, "3279a667");

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int arg0) {

		Log.d("HangoutsWidget", "Checking for unread Hangouts messages");
		final ExtensionData edtInformation = new ExtensionData();
		edtInformation.visible(false);

		try {

			Log.d("HangoutsWidget", "Reading unread messages from the databases");
			for (String strDatabase : lstDatabases) {

				BugSenseHandler.clearCrashExtraData();

				Command cmdSystem = new Command(0, "sqlite3 " + strDatabase + " \"SELECT author_full_name, COUNT(*) FROM message_notifications_view WHERE notified_for_failure = 0 AND type = 2 GROUP BY author_full_name;\"") {

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

					Command cmdCustom = new Command(0, "/data/data/com.mridang.hangouts/files/sqlite3 " + strDatabase + " \"SELECT author_full_name FROM message_notifications_view WHERE notified_for_failure = 0 AND type = 2 GROUP BY author_full_name;\"") {

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

			}

			Integer intMessages = Integer.parseInt(edtInformation.status() == null ? "0" : edtInformation.status());
			edtInformation.status(getResources().getQuantityString(R.plurals.message, intMessages, intMessages));
			edtInformation.visible(intMessages > 0);
			edtInformation.clickIntent(ittApplication);
			Log.d("HangoutsWidget", (edtInformation.expandedBody() == null ? 0 : edtInformation.expandedBody().split("\n").length) + " unread");

		} catch (RootDeniedException e) {
			Log.w("HangoutsWidget", "Root access was denied to the extension");
			Toast.makeText(getApplicationContext(), R.string.unrooted_error, Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Log.e("HangoutsWidget", "Encountered an error", e);
			BugSenseHandler.sendException(e);
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