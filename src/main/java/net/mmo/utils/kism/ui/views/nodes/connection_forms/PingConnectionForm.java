/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17 Mar 2021
 */

package net.mmo.utils.kism.ui.views.nodes.connection_forms;

import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.connections.PingConnection;


public class PingConnectionForm extends IPConnectionForm<PingConnection>
{
	private static final long serialVersionUID = -5157762871511746801L;

	public PingConnectionForm(NodeService nodeService) {
		super(PingConnection.class, nodeService);
	}
}
