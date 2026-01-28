/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/** utility class to make the default properties accessible */
@SuppressWarnings("javadoc")
@Slf4j
public class KeyValuesConverter
{
	// reading:
	public final static String KEY_VALUE_SEPARATORS = ":"; //$NON-NLS-1$
	public final static String PROPERTY_SEPARATORS = "[\t\n]"; //$NON-NLS-1$
	public final static String LEGAL_KEY_NAME_PATTERN = "\\p{Alpha}[\\w\\-]*"; //$NON-NLS-1$
	public final static String OPTIONAL_WHITE_SPACE = "\\s*"; //$NON-NLS-1$
	// generating:
	public final static char KEY_VALUE_SEPARATOR = ':';
	public final static char VALUES_SEPARATOR     = ',';
	public final static char PROPERTY_SEPARATOR = '\n';

	public final static String KEY_VALUE_SEPARATOR_STRING = KEY_VALUE_SEPARATOR + " "; //$NON-NLS-1$
	public final static String VALUES_SEPARATOR_STRING    = VALUES_SEPARATOR + " "; //$NON-NLS-1$
	public final static String PROPERTY_SEPARATOR_STRING  = Character.toString(PROPERTY_SEPARATOR);

	private final static Pattern KEY_VALUE_PATTERN =
		Pattern.compile("(" + LEGAL_KEY_NAME_PATTERN + ")" + // group 1: key //$NON-NLS-1$ //$NON-NLS-2$
		                OPTIONAL_WHITE_SPACE + KEY_VALUE_SEPARATORS + OPTIONAL_WHITE_SPACE + // separator
		                "(.*)"); // group 2: value //$NON-NLS-1$

	private KeyValuesConverter() {
		// utility class only - must never be created as instance!
	}

	/**
	 * Should actually be Map<String, String> but that isn't possible since Properties are defined as <Object, Object>
	 * @param map
	 * @return string representation
	 */
	public static String convertMapToString(Map<? extends Object, Object> map) { // ? extends object to satisfy the Java compiler when assigning a Map<String, Object>
		return convertMapToString(map, PROPERTY_SEPARATOR_STRING);
	}
	public static String convertMapToString(Map<? extends Object, Object> map, String propSeparator) { // ? extends object to satisfy the Java compiler when assigning a Map<String, Object>
		return map.entrySet().stream().map(entry -> entry.getKey() + KEY_VALUE_SEPARATOR_STRING + entry.getValue()).sorted().collect(Collectors.joining(propSeparator));
	}
	public static TreeMap<String, Object> convertStringToMap(String values) {
		TreeMap<String, Object> map = new TreeMap<>();
		log.trace("convertStringToMap(\"{}\")", values); //$NON-NLS-1$
		for (String entry: values.split(PROPERTY_SEPARATORS)) {
			if (!entry.isBlank()) {
//				log.trace("convertStringToMap(\"{}\"): entry:\"{}\"", values, entry); //$NON-NLS-1$
				Matcher m = KEY_VALUE_PATTERN.matcher(entry);
				if (m.matches()) {
					String key   = m.group(1);
					String value = m.group(2);
					if (StringUtils.isEmpty(key)) {
						log.error("convertStringToMap(\"{}\"): entry:\"{}\") -> key is null or blank", values, entry); //$NON-NLS-1$
						throw new IllegalArgumentException(String.format(Messages.getString("KeyValuesConverter.IllegalKeyName"), key)); //$NON-NLS-1$
					}
					if (value == null) {
						log.error("convertStringToMap(\"{}\"): entry:\"{}\") -> value is null", values, entry); //$NON-NLS-1$
						throw new IllegalArgumentException(String.format(Messages.getString("KeyValuesConverter.IllegalValue"), key)); //$NON-NLS-1$
					}
					map.put(key, value.trim());
				} else {
					throw new IllegalArgumentException(String.format(Messages.getString("KeyValuesConverter.IllegalSyntax"), entry)); //$NON-NLS-1$
				}
			}
		}
		return map;
	}

	/** utility method to extract fragment-values from HTTP headers */
	public static Object extractValueFragment(Map<String, List<String>> map,
	                                          String key,
	                                          String fragementSeparator,
	                                          Pattern pattern,
	                                          int matchingGroup,
	                                          Function<String, Object> converter,
	                                          Object defaultValue) {
		if (map != null) {
			List<String> mapValues = map.get(key.toUpperCase());
			if (mapValues != null) {
				for (String mapValue: mapValues) {
					int pos = mapValue.indexOf(fragementSeparator);
					if (pos > 1) {
						log.trace("extractValueFragment: mapValue='{}'", mapValue); //$NON-NLS-1$
						String charsetFragment = mapValue.substring(pos);
						log.trace("extractValueFragment: charsetFragment='{}'", charsetFragment); //$NON-NLS-1$
						Matcher m = pattern.matcher(charsetFragment);
						String fragment = null;
						if (m.matches() && ((fragment = m.group(matchingGroup)) != null)) {
							log.trace("extractValueFragment: match='{}'", fragment); //$NON-NLS-1$
							Object res = converter.apply(fragment);
							if (res != null) return res;
						} else {
							log.warn("extractValueFragment: '{}' not matching pattern '{}'.", mapValue, pattern); //$NON-NLS-1$
							// throw new Exception("charset '" + contentType + "' not matching expected format");
						}
					} else {
						log.trace("extractValueFragment: no matching fragment found in ", mapValue); //$NON-NLS-1$
					}
				}
			} else {
				log.trace("extractValueFragment: '{}' not found in headers.", key); //$NON-NLS-1$
			}
		}
		log.trace("extractValueFragment: using default value: '{}'", defaultValue); //$NON-NLS-1$
		return defaultValue;
	}

	public static String convertHeaderMapToString(Map<String, List<String>> headers) {
		return headers.entrySet().stream()
			.map(entry -> entry.getKey() + KEY_VALUE_SEPARATOR_STRING + convertHeaderValues(entry.getValue()))
			.collect(Collectors.joining(PROPERTY_SEPARATOR_STRING));
	}
	public static String convertHeaderValues(List<String> values) {
		return String.join(VALUES_SEPARATOR_STRING, values);
	}
}
