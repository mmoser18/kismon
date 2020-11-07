/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 23.12.2020
 */

package net.mmo.utils.kism.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code shamelessly stolen from the example shown here and then adapted for my purposes:
 * https://stackoverflow.com/questions/17349334/search-and-replace-formatted-properties-inside-java-string
 */
@SuppressWarnings("javadoc")
// @Slf4j
public class PropertyResolver
{
	public static final Pattern EXPRESSION_PATTERN =
		Pattern.compile("\\$(" + KeyValuesConverter.LEGAL_KEY_NAME_PATTERN + ")|\\{(" + KeyValuesConverter.LEGAL_KEY_NAME_PATTERN + ")\\}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	/**
	 * Replace ${properties} in an expression
	 *
	 * @param startStr expression string
	 * @param props properties to start with
	 * @return resolved expression string
	 * @throws Exception
	 */
	public static String resolveProperties(String startStr, NodeProperties props) throws Exception {
		String tmpStr = startStr;
		for (;;) { // we repeat this until we find no more placeHolder:
			// log.trace("tmpStr: '{}'", tmpStr); //$NON-NLS-1$
			int i = 0;
			StringBuilder buf = new StringBuilder();
			Matcher matcher = EXPRESSION_PATTERN.matcher(tmpStr);
			while (matcher.find()) { // placeholder found?
				buf.append(tmpStr.substring(i, matcher.start()-1));
				String key = matcher.group(2);
				// log.trace("group2: '{}'", key); //$NON-NLS-1$
				if (key == null) {
					key = matcher.group(1);
					// log.trace("group1: '{}'", key); //$NON-NLS-1$
				}
				String property = (key != null ? props.getProperty(key) : null); // look up property and replace it
				// log.trace("property: '{}'", property); //$NON-NLS-1$
				if (property == null) { // no key or property not found, don't replace
					// property = String.format(Messages.getString("PropertyResolver.KeyNotFound"), key); //$NON-NLS-1$
					throw new IllegalArgumentException(String.format(Messages.getString("PropertyResolver.KeyNotFound"), key)); //$NON-NLS-1$
				}
				buf.append(property); // replace matching group by placeholder
				i = matcher.end();
			}
			if (i <= 0) return tmpStr; // no (more) placeholder found - done!
			buf.append(tmpStr.substring(i)); // add remainder of string
			tmpStr = buf.toString().translateEscapes();
		}
	}

}
