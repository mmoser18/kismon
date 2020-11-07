/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("javadoc")
@Setter
@Getter
public class RESTConnection extends HTTPConnection
{
	private static final long serialVersionUID = -2228821478937714703L;

	public RESTConnection() { // required for deserialization & node-factory
		super();
	}
	public RESTConnection(String name, String description) {
		super(name, description);
	}
}
