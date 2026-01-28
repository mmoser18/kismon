/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
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
