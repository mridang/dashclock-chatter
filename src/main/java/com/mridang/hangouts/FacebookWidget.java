package com.mridang.hangouts;

import java.util.Arrays;
import java.util.List;


/**
 * This class is the main class that provides the widget
 */
public class FacebookWidget extends MessageExtension {

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
		return "Facebook";
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getPackage()
	 */
	@Override
	protected String getPackage() {
		return "com.facebook.orca";
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getDb()
	 */
	@Override
	protected List<String> getDb() {
		return Arrays.asList("threads_db2");
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getIcon()
	 */
	@Override
	protected Integer getIcon() {
		return R.drawable.ic_facebook;
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getQuery()
	 */
	@Override
	protected String getQuery() {
		return "SELECT IFNULL(tu.name, th.name) AS name"
			 + "  FROM threads th"
			 + "  LEFT"
			 + " OUTER"
			 + "  JOIN thread_users tu"
			 + "    ON REPLACE(th.thread_key, 'ONE_TO_ONE:', 'FACEBOOK:')"
			 + "       LIKE (tu.user_key || '%')"
			 + " WHERE th.unread = 1;";
	}

}