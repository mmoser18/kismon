/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 02.12.2020
 */

package net.mmo.utils.kism.utils;

import java.util.HashMap;
import java.util.Map;
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
	// generating:
	private final static String KEY_VALUE_SEPARATOR = ":"; //$NON-NLS-1$
	private final static String PROPERTY_SEPARATOR = "\n"; //$NON-NLS-1$

	private KeyValuesConverter() {
		// utility class only - must never be created as instance!
	}

	public static String convertMapToString(Map<? extends Object, Object> map) { // ? extends object to satisfy the Java compiler when assigning a Map<String, Object>
		return map.entrySet().stream().map(entry -> entry.getKey() + KEY_VALUE_SEPARATOR + entry.getValue()).collect(Collectors.joining(PROPERTY_SEPARATOR));
	}
	public static Map<String, Object> convertStringToMap(String values) {
		Map<String, Object> map = new HashMap<>();
		log.trace("convertStringToMap(\"{}\")", values); //$NON-NLS-1$
		for (String entry: values.split(PROPERTY_SEPARATORS)) {
			if (!entry.isBlank()) {
//				log.trace("convertStringToMap(\"{}\"): entry:\"{}\"", values, entry); //$NON-NLS-1$
				String[] splits = entry.split(KEY_VALUE_SEPARATORS);
				if (splits.length != 2) {
					log.error("convertStringToMap(\"{}\"): entry:\"{}\": splits:{} (length:{})", values, entry, splits, splits.length); //$NON-NLS-1$
					throw new IllegalArgumentException(String.format(Messages.getString("KeyValuesConverter.IllegalSyntax"), entry)); //$NON-NLS-1$
				} else if (!splits[0].matches(LEGAL_KEY_NAME_PATTERN)) {
					log.error("convertStringToMap(\"{}\"): entry:\"{}\": split[0]:'{}')", values, entry, splits[0]); //$NON-NLS-1$
					throw new IllegalArgumentException(String.format(Messages.getString("KeyValuesConverter.IllegalKeyName"), splits[0])); //$NON-NLS-1$
				} else {
//					log.trace("getEntriesFromString(\"{}\"): entry:\"{}\": splits:\"{}\"=\"{}\"", values, entry, splits[0], splits[1]); //$NON-NLS-1$
					map.put(splits[0], splits[1]);
				}
			}
		}
		return map;
	}
}
