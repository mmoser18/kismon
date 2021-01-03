/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import net.mmo.utils.kism.entities.connections.HTTPConnection.HTTP_Method;
import net.mmo.utils.kism.entities.connections.SOAPConnection;

public class SOAPConnectionForm extends HTTPConnectionForm<SOAPConnection>
{
	private static final long serialVersionUID = 5942900173416889404L;

	public SOAPConnectionForm() {
		super(SOAPConnection.class);
	}

	@Override
	protected void createFormFields() {
		super.createFormFields();

		this.method.setVisible(false); // there is no choice of methjods for SOAP
		this.method.addValueChangeListener(change ->
			{
				HTTP_Method value = change.getValue();
				if (value != null && value != HTTP_Method.POST) {
					throw new IllegalArgumentException("HTTP-Method for SOAP can only be POST!"); //$NON-NLS-1$
				}
			});
	}
}
