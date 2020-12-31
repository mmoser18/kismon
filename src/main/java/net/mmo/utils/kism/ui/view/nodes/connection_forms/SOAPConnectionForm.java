/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import net.mmo.utils.kism.entities.connections.SOAPConnection;

public class SOAPConnectionForm extends ConnectionForm<SOAPConnection>
{
	private static final long serialVersionUID = 5942900173416889404L;

	public SOAPConnectionForm() {
		super(SOAPConnection.class);
	}
}
