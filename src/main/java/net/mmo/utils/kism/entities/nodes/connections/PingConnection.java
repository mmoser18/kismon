/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.cfg.NotYetImplementedException;

@Entity
@Setter
@Getter
public class PingConnection extends TCPConnection
{
	private static final long serialVersionUID = 643208328101928988L;

	@SuppressWarnings("unused")
	private PingConnection() { // required for deserialization
		super();
	}
	public PingConnection(String name, String description) {
		super(name, description);
	}

	@Override
	public void sendRequest() throws Exception {
		throw new NotYetImplementedException("Ping not yet implemented"); //$NON-NLS-1$
	}

	@Override
	protected String getRequestResult() {
		return "tbd"; //$NON-NLS-1$
	}
}
