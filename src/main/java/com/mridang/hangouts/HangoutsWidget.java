package com.mridang.hangouts;

import java.util.Arrays;
import java.util.List;


/**
 * This class is the main class that provides the widget
 */
public class HangoutsWidget extends MessageExtension {

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getTag()
	 */
	@Override
	protected String getTag() {
		return getClass().getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getTag()
	 */
	@Override
	protected String getName() {
		return "Hangouts";
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getPackage()
	 */
	@Override
	protected String getPackage() {
		return "com.google.android.talk";
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getDb()
	 */
	@Override
	protected List<String> getDb() {
		return Arrays.asList("babel0.db", "babel1.db", "babel2.db", "babel3.db");
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getIcon()
	 */
	@Override
	protected Integer getIcon() {
		return R.drawable.ic_hangouts;
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getQuery()
	 */
	@Override
	protected String getQuery() {
		return "SELECT IFNULL(name, generated_name) AS conversation_name"
             + "  FROM conversations"
             + " WHERE self_watermark < latest_message_timestamp"
             + " ORDER"
             + "    BY sort_timestamp DESC;";
	}

}