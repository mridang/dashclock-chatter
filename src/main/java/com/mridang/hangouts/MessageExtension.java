package com.mridang.hangouts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.dashclock.api.ExtensionData;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.Shell;

import org.acra.ACRA;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/*
 * This class is the main class that provides the widget
 */
@SuppressLint("SdCardPath")
public abstract class MessageExtension extends ImprovedExtension {

	/**
	 * The launch intent of the application for the click event
	 */
	private Intent ittApplication = new Intent();
	/**
	 * The list of file object pointing to the databases
	 */
	private final List<File> filApplications = new ArrayList<>();
	/**
	 * The chmod command instance for changing the permissions
	 */
	private Command cmdChmod;

	/*
	* (non-Javadoc)
	* @see com.mridang.cellinfo.ImprovedExtension#getIntents()
	*/
	@Override
	protected IntentFilter getIntents() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.cellinfo.ImprovedExtension#getTag()
	 */
	@Override
	protected String getTag() {
		return getClass().getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.cellinfo.ImprovedExtension#getUris()
	 */
	@Override
	protected String[] getUris() {
		return null;
	}

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
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@SuppressWarnings("SynchronizeOnNonFinalField")
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
				Log.e(this.getTag(), "Timeout getting shell access", e);
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

					if (shlTerm != null) {
						shlTerm.close();
					}

				} catch (IOException e) {
					Log.w(this.getTag(), "Error closing shell", e);
					ACRA.getErrorReporter().handleSilentException(e);
					//noinspection ReturnInsideFinallyBlock
					return;
				}

			}

			Integer intRowcount = 0;
			Set<String> strContacts = new HashSet<>();
			for (File filApplication : filApplications) {

				if (!filApplication.exists() || !filApplication.canRead() || !filApplication.canExecute()) {

					Log.w(this.getTag(), "Database doesn't exist or unable to read database");
					continue;

				}

				SQLiteDatabase sqlDatabase = SQLiteDatabase.openDatabase(filApplication.getPath(), null, SQLiteDatabase.OPEN_READONLY);
				//noinspection TryFinallyCanBeTryWithResources
				try {

					Cursor curMessages = sqlDatabase.rawQuery(getQuery(), null);
					//noinspection TryFinallyCanBeTryWithResources
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
					sbdContacts.append(strContact).append(itrContacts.hasNext() ? ", " : "");
				}

				edtInformation.status(intRowcount.toString());
				edtInformation.expandedTitle(getApplicationContext().getResources().getQuantityString(R.plurals.message, intRowcount, intRowcount, getName()));
				edtInformation.expandedBody(sbdContacts.toString());
				edtInformation.visible(true);
				edtInformation.clickIntent(ittApplication);

				Log.v(this.getTag(), intRowcount + " unread messages from " + sbdContacts.toString());

			} else {

				edtInformation.visible(false);
				Log.d(this.getTag(), "No unread messages");

			}

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e(this.getTag(), "Encountered an error", e);
			ACRA.getErrorReporter().handleSilentException(e);
		}

		edtInformation.icon(getIcon());
		doUpdate(edtInformation);

	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.cellinfo.ImprovedExtension#onReceiveIntent(android.content.Context, android.content.Intent)
	 */
	@Override
	protected void onReceiveIntent(Context ctxContext, Intent ittIntent) {
		onUpdateData(UPDATE_REASON_MANUAL);
	}

	/**
	 * Get the name of the application that contains the messages
	 *
	 * @return the name of the application that contains the messages
	 */
	protected abstract String getName();

	/**
	 * Get the name of the package that contains the database
	 *
	 * @return the name of the package that contains the database
	 */
	protected abstract String getPackage();

	/**
	 * Get the names of the databases that contains the messages
	 *
	 * @return the names of the databases that contains the message
	 */
	protected abstract List<String> getDb();

	/**
	 * Get the drawable resource that is used for the extension
	 *
	 * @return the drawable resource that is used for the extension
	 */
	protected abstract Integer getIcon();

	/**
	 * Get the SQL of the query that will return the messages
	 *
	 * @return the SQL of the query that will return the messages
	 */
	protected abstract String getQuery();

}