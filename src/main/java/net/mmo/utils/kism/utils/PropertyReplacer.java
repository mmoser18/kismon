/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 23.12.2020
 */

package net.mmo.utils.kism.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code shamelessly stolen from the example shown here:
 * https://stackoverflow.com/questions/17349334/search-and-replace-formatted-properties-inside-java-string
 */
public class PropertyReplacer
{
	private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]*)\\}"); //$NON-NLS-1$

	/**
	 * Replace ${properties} in an expression
	 *
	 * @param expression expression string
	 * @param map property map
	 * @return resolved expression string
	 */
	public static String resolveExpression(String expression, Map<Object, Object> map) {
		StringBuilder result = new StringBuilder(expression.length());
		int i = 0;
		Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
		while (matcher.find()) {
			// Strip leading "${" and trailing "}" off.
			result.append(expression.substring(i, matcher.start()));
			String property = matcher.group();
			property = property.substring(2, property.length() - 1);
			if (map.containsKey(property)) { // look up property and replace
				property = map.get(property).toString();
			} else { // property not found, don't replace
				property = matcher.group();
			}
			result.append(property);
			i = matcher.end();
		}
		result.append(expression.substring(i));
		return result.toString();
	}

}
