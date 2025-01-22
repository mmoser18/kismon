/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes.connections;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("javadoc")
@Setter
@Getter
public class TelnetConnection extends TCPConnection
{
	private static final long serialVersionUID = 643208328101928988L;


	public TelnetConnection() { // required for deserialization & node-factory
		super();
	}
	public TelnetConnection(String name, String description) {
		super(name, description);
	}

	@Override
	public void sendRequest() throws Exception {
		throw new Exception("Telnet not yet implemented"); //$NON-NLS-1$
	}
}
