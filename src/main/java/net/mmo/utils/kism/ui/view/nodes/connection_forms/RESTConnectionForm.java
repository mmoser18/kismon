/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.connections.HTTPConnection.HTTP_Method;
import net.mmo.utils.kism.entities.connections.RESTConnection;

@Slf4j
public class RESTConnectionForm extends ConnectionForm<RESTConnection>
{
	private static final long serialVersionUID = 7200842663851555372L;

	public static final String MethodClassName   = "node-form-field-method"; //$NON-NLS-1$
	public static final String ExecuteClassName  = "node-form-button-execute"; //$NON-NLS-1$

	public static final String RequestHeadersClassName  = "node-form-field-request-headers"; //$NON-NLS-1$
	public static final String RequestBodyClassName     = "node-form-field-request-body"; //$NON-NLS-1$

	public static final String ResponseHeadersClassName  = "node-form-field-response-headers"; //$NON-NLS-1$
	public static final String ResponseBodyClassName     = "node-form-field-response-body"; //$NON-NLS-1$

	private ComboBox<HTTP_Method> method;

	private TextArea requestHeaders;
	private TextArea requestBody;

	private TextArea responseHeaders;
	private TextArea responseBody;

	private Button execute;

	public RESTConnectionForm() {
		super(RESTConnection.class);
	}

	@Override
	protected void createFormFields() {
		super.createFormFields();

		this.method = new ComboBox<>(Messages.getString("HTTPConnectionForm.Method.Label")); //$NON-NLS-1$
		this.method.setItems(HTTP_Method.values());
		this.method.setClassName(MethodClassName);
		this.method.addValueChangeListener(change ->
			{
				HTTP_Method methodValue = change.getValue();
				log.info("method changed to {}", methodValue); //$NON-NLS-1$
				adjustPayloadVisibility(methodValue);
			});

		this.requestHeaders = new TextArea(Messages.getString("HTTPConnectionForm.RequestHeaders.Label")); //$NON-NLS-1$
		this.requestHeaders.setClassName(RequestHeadersClassName);
		this.binder.forField(this.requestHeaders).bind(n -> (n.getRequestHeaders() != null
		                                                     ? n.getRequestHeaders().toString()
		                                                     : null),
		                                               null);

		this.requestBody = new TextArea(Messages.getString("HTTPConnectionForm.RequestBody.Label")); //$NON-NLS-1$
		this.requestBody.setClassName(RequestBodyClassName);
		this.binder.forField(this.requestBody).bind(n -> (n.getRequestBody() != null
		                                                  ? new String(n.getRequestBody(), StandardCharsets.UTF_8)
		                                                  : null),
		                                            null);

		this.responseHeaders = new TextArea(Messages.getString("HTTPConnectionForm.ResponseHeaders.Label")); //$NON-NLS-1$
		this.responseHeaders.setClassName(ResponseHeadersClassName);
		this.binder.forField(this.responseHeaders).bind(n -> (n.getResponseHeaders() != null
		                                                      ? n.getResponseHeaders().toString()
		                                                      : null),
		                                                null);

		this.responseBody = new TextArea(Messages.getString("HTTPConnectionForm.ResponseBody.Label")); //$NON-NLS-1$
		this.responseBody.setClassName(ResponseBodyClassName);
		this.binder.forField(this.responseBody).bind(n -> (n.getResponseBody() != null
		                                                  ? new String(n.getResponseBody(), StandardCharsets.UTF_8)
		                                                  : null),
		                                            null);

		this.execute = new Button(Messages.getString("HTTPConnectionForm.Execute.Label")); //$NON-NLS-1$
		this.execute.setClassName(ExecuteClassName);
		this.execute.addClickListener(event -> this.node.sendRequest());

		this.fields.add(this.method,
		                this.requestHeaders, this.requestBody,
		                this.execute,
		                this.responseHeaders, this.responseBody);
	}

	@Override
	public void setNode(RESTConnection node) {
		super.setNode(node);
		adjustPayloadVisibility(node != null ? node.getMethod() : null);
	}

	/* we want the payload field only be visible when a corresponding method is selected */
	private void adjustPayloadVisibility(HTTP_Method methodValue) {
		this.requestBody.setVisible(methodValue != null ? methodValue.isWithPayload() : false); // null happens when setting the form's node to null (when closing it)
	}
}
