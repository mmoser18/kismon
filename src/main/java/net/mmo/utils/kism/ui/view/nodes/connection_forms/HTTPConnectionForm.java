/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.connections.HTTPConnection;
import net.mmo.utils.kism.entities.connections.HTTPConnection.HTTP_Method;
import net.mmo.utils.kism.entities.connections.RESTConnection;
import net.mmo.utils.kism.ui.view.nodes.NodeView;
import net.mmo.utils.kism.utils.KeyValuesConverter;

@Slf4j
public abstract class HTTPConnectionForm<N extends HTTPConnection> extends IPConnectionForm<N>
{
	private static final long serialVersionUID = 7200842663851555372L;

	static final String MethodClassName   = "node-form-field-method"; //$NON-NLS-1$
	static final String RequestHeadersClassName  = "node-form-field-request-headers"; //$NON-NLS-1$
	static final String RequestBodyClassName     = "node-form-field-request-body"; //$NON-NLS-1$

	static final String ResponseStatusCodeClassName = "node-form-field-response-status"; //$NON-NLS-1$
	static final String ResponseHeadersClassName  = "node-form-field-response-headers"; //$NON-NLS-1$
	static final String ResponseBodyClassName     = "node-form-field-response-body"; //$NON-NLS-1$

	protected ComboBox<HTTP_Method> method;

	protected TextArea headers;
	protected TextArea payload;

	protected TextArea requestHeaders;
	protected TextArea requestBody;

	protected IntegerField responseStatusCode;

	protected TextArea responseHeaders;
	protected TextArea responseBody;

	public HTTPConnectionForm(Class<N> clazz) {
		super(clazz);
	}

	@Override
	protected void createFormFields() {
		super.createFormFields();

		this.method = new ComboBox<>(Messages.getString("HTTPConnectionForm.Method.Label")); //$NON-NLS-1$
		this.method.setClassName(MethodClassName);
		this.method.setItems(HTTP_Method.values());
		this.method.addValueChangeListener(change ->
			{
				HTTP_Method methodValue = change.getValue();
				log.info("method changed to {}", methodValue); //$NON-NLS-1$
				adjustPayloadVisibility(methodValue); // Note: this is called BEFORE the Save button has been pressed!
			});

		HorizontalLayout methodAndExecuteButton = new HorizontalLayout(this.method, this.execute);
		methodAndExecuteButton.addClassName(CombinedClassName);

		this.headers = new TextArea(Messages.getString("HTTPConnectionForm.RequestHeaders.Label")); //$NON-NLS-1$
		this.headers.setClassName(RequestHeadersClassName);
		this.headers.setThemeName(LabelPaddingTheme);
		this.headers.addValueChangeListener(event ->
		{
			log.info("headers value changed to: '{}'", event.getValue()); //$NON-NLS-1$
			if (event.isFromClient()) {
				this.node.setHeaders(event.getValue());
				updateRequestHeaders();
			}
		});

		this.binder.forField(this.headers)
			.withValidator(str ->
			{ // Explicit validator instance
				try {
					if (this.node != null) KeyValuesConverter.convertStringToMap(this.node.resolveProperties(str));
					return true; // still here -> the above conversion went succeeded
				} catch (Exception ex) {
					log.error("Exception converting '{}' to headers: {}", str, ex); //$NON-NLS-1$
					NodeView.UsageError(String.format(Messages.getString("HTTPConnectionForm.RequestHeaders.ErrorConvertingHeaders"), str, ex)); //$NON-NLS-1$
					return false;
				}
			}, Messages.getString("IPConnectionForm.Validator.Headers.IllegalValue")) //$NON-NLS-1$
			.bind(HTTPConnection::getRequestHeadersAsString, HTTPConnection::setHeaders);

		this.payload = new TextArea(Messages.getString("HTTPConnectionForm.RequestBody.Label")); //$NON-NLS-1$
		this.payload.setClassName(RequestBodyClassName);
		this.payload.setThemeName(LabelPaddingTheme);
		this.payload.addValueChangeListener(event ->
		{
			log.info("payload value changed to: '{}'", event.getValue()); //$NON-NLS-1$
			if (event.isFromClient()) {
				this.node.setPayload(event.getValue());
				updateRequestHeaders();
			}
		});

		this.responseStatusCode = new IntegerField(Messages.getString("HTTPConnectionForm.ResponseStatusCode.Label")); //$NON-NLS-1$
		this.responseStatusCode.setClassName(ResponseStatusCodeClassName);
		this.responseStatusCode.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.responseStatusCode).bind(HTTPConnection::getResponseStatusCode, null);

		HorizontalLayout responseStatusAndDuration = new HorizontalLayout(this.responseStatusCode, this.duration);
		responseStatusAndDuration.addClassName(CombinedClassName);

		this.requestHeaders = new TextArea(Messages.getString("HTTPConnectionForm.RequestHeaders.Label")); //$NON-NLS-1$
		this.requestHeaders.setClassName(RequestHeadersClassName);
		this.requestHeaders.setThemeName(LabelPaddingTheme);
		this.requestHeaders.addThemeName(MonospaceTheme);
		this.binder.forField(this.requestHeaders).bind(HTTPConnection::getRequestHeadersAsString, null);

		this.requestBody = new TextArea(Messages.getString("HTTPConnectionForm.RequestBody.Label")); //$NON-NLS-1$
		this.requestBody.setClassName(RequestBodyClassName);
		this.requestBody.setThemeName(LabelPaddingTheme);
		this.requestBody.addThemeName(MonospaceTheme);
		this.binder.forField(this.requestBody).bind(HTTPConnection::getRequestBodyAsString, null);

		this.responseHeaders = new TextArea(Messages.getString("HTTPConnectionForm.ResponseHeaders.Label")); //$NON-NLS-1$
		this.responseHeaders.setClassName(ResponseHeadersClassName);
		this.responseHeaders.setThemeName(LabelPaddingTheme);
		this.responseHeaders.addThemeName(MonospaceTheme);
		this.binder.forField(this.responseHeaders).bind(HTTPConnection::getResponseHeadersAsString, null);

		this.responseBody = new TextArea(Messages.getString("HTTPConnectionForm.ResponseBody.Label")); //$NON-NLS-1$
		this.responseBody.setClassName(ResponseBodyClassName);
		this.responseBody.setThemeName(LabelPaddingTheme);
		this.responseBody.addThemeName(MonospaceTheme);
		this.binder.forField(this.responseBody).bind(HTTPConnection::getResponseBodyAsString, null);

		this.fields.add(this.headers, this.payload,
		                methodAndExecuteButton,
		                new Label(Messages.getString("HTTPConnectionForm.Result.Label")), //$NON-NLS-1$
		                responseStatusAndDuration,
		                this.requestHeaders, this.requestBody,
		                this.responseHeaders, this.responseBody);
	}

	@Override
	public void setNode(N node) {
		super.setNode(node);
		updateRequestInfo();
	}

	@Override
	protected void handleChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
		switch (propertyName) {
		case HTTPConnection.PROPERTYNAME_RESPONSE_STATUS_CODE:
			if (individualUpdates) {
				this.responseStatusCode.setValue((Integer)newValue);
			}
			break;
		case HTTPConnection.PROPERTYNAME_REQUEST_HEADERS:
			if (individualUpdates) {
				this.requestHeaders.setValue(sourceNode.getRequestHeadersAsString());
			}
			break;
		case HTTPConnection.PROPERTYNAME_REQUEST_BODY:
			if (individualUpdates) {
				this.requestBody.setValue(sourceNode.getRequestBodyAsString());
			}
			break;
		case HTTPConnection.PROPERTYNAME_RESPONSE_HEADERS:
			if (individualUpdates) {
				this.responseHeaders.setValue(sourceNode.getResponseHeadersAsString());
			}
			break;
		case HTTPConnection.PROPERTYNAME_RESPONSE_BODY:
			if (individualUpdates) {
				this.responseBody.setValue(sourceNode.getResponseBodyAsString());
			}
			break;
		case RESTConnection.PROPERTYNAME_RESPONSE_COMPLETE:
			if (!individualUpdates) { // do all of the above in one go:
				this.responseStatusCode.setValue(sourceNode.getResponseStatusCode());
				this.requestHeaders.setValue(sourceNode.getRequestHeadersAsString());
				this.requestBody.setValue(sourceNode.getRequestBodyAsString());
				this.responseHeaders.setValue(sourceNode.getResponseHeadersAsString());
				this.responseBody.setValue(sourceNode.getResponseBodyAsString());
			}
			break;
		default:
			super.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
		}
	}

	private void updateRequestInfo() {
		adjustPayloadVisibility(this.node != null ? this.node.getMethod() : null);
		updateRequestHeaders();
	}

	@Override
	protected void updateResolvableValues() {
		super.updateResolvableValues();
		updateRequestHeaders();
	}

	/* we want the payload field only be visible when a corresponding method is selected */
	private void adjustPayloadVisibility(HTTP_Method methodValue) {
		this.payload.setVisible(methodValue != null ? methodValue.isWithPayload() : false); // null happens when setting the form's node to null (when closing it)
		this.requestBody.setVisible(methodValue != null ? methodValue.isWithPayload() : false); // null happens when setting the form's node to null (when closing it)
	}

	private void updateRequestHeaders() {
		if (this.node != null) {
			try {
				this.node.createRequest();
				this.requestHeaders.setValue(this.node.getRequestHeadersAsString());
				this.requestBody.setValue(this.node.getRequestBodyAsString());
			} catch (Exception ex) {
				log.error("Exception preparing/reporting request: ", ex); //$NON-NLS-1$
				this.requestHeaders.setValue(String.format(Messages.getString("HTTPConnectionForm.RequestHeaders.ErrorCreatingRequest"), ex)); //$NON-NLS-1$
			}
		} else {
			this.requestHeaders.setValue(Messages.getString("HTTPConnectionForm.RequestHeaders.Undefined")); //$NON-NLS-1$
		}
	}
}