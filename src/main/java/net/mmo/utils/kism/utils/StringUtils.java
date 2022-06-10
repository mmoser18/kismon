/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 31 May 2022
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
