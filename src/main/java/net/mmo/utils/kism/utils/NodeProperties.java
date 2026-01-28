/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

/** utility class to make a node's properties accessible */
@SuppressWarnings("javadoc")
@Slf4j
public class NodeProperties extends Properties
{
	private static final long serialVersionUID = -7108847395448335625L;

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
		// log.trace("getProperty({}):", key); //$NON-NLS-1$
		String res = super.getProperty(key);
		log.trace("getProperty('{}') --> '{}'", key, res); //$NON-NLS-1$
		return res;
	}

	/* This method is the actual purpose of this class: to get access to and be able to set "defaults": */
	public void setParentProperties(Properties parentProperties) {
		this.defaults = parentProperties;
	}

	public String getPropertiesAsString() {
		return KeyValuesConverter.convertMapToString(this);
	}
	public void setPropertiesFromString(String values) {
		this.clear();
		this.putAll(KeyValuesConverter.convertStringToMap(values));
	}
}
