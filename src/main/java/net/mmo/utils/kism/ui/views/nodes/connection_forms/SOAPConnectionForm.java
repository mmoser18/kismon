/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.PreserveOnRefresh;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.connections.HTTPConnection.HTTP_Method;
import net.mmo.utils.kism.entities.nodes.connections.SOAPConnection;

@SuppressWarnings("javadoc")
@PreserveOnRefresh
public class SOAPConnectionForm extends HTTPConnectionForm<SOAPConnection>
{
	private static final long serialVersionUID = 5942900173416889404L;

	static final String SoapActionClassName   = NodeFormField + "soap-action"; //$NON-NLS-1$

	protected TextField soapAction;

	@Override
	public void init(NodeService nodeService) {
		super.init(SOAPConnection.class, nodeService);
	}
	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.soapAction = new TextField();
		this.soapAction = new TextField(Messages.getString("SOAPConnectionForm.SoapAction.Label")); //$NON-NLS-1$
		this.soapAction.setClassName(SoapActionClassName);
		this.soapAction.setThemeName(LabelPaddingTheme);
		this.soapAction.setClearButtonVisible(this.clearButtonsVisible);
		this.soapAction.setEnabled(this.isAdminUser);
		this.binder.forField(this.soapAction)
			.withValidator(this.propertiesResolvableValidator)
			.withValidator((value, context) ->
			{ // Explicit validator instance:
				this.log.trace("validating soapAction: '{}'", value); //$NON-NLS-1$
				String errMsg = checkSoapAction(value); // no error message -> OK
				return (errMsg != null ? ValidationResult.error(errMsg) : ValidationResult.ok());
			})
			.bind(SOAPConnection::getSoapAction, SOAPConnection::setSoapAction);
		this.connnectionDetailsPanel.addComponentAtIndex(2, this.soapAction); // put soapAction right after URL fields

		// there is no choice of methods for SOAP!
		this.method.setVisible(false);
		this.method.setEnabled(false); // just in case...
		this.method.addValueChangeListener(change ->
			{
				HTTP_Method value = change.getValue();
				if (value != null && value != HTTP_Method.POST) {
					throw new IllegalArgumentException("HTTP-Method for SOAP can only be POST!"); //$NON-NLS-1$
				}
			});
	}

	String checkSoapAction(String value) {
		return updateResultingField(this.soapAction.getLabel(), this.node, value, null, null);
	}
}
