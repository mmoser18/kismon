/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connections;

import com.vaadin.flow.component.textfield.TextArea;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.ui.view.nodes.Messages;
import net.mmo.utils.kism.ui.view.nodes.NodeForm;

public class ConnectionForm extends NodeForm
{
	private static final long serialVersionUID = 5772018540738340331L;

	TextArea protocol  = new TextArea(Messages.getString("ConnectionForm.TextField.Protocol.Label")); //$NON-NLS-1$
	TextArea host      = new TextArea(Messages.getString("ConnectionForm.TextField.Host.Label")); //$NON-NLS-1$
	TextArea port      = new TextArea(Messages.getString("ConnectionForm.TextField.Port.Label")); //$NON-NLS-1$
	TextArea path      = new TextArea(Messages.getString("ConnectionForm.TextField.Path.Label")); //$NON-NLS-1$
	TextArea arguments = new TextArea(Messages.getString("ConnectionForm.TextField.Arguments.Label")); //$NON-NLS-1$

	public ConnectionForm(NodeService nodeService) {
		super(nodeService);
	}

}
