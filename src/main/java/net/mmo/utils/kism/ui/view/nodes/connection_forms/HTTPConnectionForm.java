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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.connections.HTTPConnection;
import net.mmo.utils.kism.entities.connections.HTTPConnection.HTTP_Method;
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
	static final String AcceptableResturnCodesClassName = "node-form-field-acceptable-return-code"; //$NON-NLS-1$

	protected ComboBox<HTTP_Method> method;

	protected TextArea headers;
	protected TextArea payload;

	protected TextArea requestHeaders;
	protected TextArea requestBody;

	protected IntegerField responseStatusCode;

	protected TextArea responseHeaders;
	protected TextArea responseBody;

	protected TextField acceptableReturnCodes;


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
				// this.node.setHeaders(event.getValue());
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
				// this.node.setPayload(event.getValue());
				updateRequestHeaders();
			}
		});

		this.responseStatusCode = new IntegerField(Messages.getString("HTTPConnectionForm.ResponseStatusCode.Label")); //$NON-NLS-1$
		this.responseStatusCode.setClassName(ResponseStatusCodeClassName);
		this.responseStatusCode.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.responseStatusCode).bind(HTTPConnection::getResponseStatusCode, null);

		HorizontalLayout responseDetailsFields = new HorizontalLayout(this.responseStatusCode, this.duration/*, this.timestamp*/);
		responseDetailsFields.addClassName(CombinedClassName);

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

		this.acceptableReturnCodes = new TextField(Messages.getString("HTTPConnectionForm.AcceptableResponseCodes.Label")); //$NON-NLS-1$
		this.acceptableReturnCodes.setClassName(AcceptableResturnCodesClassName);
		this.acceptableReturnCodes.setThemeName(LabelPaddingTheme);
		this.acceptableReturnCodes.addThemeName(MonospaceTheme);

		Binder.Binding<N, Boolean> checkResultBinding = // memorize the binding below so we can re-trigger it
			this.binder.forField(this.checkResult)
				.withValidator(check -> (check == this.acceptableReturnCodes.getValue().isEmpty()),
				               Messages.getString("HTTPConnectionForm.CheckResult.ValidationErrorMsg")) //$NON-NLS-1$
				.bind(HTTPConnection::getCheckResult, HTTPConnection::setCheckResult);

		Binder.Binding<N, String> acceptableReturnCodesBinding = // memorize the binding below so we can re-trigger it
			this.binder.forField(this.acceptableReturnCodes)
				.withValidator(text -> this.checkResult.isEnabled() ? text.matches("\\d{1,3}(\\s*,\\s*\\d{1,3})*") : text.isEmpty(), //$NON-NLS-1$
				               Messages.getString("HTTPConnectionForm.AcceptableResponseCodes.ValidationErrorMsg")) //$NON-NLS-1$
				.bind(HTTPConnection::getAcceptableReturnCodes, HTTPConnection::setAcceptableReturnCodes);

		this.checkResult.addValueChangeListener(event -> acceptableReturnCodesBinding.validate());
		this.acceptableReturnCodes.addValueChangeListener(event -> checkResultBinding.validate());

		HorizontalLayout checkResultFields = new HorizontalLayout(this.checkResult, this.acceptableReturnCodes);
		checkResultFields.addClassName(CombinedClassName);

		this.fields.add(this.headers, this.payload,
		                methodAndExecuteButton,
		                new Label(Messages.getString("HTTPConnectionForm.Result.Label")), //$NON-NLS-1$
		                responseDetailsFields,
		                this.requestHeaders, this.requestBody,
		                this.responseHeaders, this.responseBody,
		                checkResultFields);
	}

	@Override
	public boolean setNode(N node) {
		boolean res = super.setNode(node);
		updateRequestInfo();
		return res;
	}

//	@Override
//	protected void validate() throws ValidationException {
//		super.validate();
//		if (this.node != null) {
//			try {
//				this.node.createRequest();
//			} catch (Exception ex) {
//				log.error("Exception preparing/reporting request: ", ex); //$NON-NLS-1$
//				throw new ValidationException(Collections.emptyList(), Collections.emptyList());
//			}
//		}
//	}



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
			} catch (Exception ex) {
				log.error("Exception preparing/reporting request: ", ex); //$NON-NLS-1$
				this.node.setRequestBody(String.format(Messages.getString("HTTPConnectionForm.RequestHeaders.ErrorCreatingRequest"), ex).getBytes()); //$NON-NLS-1$
			}
		} else {
			this.requestHeaders.setValue(Messages.getString("HTTPConnectionForm.RequestHeaders.Undefined")); //$NON-NLS-1$
			this.requestBody.setValue(Messages.getString("HTTPConnectionForm.RequestBody.Undefined")); //$NON-NLS-1$
			this.responseHeaders.setValue(Messages.getString("HTTPConnectionForm.ResponseHeaders.Undefined")); //$NON-NLS-1$
			this.responseBody.setValue(Messages.getString("HTTPConnectionForm.ResponseBody.Undefined")); //$NON-NLS-1$
		}
	}
}