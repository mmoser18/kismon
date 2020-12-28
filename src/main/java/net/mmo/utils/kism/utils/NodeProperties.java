/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 02.12.2020
 */

package net.mmo.utils.kism.utils;

import java.util.Properties;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/** utility class to make the default properties accessible */
@Slf4j
public class NodeProperties extends Properties
{
	private static final long serialVersionUID = -7108847395448335625L;

	private final static String KEY_VALUE_SEPARATORS = "[:=]"; //$NON-NLS-1$
	private final static String KEY_VALUE_SEPARATOR = "="; //$NON-NLS-1$
	private final static String PROPERTY_SEPARATORS = "[,;\t\n]"; //$NON-NLS-1$
	private final static String PROPERTY_SEPARATOR = "\n"; //$NON-NLS-1$

	public NodeProperties() {
		// empty
	}

	public NodeProperties(Properties parentProperties) {
		setParentProperties(parentProperties);
	}

	public Properties getParentProperties() {
		return this.defaults;
	}

	@Override
	public String getProperty(String key) {
		log.trace("getProperty({}):", key); //$NON-NLS-1$
		String res = super.getProperty(key);
		log.trace("getProperty({}) --> {}", key, res); //$NON-NLS-1$
		return res;
	}

	/* This is the actual purpose of this class: to get access to and be able to set "defaults": */
	public void setParentProperties(Properties parentProperties) {
		this.defaults = parentProperties;
	}

	public String getPropertiesAsString() {
		return this.entrySet().stream().map(entry -> entry.getKey() + KEY_VALUE_SEPARATOR + entry.getValue()).collect(Collectors.joining(PROPERTY_SEPARATOR));
	}
	public void setPropertiesFromString(String values) {
		this.clear(); // remove all old values
		log.debug("setProperties(\"{}\")", values); //$NON-NLS-1$
		for (String entry: values.split(PROPERTY_SEPARATORS)) {
			if (!entry.isBlank()) {
				log.trace("setProperties(\"{}\"): entry:\"{}\"", values, entry); //$NON-NLS-1$
				String[] splits = entry.split(KEY_VALUE_SEPARATORS);
				if (splits.length >= 2) {
					log.trace("setProperties(\"{}\"): entry:\"{}\": splits:\"{}\"=\"{}\"", values, entry, splits[0], splits[1]); //$NON-NLS-1$
					this.put(splits[0], splits[1]);
				} else {
					log.debug("setProperties(\"{}\"): entry:\"{}\": splits:{} (length:{})", values, entry, splits, splits.length); //$NON-NLS-1$
				}
			}
		}
	}
}
