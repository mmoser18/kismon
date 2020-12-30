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
public class KeyValues
{
	private final static String KEY_VALUE_SEPARATORS = "[:=]"; //$NON-NLS-1$
	private final static String KEY_VALUE_SEPARATOR = "="; //$NON-NLS-1$
	private final static String PROPERTY_SEPARATORS = "[,;\t\n]"; //$NON-NLS-1$
	private final static String PROPERTY_SEPARATOR = "\n"; //$NON-NLS-1$

	private KeyValues() {
		// must never be created!
	}

	public static String getMapAsString(Map<Object, Object> set) {
		return set.entrySet().stream().map(entry -> entry.getKey() + KEY_VALUE_SEPARATOR + entry.getValue()).collect(Collectors.joining(PROPERTY_SEPARATOR));
	}
	public static Map<String, Object> getEntriesFromString(String values) {
		Map<String, Object> map = new HashMap<>();
		log.debug("getEntriesFromString(\"{}\")", values); //$NON-NLS-1$
		for (String entry: values.split(PROPERTY_SEPARATORS)) {
			if (!entry.isBlank()) {
//				log.trace("getEntriesFromString(\"{}\"): entry:\"{}\"", values, entry); //$NON-NLS-1$
				String[] splits = entry.split(KEY_VALUE_SEPARATORS);
				if (splits.length != 2) {
					log.error("getEntriesFromString(\"{}\"): entry:\"{}\": splits:{} (length:{})", values, entry, splits, splits.length); //$NON-NLS-1$
					throw new IllegalArgumentException(String.format(Messages.getString("KeyValues.IllegalSyntax"), entry)); //$NON-NLS-1$
				} else if (!splits[0].matches("\\w+")) { //$NON-NLS-1$
					log.error("getEntriesFromString(\"{}\"): entry:\"{}\": split[0]:'{}')", values, entry, splits[0]); //$NON-NLS-1$
					throw new IllegalArgumentException(String.format(Messages.getString("KeyValues.IllegalKeyName"), splits[0])); //$NON-NLS-1$
				} else {
//					log.trace("getEntriesFromString(\"{}\"): entry:\"{}\": splits:\"{}\"=\"{}\"", values, entry, splits[0], splits[1]); //$NON-NLS-1$
					map.put(splits[0], splits[1]);
				}
			}
		}
		return map;
	}
}
