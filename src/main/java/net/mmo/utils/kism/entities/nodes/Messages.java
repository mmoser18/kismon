/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 13.11.2020
 */

package net.mmo.utils.kism.entities.nodes;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

@SuppressWarnings("javadoc")
public class Messages
{
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$

	private static ResourceBundle RESOURCE_BUNDLE;

	private Messages() {}

	public static String getString(String key) {
		try {
			if (RESOURCE_BUNDLE == null) { RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME); }
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
