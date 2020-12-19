/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import com.vaadin.flow.component.textfield.TextField;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.ui.view.nodes.NodeForm;

@Slf4j
public class ConnectionForm extends NodeForm
{
	private static final long serialVersionUID = 5772018540738340331L;

	TextField protocol;
	TextField host;
	TextField port;
	TextField path;
	TextField arguments;

	public ConnectionForm(NodeService nodeService) {
		super(nodeService);
	}

	@Override
	protected void createFormFields() {
		log.debug("ConnectionForm.createFields() - begin"); //$NON-NLS-1$
		super.createFormFields();
		this.protocol  = new TextField(Messages.getString("ConnectionForm.TextField.Protocol.Label")); //$NON-NLS-1$
		this.host      = new TextField(Messages.getString("ConnectionForm.TextField.Host.Label")); //$NON-NLS-1$
		this.port      = new TextField(Messages.getString("ConnectionForm.TextField.Port.Label")); //$NON-NLS-1$
		this.path      = new TextField(Messages.getString("ConnectionForm.TextField.Path.Label")); //$NON-NLS-1$
		this.arguments = new TextField(Messages.getString("ConnectionForm.TextField.Arguments.Label")); //$NON-NLS-1$

		this.fields.add(this.protocol, this.host, this.port, this.path, this.arguments);
		log.debug("ConnectionForm.createFields() - end"); //$NON-NLS-1$
	}
}
