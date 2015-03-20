package com.mridang.hangouts;

import java.util.Arrays;
import java.util.List;

/**
 * This class is the main class that provides the widget
 */
public class WaddsappWidget extends MessageExtension {

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
		return "WhatsApp";
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getPackage()
	 */
	@Override
	protected String getPackage() {
		return "com.whatsapp";
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getDb()
	 */
	@Override
	protected List<String> getDb() {
		return Arrays.asList("wa.db");
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getIcon()
	 */
	@Override
	protected Integer getIcon() {
		return R.drawable.ic_whatsapp;
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hangouts.MessageExtension#getQuery()
	 */
	@Override
	protected String getQuery() {
		return "SELECT display_name "
			 + "  FROM wa_contacts"
			 + " WHERE unseen_msg_count > 0;";
	}

}