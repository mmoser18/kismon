/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes.connection_forms;

import com.vaadin.flow.router.PreserveOnRefresh;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.connections.RESTConnection;

@SuppressWarnings("javadoc")
@PreserveOnRefresh
public class RESTConnectionForm extends HTTPConnectionForm<RESTConnection>
{
	private static final long serialVersionUID = 7200842663851555372L;

	@Override
	public void init(NodeService nodeService) {
		super.init(RESTConnection.class, nodeService);
	}
}