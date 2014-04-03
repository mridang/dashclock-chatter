package com.mridang.hangouts;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.Shell;

/*
 * This class is the main class that provides the widget
 */
@SuppressLint("SdCardPath")
public class HangoutsWidget extends DashClockExtension {

	/* This is the launch intent using for starting the application */
	private Intent ittApplication;
	/* The name of the package whose databases to read */
	private final static String PACKAGE_NAME = "com.google.android.talk";
	/* The name of the databases to read */
	private final static String[] DATABASE_NAMES = {"babel0.db", "babel1.db", "babel2.db"};

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onInitialize(boolean)
	 */
	@Override
	protected void onInitialize(boolean booReconnect) {

		if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {

			Log.d("HangoutsWidget", "Checking if application is installed and getting the intent");
			try {

				PackageManager pkgManager = getPackageManager();
				ittApplication = pkgManager.getLaunchIntentForPackage(PACKAGE_NAME);
				ittApplication.addCategory(Intent.CATEGORY_LAUNCHER);

			} catch (Exception e) {
				Log.e("HangoutsWidget", "Error getting the launch intent for application", e);
				return;
			}


		} else {
			Log.w("HangoutsWidget", "The device is not rooted or root access was denied");
			Toast.makeText(getApplicationContext(), R.string.unrooted_error, Toast.LENGTH_LONG).show();
		}

		Log.d("HangoutsWidget", "Initializing");
		super.onInitialize(booReconnect);
		setUpdateWhenScreenOn(true);

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d("HangoutsWidget", "Created");
		BugSenseHandler.initAndStartSession(this, getString(R.string.bugsense));

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int arg0) {

		Log.d("HangoutsWidget", "Checking for unread messages");
		final ExtensionData edtInformation = new ExtensionData();
		edtInformation.visible(false);

		try {

			BugSenseHandler.addCrashExtraData("BusyBox", RootTools.getBusyBoxVersion());
			for (String strDatabase : Arrays.asList(DATABASE_NAMES)) {

				File filApplication = new File("/data/data/" + PACKAGE_NAME + "/databases/" +  strDatabase);
				if (filApplication.exists() && (!filApplication.canRead() || !filApplication.canExecute())) {

					Log.w("HangoutsWidget", "Unable to access destination database");

					Shell shlTerm = null; 
					try {

						Log.d("HangoutsWidget", "Changing file permissions to grant access");
						shlTerm = RootTools.getShell(true);

						final String strContents = "chmod 777 /data/data/" + PACKAGE_NAME + "/databases/*";
						Log.d("HangoutsWidget", "Executing command " + strContents);
						final Command cmdContents = new Command(1, false, strContents) {

							@Override
							public void commandTerminated(int arg0, String arg1) {
								Log.w("HangoutsWidget", "Terminated ");
								this.notify();
							}

							@Override
							public void commandOutput(int arg0, String arg1) {
								BugSenseHandler.addCrashExtraData(String.valueOf(arg0), arg1);
								Log.v("HangoutsWidget", arg1);
							}

							@Override
							public void commandCompleted(int arg0, int arg1) {
								Log.d("HangoutsWidget", "Completed with return code " + arg1);
								this.setExitCode(arg1);
								this.notify();
							}

						};

						RootTools.runShellCommand(shlTerm, cmdContents);
						synchronized (cmdContents) {
							cmdContents.wait();
						}
						if (cmdContents.getExitCode() != 0) {

							Log.e("HangoutsWidget", "Error executing command " + cmdContents.getCommand());
							throw new Exception("Error executing command " + cmdContents.getCommand());

						}

						final String strDatabases = "chmod 775 /data/data/" + PACKAGE_NAME + "/databases";
						Log.d("HangoutsWidget", "Executing command " + strDatabases);
						final Command cmdDatabases = new Command(2, false, strDatabases) {

							@Override
							public void commandTerminated(int arg0, String arg1) {
								Log.w("HangoutsWidget", "Terminated ");
								this.notify();
							}

							@Override
							public void commandOutput(int arg0, String arg1) {
								BugSenseHandler.addCrashExtraData(String.valueOf(arg0), arg1);
								Log.v("HangoutsWidget", arg1);
							}

							@Override
							public void commandCompleted(int arg0, int arg1) {
								Log.d("HangoutsWidget", "Completed with return code " + arg1);
								this.setExitCode(arg1);
								this.notify();
							}

						};

						RootTools.runShellCommand(shlTerm, cmdDatabases);
						synchronized (cmdDatabases) {
							cmdDatabases.wait();
						}
						if (cmdDatabases.getExitCode() != 0) {

							Log.e("HangoutsWidget", "Error executing command " + cmdDatabases.getCommand());
							throw new Exception("Error executing command " + cmdDatabases.getCommand());

						}

						final String strApplication = "chmod 755 /data/data/" + PACKAGE_NAME;
						Log.d("HangoutsWidget", "Executing command " + strApplication);
						final Command cmdApplication = new Command(3, false, strApplication) {

							@Override
							public void commandTerminated(int arg0, String arg1) {
								Log.w("HangoutsWidget", "Terminated ");
								this.notify();
							}

							@Override
							public void commandOutput(int arg0, String arg1) {
								BugSenseHandler.addCrashExtraData(String.valueOf(arg0), arg1);
								Log.v("HangoutsWidget", arg1);
							}

							@Override
							public void commandCompleted(int arg0, int arg1) {
								Log.d("HangoutsWidget", "Completed with return code " + arg1);
								this.setExitCode(arg1);
								this.notify();
							}

						};

						RootTools.runShellCommand(shlTerm, cmdApplication);
						synchronized (cmdApplication) {
							cmdApplication.wait();
						}
						if (cmdApplication.getExitCode() != 0) {

							Log.e("HangoutsWidget", "Error executing command " + cmdApplication.getCommand());
							throw new Exception("Error executing command " + cmdApplication.getCommand());

						}

						BugSenseHandler.clearCrashExtraData();

					} catch (IOException e) {
						Log.e("HangoutsWidget", "Error running commands or getting shell access", e);
						BugSenseHandler.sendException(e);
						return;
					} catch (TimeoutException e) {
						Log.e("HangoutsWidget", "Timeout getting shell acess", e);
						BugSenseHandler.sendException(e);
						return;
					} catch (RootDeniedException e) {
						Log.e("HangoutsWidget", "Root access was denied", e);
						BugSenseHandler.sendException(e);
						return;
					} catch (Exception e) {
						Log.e("HangoutsWidget", "An unknown error occurred", e);
						BugSenseHandler.sendException(e);
						return;
					} finally {

						try {

							if (shlTerm != null) {
								shlTerm.close();
							}

						} catch (IOException e) {
							Log.w("HangoutsWidget", "Error closing shell", e);
							BugSenseHandler.sendException(e);
							return;
						}

					}

					if (!filApplication.canRead() || !filApplication.canExecute()) {
						Log.w("HangoutsWidget", "Still unable to access destination directory");
						throw new RuntimeException("Still unable to access destination directory");
					}

				}

			}			

			PackageInfo pkgInformation = getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
			BugSenseHandler.addCrashExtraData(PACKAGE_NAME, pkgInformation.versionName);
			Integer intRowcount = 0;
			StringBuilder sbdContacts = new StringBuilder();

			for (String strDatabase : Arrays.asList(DATABASE_NAMES)) {

				File filDatabase = new File("/data/data/" + PACKAGE_NAME + "/databases/" + strDatabase);
				if (filDatabase.exists()) {

					SQLiteDatabase sqlDatabase = SQLiteDatabase.openDatabase(filDatabase.getPath(), null, SQLiteDatabase.OPEN_READONLY);
					try {

						Cursor curMessages = null;

						try {

							curMessages = sqlDatabase.rawQuery("SELECT author_full_name, COUNT(*) FROM message_notifications_view WHERE notified_for_failure = 0 AND type = 2 GROUP BY author_full_name;\"", null);

							if (curMessages.moveToFirst()) {

								Integer intColumn = curMessages.getColumnIndex("author_full_name");
								intRowcount += curMessages.getCount();

								do {

									String strName = curMessages.getString(intColumn);
									sbdContacts.append(sbdContacts.length() == 0 ? "" : "\n");
									sbdContacts.append(strName);									
									Log.v("HangoutsWidget", "Unread messages from " + strName);

								} while(curMessages.moveToNext());

							}

						} finally {
							if (curMessages != null) {
								curMessages.close();
							}
						}

					} catch (SQLiteCantOpenDatabaseException e) {
						if (e.getMessage().contains("code 14")) {
							Log.v("HangoutsWidget", e.getMessage());
						} else {
							throw e;
						}
					} finally {
						sqlDatabase.close();
					}

				}

			}

			edtInformation.status(intRowcount.toString());
			edtInformation.expandedTitle(getResources().getQuantityString(R.plurals.message, intRowcount, intRowcount));
			edtInformation.expandedBody(sbdContacts.toString());
			edtInformation.visible(intRowcount > 0);
			edtInformation.clickIntent(ittApplication);
			Log.d("HangoutsWidget", intRowcount + " unread messages");

			if (new Random().nextInt(5) == 0) {

				PackageManager mgrPackages = getApplicationContext().getPackageManager();

				try {

					mgrPackages.getPackageInfo("com.mridang.donate", PackageManager.GET_META_DATA);

				} catch (NameNotFoundException e) {

					Integer intExtensions = 0;
					Intent ittFilter = new Intent("com.google.android.apps.dashclock.Extension");
					String strPackage;

					for (ResolveInfo info : mgrPackages.queryIntentServices(ittFilter, 0)) {

						strPackage = info.serviceInfo.applicationInfo.packageName;
						intExtensions = intExtensions + (strPackage.startsWith("com.mridang.") ? 1 : 0); 

					}

					if (intExtensions > 1) {

						edtInformation.visible(true);
						edtInformation.clickIntent(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=com.mridang.donate")));
						edtInformation.expandedTitle("Please consider a one time purchase to unlock.");
						edtInformation.expandedBody("Thank you for using " + intExtensions + " extensions of mine. Click this to make a one-time purchase or use just one extension to make this disappear.");
						setUpdateWhenScreenOn(true);

					}

				}

			} else {
				setUpdateWhenScreenOn(true);
			}

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e("HangoutsWidget", "Encountered an error", e);
			BugSenseHandler.sendException(e);
		}

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