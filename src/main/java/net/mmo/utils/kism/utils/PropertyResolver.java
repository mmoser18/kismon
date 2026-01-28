/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * Code shamelessly stolen from the example shown here and then adapted for my purposes:
 * https://stackoverflow.com/questions/17349334/search-and-replace-formatted-properties-inside-java-string
 */
@SuppressWarnings("javadoc")
@Slf4j
public class PropertyResolver
{
	public static final Pattern EXPRESSION_PATTERN =
		Pattern.compile("\\$(" + KeyValuesConverter.LEGAL_KEY_NAME_PATTERN + ")|\\$\\{(" + KeyValuesConverter.LEGAL_KEY_NAME_PATTERN + ")\\}|(\\$\\$)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

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
		StringBuilder buf = new StringBuilder();
		int startSearch = 0;
		for (;;) { // we repeat this until we find no more placeHolder:
			// log.trace("tmpStr: '{}'", tmpStr); //$NON-NLS-1$
			Matcher matcher = EXPRESSION_PATTERN.matcher(tmpStr);
			int matcherEnd = 0;
			// log.trace("find starting from: '{}'", startSearch); //$NON-NLS-1$
			if (matcher.find(startSearch)) { // placeholder found?
				// copy prefix without any placeholders over to buf:
				buf.append(tmpStr.substring(startSearch, startSearch = matcher.start()));
				// log.trace("buf: '{}'", buf); //$NON-NLS-1$
				matcherEnd = matcher.end();
				// log.trace("startSearch: '{}'", startSearch); //$NON-NLS-1$
				// log.trace("matcherEnd : '{}'", matcherEnd); //$NON-NLS-1$
				// log.trace("matched str: '{}'", tmpStr.substring(startSearch, matcherEnd)); //$NON-NLS-1$
				String key = matcher.group(3); // variant: $$
				// log.trace("group3: '{}'", key); //$NON-NLS-1$
				if (key == null) {
					key = matcher.group(2); // variant: ${...}
					// log.trace("group2: '{}'", key); //$NON-NLS-1$
					if (key == null) {
						key = matcher.group(1); // variant: $...
						// log.trace("group1: '{}'", key); //$NON-NLS-1$
					}
					String property = (key != null ? props.getProperty(key) : null); // look up property and replace it
					// log.trace("property: '{}'", property); //$NON-NLS-1$
					if (property == null) { // no key or property not found, don't replace
						final String msg = String.format(Messages.getString("PropertyResolver.KeyNotFound"), key); //$NON-NLS-1$
						property = msg;
						throw new IllegalArgumentException(msg);
					}
					buf.append(property); // replace matching group by resolved property
				} else { // we found a $$ -> convert to a single $
					buf.append('$'); // add a single $ ...
					startSearch += 1; // ... which will NOT be considered as placeholder-start!
				}
				// log.trace("buf: '{}'", buf); //$NON-NLS-1$
			}
			if (matcherEnd <= 0) {
				break; // no (more) placeholder found - done!
			}
			// log.trace("remainder: '{}'", tmpStr.substring(matcherEnd)); //$NON-NLS-1$
			buf.append(tmpStr.substring(matcherEnd)); // add remainder of string
			// log.trace("buf: '{}'", buf); //$NON-NLS-1$
			tmpStr = buf.toString();
			buf.setLength(startSearch);
		}
		buf.append(tmpStr.substring(startSearch)); // add remainder of string
		// log.trace("resolved property: '{}'", buf); //$NON-NLS-1$
		return buf.toString().translateEscapes(); // replace escaped control characters with actual control characters
	}
}
