/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 02.12.2020
 */

package net.mmo.utils.kism.utils;

import java.util.Properties;

/** utility class to make the default properties accessible */
public class NodeProperties extends Properties
{
	private static final long serialVersionUID = -7108847395448335625L;

	public Properties getParentProperties() {
		return this.defaults;
	}
	public void setParentProperties(Properties parentProperties) {
		this.defaults = parentProperties;
	}
}
