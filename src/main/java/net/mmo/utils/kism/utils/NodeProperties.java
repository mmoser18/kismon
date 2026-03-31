/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.NodeProperties.NodePropertiesDeserializer;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

/** utility class to make a node's properties' field "defaults" accessible */
@SuppressWarnings("javadoc")
@Slf4j
@JsonDeserialize(using = NodePropertiesDeserializer.class)
public class NodeProperties extends Properties
{
	private static final long serialVersionUID = -7108847395448335625L;

	public NodeProperties() {
		this(null);
	}

	public NodeProperties(NodeProperties parentProperties) {
		setParentProperties(parentProperties);
	}

	@Override
	public String getProperty(String key) {
		// log.trace("getProperty({}):", key); //$NON-NLS-1$
		String res = super.getProperty(key);
		log.trace("getProperty('{}') --> '{}'", key, res); //$NON-NLS-1$
		return res;
	}

// unused...
//	public Properties getParentProperties() {
//		return this.defaults;
//	}

	/* This method is the actual purpose of this entire class: to get access to and be able
	 * to set the "defaults"-field:
	 */
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

	/**
	 * Had to add this Deserializer to teach Jackson to deserialize this object indeed as
	 * "NodeProperties" and not as a super-class-object "Properties" (which then caused a
	 * subsequent class-cast exception). Got this solution from:
	 * https://stackoverflow.com/questions/79917391/odd-type-casting-error-with-com-fasterxml-jackson-databind-v3-when-deserializi/79917449#79917449
	 **/
	public static class NodePropertiesDeserializer extends ValueDeserializer<NodeProperties>
	{
		@Override
		public NodeProperties deserialize(JsonParser p, DeserializationContext ctxt)
			throws JacksonException {
			Properties props = p.readValueAs(Properties.class);
			NodeProperties nodeProps = new NodeProperties();
			nodeProps.putAll(props);
			return nodeProps;
		}
	}
}
