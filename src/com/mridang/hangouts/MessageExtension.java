package com.mridang.hangouts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

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
public abstract class MessageExtension extends DashClockExtension {

	/* The launch intent of the application for the click event */
	protected Intent ittApplication = new Intent();
	/* The list of file object pointing to the databases */
	protected List<File> filApplications = new ArrayList<File>();
	/* The chmod command instance for changing the permissions */
	private Command cmdChmod;

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onInitialize(boolean)
	 */
	@Override
	protected void onInitialize(boolean booReconnect) {

		if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {

			Log.d(this.getTag(), "Checking if application is installed and getting the intent");
			try {

				ittApplication = getPackageManager().getLaunchIntentForPackage(getPackage());
				ittApplication.addCategory(Intent.CATEGORY_LAUNCHER);

			} catch (Exception e) {
				Log.e(this.getTag(), "Error getting the launch intent for application", e);
				return;
			}

			for (String strDatabase : getDb()) {
				filApplications.add(new File("/data/data/" + getPackage() + "/databases/" +  strDatabase));
			}
			String strCommand = "chmod 777 /data/data/" + getPackage() + "/databases/* ; ";
			strCommand = strCommand + "chmod 775 /data/data/" + getPackage() + "/databases ; ";
			strCommand = strCommand + "chmod 755 /data/data/" + getPackage();
			cmdChmod = new Command(1, false, strCommand) {

				@Override
				public void commandTerminated(int arg0, String arg1) {
					Log.w(MessageExtension.this.getTag(), "Terminated ");
					this.notify();
				}

				@Override
				public void commandOutput(int arg0, String arg1) {
					ACRA.getErrorReporter().putCustomData(String.valueOf(arg0), arg1);
					Log.v(MessageExtension.this.getTag(), arg1);
				}

				@Override
				public void commandCompleted(int arg0, int arg1) {
					Log.d(MessageExtension.this.getTag(), "Completed with return code " + arg1);
					this.setExitCode(arg1);
					this.notify();
				}

			};

		} else {
			Log.w(this.getTag(), "The device is not rooted or root access was denied");
			Toast.makeText(getApplicationContext(), R.string.unrooted_error, Toast.LENGTH_LONG).show();
		}

		Log.d(this.getTag(), "Initializing");
		super.onInitialize(booReconnect);
		setUpdateWhenScreenOn(true);

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d(this.getTag(), "Created");
		ACRA.init(new AcraApplication(getApplicationContext()));

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int intReason) {

		Log.d(this.getTag(), "Checking for unread messages");
		final ExtensionData edtInformation = new ExtensionData();
		edtInformation.visible(false);

		try {

			Log.w(this.getTag(), "Unable to access destination database");
			Shell shlTerm = null; 
			try {

				Log.d(this.getTag(), "Changing file permissions to grant access");
				Log.d(this.getTag(), "Executing command " + cmdChmod.getCommand());

				shlTerm = RootTools.getShell(true);
				RootTools.runShellCommand(shlTerm, cmdChmod);
				synchronized (cmdChmod) {
					cmdChmod.wait();
				}

				if (cmdChmod.getExitCode() != 0) {
					Log.e(this.getTag(), "Error executing command " + cmdChmod.getCommand());
					throw new Exception("Error executing command " + cmdChmod.getCommand());
				}

			} catch (IOException e) {
				Log.e(this.getTag(), "Error running commands or getting shell access", e);
				ACRA.getErrorReporter().handleSilentException(e);
				return;
			} catch (TimeoutException e) {
				Log.e(this.getTag(), "Timeout getting shell acess", e);
				ACRA.getErrorReporter().handleSilentException(e);
				return;
			} catch (RootDeniedException e) {
				Log.e(this.getTag(), "Root access was denied", e);
				ACRA.getErrorReporter().handleSilentException(e);
				return;
			} catch (Exception e) {
				Log.e(this.getTag(), "An unknown error occurred", e);
				ACRA.getErrorReporter().handleSilentException(e);
				return;
			} finally {

				try {

					shlTerm.close();

				} catch (IOException e) {
					Log.w(this.getTag(), "Error closing shell", e);
					ACRA.getErrorReporter().handleSilentException(e);
					return;
				}

			}

			Integer intRowcount = 0;
			Set<String> strContacts = new HashSet<String>();
			for (File filApplication : filApplications) {

				if (!filApplication.exists() || !filApplication.canRead() || !filApplication.canExecute()) {

					Log.w(this.getTag(), "Database doesn't exist or unable to read database");
					continue;

				}

				SQLiteDatabase sqlDatabase = SQLiteDatabase.openDatabase(filApplication.getPath(), null, SQLiteDatabase.OPEN_READONLY);
				try {

					Cursor curMessages = sqlDatabase.rawQuery(getQuery(), null);
					try {

						if (curMessages.moveToFirst()) {

							intRowcount += curMessages.getCount();
							do {
								strContacts.add(curMessages.getString(0));
							} while(curMessages.moveToNext());

						}

					} finally {
						curMessages.close();
					}

				} finally {
					sqlDatabase.close();
				}

			}

			if (intRowcount > 0) {

				StringBuilder sbdContacts = new StringBuilder(256);
				for (Iterator<String> itrContacts = strContacts.iterator(); itrContacts.hasNext();) {
					String strContact = itrContacts.next();
					sbdContacts.append(strContact + (itrContacts.hasNext() ? ", " : ""));
				}

				edtInformation.status(intRowcount.toString());
				edtInformation.expandedTitle(getResources().getQuantityString(R.plurals.message, intRowcount, intRowcount, getName()));
				edtInformation.expandedBody(sbdContacts.toString());
				edtInformation.visible(true);
				edtInformation.clickIntent(ittApplication);

				Log.v(this.getTag(), intRowcount + " unread messages from " + sbdContacts.toString());

			} else {

				edtInformation.visible(false);
				Log.d(this.getTag(), "No unread messages");

			}

			if (new Random().nextInt(5) == 0 && !(0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))) {

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
						edtInformation.clickIntent(new Intent(Intent.ACTION_VIEW).setData(Uri
								.parse("market://details?id=com.mridang.donate")));
						edtInformation.expandedTitle("Please consider a one time purchase to unlock.");
						edtInformation
						.expandedBody("Thank you for using "
								+ intExtensions
								+ " extensions of mine. Click this to make a one-time purchase or use just one extension to make this disappear.");
						setUpdateWhenScreenOn(true);

					}

				}

			}

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e(this.getTag(), "Encountered an error", e);
			ACRA.getErrorReporter().handleSilentException(e);
		}

		edtInformation.icon(getIcon());
		publishUpdate(edtInformation);
		Log.d(this.getTag(), "Done");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onDestroy()
	 */
	public void onDestroy() {

		super.onDestroy();
		Log.d(this.getTag(), "Destroyed");

	}

	/**
	 * Get the name of the class that extends this extension
	 * @return the name of the class that extends this extension
	 */
	protected abstract String getTag();

	/**
	 * Get the name of the application that contains the messages
	 * @return the name of the application that contains the messages
	 */
	protected abstract String getName();

	/**
	 * Get the name of the package that contains the database
	 * @return the name of the package that contains the database
	 */
	protected abstract String getPackage();

	/**
	 * Get the names of the databases that contains the messages
	 * @return the names of the databases that contains the message
	 */
	protected abstract List<String> getDb();

	/**
	 * Get the drawable resource that is used for the extension
	 * @return the drawable resource that is used for the extension
	 */
	protected abstract Integer getIcon();

	/**
	 * Get the SQL of the query that will return the messages
	 * @return the SQL of the query that will return the messages
	 */
	protected abstract String getQuery();

}