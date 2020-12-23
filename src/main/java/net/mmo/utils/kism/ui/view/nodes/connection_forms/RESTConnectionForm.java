/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.connections.RESTConnection;
import net.mmo.utils.kism.entities.connections.RESTConnection.HTTP_Method;

@Slf4j
public class RESTConnectionForm extends ConnectionForm<RESTConnection>
{
	private static final long serialVersionUID = 7200842663851555372L;

	public static final String MethodClassName   = "node-form-field-method"; //$NON-NLS-1$
	public static final String PayloadClassName  = "node-form-field-payload"; //$NON-NLS-1$

	private ComboBox<HTTP_Method> method;
	private TextArea payLoad;

	public RESTConnectionForm() {
		super(RESTConnection.class);
	}

	@Override
	protected void createFormFields() {
		super.createFormFields();

		this.method = new ComboBox<>(Messages.getString("RESTConnectionForm.Method.Label")); //$NON-NLS-1$
		this.method.setItems(HTTP_Method.values());
		this.method.setClassName(MethodClassName);
		this.method.addValueChangeListener(change ->
			{
				HTTP_Method methodValue = change.getValue();
				log.info("method changed to {}", methodValue); //$NON-NLS-1$
				adjustPayloadVisibility(methodValue);
			});

		this.payLoad = new TextArea(Messages.getString("RESTConnectionForm.Payload.Label")); //$NON-NLS-1$
		this.payLoad.setClassName(PayloadClassName);

		this.fields.add(this.method, this.payLoad);
	}

	@Override
	public void setNode(RESTConnection node) {
		super.setNode(node);
		adjustPayloadVisibility(node != null ? node.getMethod() : null);
	}

	/* we want the payload field only be visible when a corresponding method is selected */
	private void adjustPayloadVisibility(HTTP_Method methodValue) {
		this.payLoad.setVisible(methodValue != null ? methodValue.isWithPayload() : false); // null happens when setting the form's node to null (when closing it)
	}
}
