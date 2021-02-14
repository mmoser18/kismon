/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.connections.HTTPConnection.HTTP_Method;
import net.mmo.utils.kism.entities.connections.SOAPConnection;

@Slf4j
public class SOAPConnectionForm extends HTTPConnectionForm<SOAPConnection>
{
	private static final long serialVersionUID = 5942900173416889404L;

	static final String SoapActionClassName   = NodeFormField + "soap-operation"; //$NON-NLS-1$

	protected TextField soapAction;


	public SOAPConnectionForm(NodeService nodeService) {
		super(SOAPConnection.class, nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.soapAction = new TextField();
		this.soapAction = new TextField(Messages.getString("SOAPConnectionForm.SoapAction.Label")); //$NON-NLS-1$
		this.soapAction.setClassName(SoapActionClassName);
		this.soapAction.setThemeName(LabelPaddingTheme);
		this.soapAction.setClearButtonVisible(clearButtonsVisible);
		this.binder.forField(this.soapAction)
			.withValidator(this.propertiesResolvableValidator)
			.withValidator((value, context) ->
			{ // Explicit validator instance:
				log.trace("validating soapAction: '{}'", value); //$NON-NLS-1$
				String errMsg = checkSoapAction(value); // no error message -> OK
				return (errMsg != null ? ValidationResult.error(errMsg) : ValidationResult.ok());
			})
			.bind(SOAPConnection::getSoapAction, SOAPConnection::setSoapAction);
		this.connnectionDetailsPanel.addComponentAtIndex(2, this.soapAction); // put soapAction right after URL fields

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

	String checkSoapAction(String value) {
		return updateResultingField(this.soapAction.getLabel(), this.node, value, null, null);
	}
}
