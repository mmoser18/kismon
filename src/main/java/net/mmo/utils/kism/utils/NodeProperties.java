/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 02.12.2020
 */

package net.mmo.utils.kism.utils;

import java.util.Properties;
import java.util.stream.Collectors;

/** utility class to make the default properties accessible */
public class NodeProperties extends Properties
{
	private static final long serialVersionUID = -7108847395448335625L;

	private final static String PROPERTY_SEPARATOR = ","; //$NON-NLS-1$
	private final static String PROPERTY_SEPARATORS = "[,\n]"; //$NON-NLS-1$
	private final static String KEY_VALUE_SEPARATOR = ":"; //$NON-NLS-1$

	public Properties getParentProperties() {
		return this.defaults;
	}
	public void setParentProperties(Properties parentProperties) {
		this.defaults = parentProperties;
	}

	public String getProperties() {
		return this.entrySet().stream().map(entry -> entry.getKey() + KEY_VALUE_SEPARATOR + entry.getValue()).collect(Collectors.joining(PROPERTY_SEPARATOR));
	}
	public void setProperties(String values) {
		this.clear(); // remove all old values
		for (String entry: values.split(PROPERTY_SEPARATORS)) {
			String[] splits = entry.split(KEY_VALUE_SEPARATOR);
			this.put(splits[0], splits[1]);
		}
	}
}
