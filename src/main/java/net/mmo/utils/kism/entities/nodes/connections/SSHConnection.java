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
public class SSHConnection extends TCPConnection
{
	private static final long serialVersionUID = 643208328101928988L;


	public SSHConnection() { // required for deserialization & node-factory
		super();
	}
	public SSHConnection(String name, String description) {
		super(name, description);
	}

	@Override
	public void sendRequest() throws Exception {
		throw new NotYetImplementedException("SSH not yet implemented"); //$NON-NLS-1$
	}

	@Override
	public String getRequestResult() {
		return "tbd"; //$NON-NLS-1$
	}
}
