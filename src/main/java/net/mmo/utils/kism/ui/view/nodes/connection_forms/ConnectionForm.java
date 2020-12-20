/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.TextField;
import net.mmo.utils.kism.ui.view.nodes.NodeForm;

public class ConnectionForm extends NodeForm
{
	private static final long serialVersionUID = 5772018540738340331L;

	public static final String ProtocolClassName  = "node-form-protocol"; //$NON-NLS-1$
	public static final String HostClassName      = "node-form-host"; //$NON-NLS-1$
	public static final String PortClassName      = "node-form-port"; //$NON-NLS-1$
	public static final String PathClassName      = "node-form-path"; //$NON-NLS-1$
	public static final String ArgumentsClassName = "node-form-arguments"; //$NON-NLS-1$
	public static final String EnabledClassName   = "node-form-enabled"; //$NON-NLS-1$

	TextField protocol;
	TextField host;
	TextField port;
	TextField path;
	TextField arguments;
	Checkbox enabled;

	public ConnectionForm() {
		super();
	}

	@Override
	protected void createFormFields() {
		super.createFormFields();
		this.protocol  = new TextField(Messages.getString("ConnectionForm.Protocol.Label")); //$NON-NLS-1$
		this.protocol.setClassName(ProtocolClassName);
		this.protocol.setMaxLength(10); // is there anything longer than 5 characters?

		this.host      = new TextField(Messages.getString("ConnectionForm.Host.Label")); //$NON-NLS-1$
		this.host.setClassName(HostClassName);
		this.host.setMaxLength(255); // official max. length for an FQDN

		this.port      = new TextField(Messages.getString("ConnectionForm.Port.Label")); //$NON-NLS-1$
		this.port.setClassName(PortClassName);
		this.port.setMaxLength(5);

		this.path      = new TextField(Messages.getString("ConnectionForm.Path.Label")); //$NON-NLS-1$
		this.path.setClassName(PathClassName);

		this.arguments = new TextField(Messages.getString("ConnectionForm.Arguments.Label")); //$NON-NLS-1$
		this.arguments.setClassName(ArgumentsClassName);

		this.enabled = new Checkbox(Messages.getString("ConnectionForm.Enabled.Label")); //$NON-NLS-1$
		this.enabled.addClassName(EnabledClassName);

		this.fields.add(this.protocol, this.host, this.port, this.path, this.arguments, this.enabled);
	}
}
