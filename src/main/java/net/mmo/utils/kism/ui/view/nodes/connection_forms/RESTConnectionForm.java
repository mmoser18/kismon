/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import net.mmo.utils.kism.backend.service.NodeService;

public class RESTConnectionForm extends ConnectionForm
{
	private static final long serialVersionUID = 7200842663851555372L;

	public RESTConnectionForm(NodeService nodeService) {
		super(nodeService);
	}

}
