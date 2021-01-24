/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 02.12.2020
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
@Slf4j
public class KeyValuesConverter
{
	// reading:
	private final static String KEY_VALUE_SEPARATORS = ":"; //$NON-NLS-1$
	private final static String PROPERTY_SEPARATORS = "[,\t\n]"; //$NON-NLS-1$
	private final static String LEGAL_KEY_NAME_PATTERN = "\\w[\\w\\-]*"; //$NON-NLS-1$
	private final static String OPTIONAL_WHITE_SPACE = "\\s*"; //$NON-NLS-1$
	// generating:
	public final static char KEY_VALUE_SEPARATOR = ':';
	public final static char VALUES_SEPARATOR     = ',';
	public final static char PROPERTY_SEPARATOR = '\n';

	public final static String KEY_VALUE_SEPARATOR_STRING = KEY_VALUE_SEPARATOR + " "; //$NON-NLS-1$
	public final static String VALUES_SEPARATOR_STRING    = VALUES_SEPARATOR + " "; //$NON-NLS-1$
	public final static String PROPERTY_SEPARATOR_STRING  = Character.toString(PROPERTY_SEPARATOR);

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
		return map.entrySet().stream().map(entry -> entry.getKey() + KEY_VALUE_SEPARATOR_STRING + entry.getValue()).collect(Collectors.joining(propSeparator));
	}
	public static TreeMap<String, Object> convertStringToMap(String values) {
		TreeMap<String, Object> map = new TreeMap<>();
		log.trace("convertStringToMap(\"{}\")", values); //$NON-NLS-1$
		for (String entry: values.split(PROPERTY_SEPARATORS)) {
			if (!entry.isBlank()) {
//				log.trace("convertStringToMap(\"{}\"): entry:\"{}\"", values, entry); //$NON-NLS-1$
				String[] splits = entry.split(OPTIONAL_WHITE_SPACE + KEY_VALUE_SEPARATORS + OPTIONAL_WHITE_SPACE);
				if (splits.length != 2) {
					log.error("convertStringToMap(\"{}\"): entry:\"{}\": splits:{} (length:{})", values, entry, splits, splits.length); //$NON-NLS-1$
					throw new IllegalArgumentException(String.format(Messages.getString("KeyValuesConverter.IllegalSyntax"), entry)); //$NON-NLS-1$
				} else if (!splits[0].matches(LEGAL_KEY_NAME_PATTERN)) {
					log.error("convertStringToMap(\"{}\"): entry:\"{}\": split[0]:'{}')", values, entry, splits[0]); //$NON-NLS-1$
					throw new IllegalArgumentException(String.format(Messages.getString("KeyValuesConverter.IllegalKeyName"), splits[0])); //$NON-NLS-1$
				} else {
//					log.trace("getEntriesFromString(\"{}\"): entry:\"{}\": splits:\"{}\"=\"{}\"", values, entry, splits[0], splits[1]); //$NON-NLS-1$
					map.put(splits[0], splits[1].trim());
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
						String charsetFragment = mapValue.substring(pos);
						log.trace("extractValueFragment: charsetFragment = '{}'", charsetFragment); //$NON-NLS-1$
						Matcher m = pattern.matcher(charsetFragment);
						String fragment = null;
						if (m.matches() && ((fragment = m.group(matchingGroup)) != null)) {
							log.trace("extractValueFragment: match = '{}'", fragment); //$NON-NLS-1$
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
		log.debug("extractValueFragment: using default value: '{}'", defaultValue); //$NON-NLS-1$
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
