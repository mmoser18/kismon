/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes;

import net.mmo.utils.kism.backend.service.NodeService;

public class RootNodeForm extends NodeForm
{
	private static final long serialVersionUID = 2620944667353199126L;

	public RootNodeForm(NodeService nodeService) {
		super(nodeService);
	}

}
