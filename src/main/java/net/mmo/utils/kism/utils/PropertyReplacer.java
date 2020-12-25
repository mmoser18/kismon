/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 23.12.2020
 */

package net.mmo.utils.kism.utils;

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
	 * @param props properties
	 * @return resolved expression string
	 * @throws Exception
	 */
	public static String resolveExpression(String expression, NodeProperties props) throws Exception {
		StringBuilder result = new StringBuilder(expression.length());
		int i = 0;
		Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
		while (matcher.find()) {
			// Strip leading "${" and trailing "}" off.
			result.append(expression.substring(i, matcher.start()));
			String group = matcher.group();
			String key = group.substring(2, group.length() - 1);
			String property = props.getProperty(key); // look up property and replace
			if (property == null) { // property not found, don't replace
				property = group;
				throw new IllegalArgumentException(String.format(Messages.getString("PropertyReplacer.KeyNotFound"), key)); //$NON-NLS-1$
				// property = String.format(Messages.getString("PropertyReplacer.KeyNotFound"), key); //$NON-NLS-1$
			}
			result.append(property);
			i = matcher.end();
		}
		result.append(expression.substring(i));
		return result.toString();
	}

}
