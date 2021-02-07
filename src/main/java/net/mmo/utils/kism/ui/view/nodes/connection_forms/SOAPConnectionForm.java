/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.textfield.TextField;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.connections.HTTPConnection.HTTP_Method;
import net.mmo.utils.kism.entities.connections.SOAPConnection;

@Slf4j
public class SOAPConnectionForm extends HTTPConnectionForm<SOAPConnection>
{
	private static final long serialVersionUID = 5942900173416889404L;

	static final String SoapOperationClassName   = NodeFormField + "soap-operation"; //$NON-NLS-1$

	protected TextField soapOperation;

	private String soapOperationErrorMsg; // used to pass an error message from the validator's predicate to the validator's message provider.

	public SOAPConnectionForm(NodeService nodeService) {
		super(SOAPConnection.class, nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.soapOperation = new TextField();
		this.soapOperation = new TextField(Messages.getString("SOAPConnectionForm.SoapOperation.Label")); //$NON-NLS-1$
		this.soapOperation.setClassName(SoapOperationClassName);
		this.soapOperation.setThemeName(LabelPaddingTheme);
		this.soapOperation.setClearButtonVisible(clearButtonsVisible);
		this.binder.forField(this.soapOperation)
			.withValidator(this.propertiesResolvableValidator)
			.withValidator(operation ->
				{
					log.trace("validating soapOperation: '{}'", operation); //$NON-NLS-1$
					return ((this.soapOperationErrorMsg = checkSoapOperation(operation)) == null); // no error message -> OK
				}, (context) -> this.soapOperationErrorMsg)
		.bind(SOAPConnection::getSoapOperation, SOAPConnection::setSoapOperation);
		this.connnectionDetailsPanel.addComponentAtIndex(2, this.soapOperation); // put soapOperation right after URL fields.

		// there is no choice of methods for SOAP!
		this.method.setVisible(false);
		this.method.addValueChangeListener(change ->
			{
				HTTP_Method value = change.getValue();
				if (value != null && value != HTTP_Method.POST) {
					throw new IllegalArgumentException("HTTP-Method for SOAP can only be POST!"); //$NON-NLS-1$
				}
			});
	}

	String checkSoapOperation(String value) {
		return updateResultingField(this.soapOperation.getLabel(), this.node, value, null, null);
	}
}
