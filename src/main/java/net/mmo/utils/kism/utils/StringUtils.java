/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

/**
 * A check that should be in java.lang.String...
 */
public class StringUtils
{
	/**
	 * @param value
	 * @return whether the string is null or blank
	 */
	public static boolean isEmpty(String value) {
		return value == null || value.isBlank();
	}
}
