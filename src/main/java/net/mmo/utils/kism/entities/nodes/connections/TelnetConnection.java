/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.cfg.NotYetImplementedException;

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
		throw new NotYetImplementedException("Telnet not yet implemented"); //$NON-NLS-1$
	}
}
