/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.MonospaceTheme;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.connections.HTTPConnection;
import net.mmo.utils.kism.entities.nodes.connections.HTTPConnection.HTTP_Method;
import net.mmo.utils.kism.entities.nodes.connections.TCPConnection;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.utils.KeyValuesConverter;
import net.mmo.utils.kism.utils.StringUtils;
import net.mmo.utils.kism.utils.XmlPrettyPrinter;

@SuppressWarnings("javadoc")
public abstract class HTTPConnectionForm<N extends HTTPConnection> extends TCPConnectionForm<N>
{
	private static final long serialVersionUID = 7200842663851555372L;

	static final String UidClassName   = NodeFormField + "dbUid"; //$NON-NLS-1$
	static final String PwdClassName   = NodeFormField + "dbPwd"; //$NON-NLS-1$
	static final String BasicAuthHdrClassName = NodeFormField + "basicAuthHeader"; //$NON-NLS-1$

	static final String MethodClassName                = NodeFormField + "method"; //$NON-NLS-1$
	static final String RequestHeadersClassName        = NodeFormField + "httpRequest-headers"; //$NON-NLS-1$
	static final String RequestBodyClassName           = NodeFormField + "httpRequest-body"; //$NON-NLS-1$

	static final String ResponseStatusCodeClassName    = NodeFormField + "response-status"; //$NON-NLS-1$
	static final String ResponseHeadersClassName       = NodeFormField + "response-headers"; //$NON-NLS-1$
	static final String ResponseBodyClassName          = NodeFormField + "response-body"; //$NON-NLS-1$
	static final String AcceptableReturnCodesClassName = NodeFormField + "acceptable-return-codes"; //$NON-NLS-1$
	protected TextField targetUid;
	protected PasswordField targetPwd;
	protected Checkbox includeBasicAuthHeader;

	protected Select<HTTP_Method> method;

	// original request values (possibly containing properties):
	protected TextArea headers;
	protected TextArea payload;

	// resolved request values:
	protected TextArea requestHeaders;
	protected TextArea requestBody;

	// response values:
	protected IntegerField responseStatusCode;
	protected TextArea responseHeaders;
	protected TextArea responseBody;

	// validation values
	protected TextField acceptableReturnCodes;

	protected Checkbox prettyPrintRequestXml;
	protected Checkbox prettyPrintResponseXml;

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		// had to move the URL validation here since JDBC-URLs look completely differently and
		// didn't pass the conventional URI check!
		this.binder.forField(this.url)
			.withValidator(this.urlValidator)
			.bind(TCPConnection::getUrl, TCPConnection::setUrl);

		this.binder.forField(this.resultingURL)
			.withValidator(this.urlValidator)
			.bind(TCPConnection::getResultingUrl, null);

		this.targetUid = new TextField(Messages.getString("HTTPConnectionForm.Uid.Label")); //$NON-NLS-1$
		this.targetUid.setClassName(UidClassName);
		this.targetUid.setThemeName(LabelPaddingTheme);
		this.targetUid.setClearButtonVisible(this.clearButtonsVisible);
		this.targetUid.setEnabled(this.isAdminUser);
		this.binder.forField(this.targetUid)
			.withValidator(this.propertiesResolvableValidator)
			.bind(HTTPConnection::getTargetUid, HTTPConnection::setTargetUid);

		this.targetPwd = new PasswordField(Messages.getString("HTTPConnectionForm.Pwd.Label")); //$NON-NLS-1$
		this.targetPwd.setClassName(PwdClassName);
		this.targetPwd.setThemeName(LabelPaddingTheme);
		this.targetPwd.setClearButtonVisible(this.clearButtonsVisible);
		this.targetPwd.setEnabled(this.isAdminUser);
		this.targetPwd.setRevealButtonVisible(this.isAdminUser);
		if (this.isAdminUser) {
			this.binder.forField(this.targetPwd)
				.withValidator(this.propertiesResolvableValidator)
				.bind(HTTPConnection::getTargetPwd, HTTPConnection::setTargetPwd);
		} else { // we don't want to send the pwd to the client - not even hidden - if (s)he is not an admin user!
			this.binder.forField(this.targetPwd).bind(CommonConstants::suppressedPassword, null);
		}

		this.includeBasicAuthHeader= new Checkbox(Messages.getString("HTTPConnectionForm.IncludeBasicAuthHeader.Label")); //$NON-NLS-1$
		this.includeBasicAuthHeader.setClassName(BasicAuthHdrClassName);
		this.includeBasicAuthHeader.setEnabled(this.isAdminUser);
		this.binder.forField(this.includeBasicAuthHeader)
			.withValidator((value, _) ->
				{ // Explicit validator instance:
					if (value) {
						if (this.node != null) {
							try {
								String resolvedUid = this.node.resolveProperties(this.node.getTargetUid());
								@SuppressWarnings("unused")
								String resolvedPwd = this.node.resolveProperties(this.node.getTargetPwd());
								if (StringUtils.isEmpty(resolvedUid)) { // We only check the uid. No check on pwd! pwd CAN resolve to empty - an account without password is possible and legal!
									return ValidationResult.error(Messages.getString("HTTPConnectionForm.Validator.IncludeBasicAuth.UidPwdNotSet")); //$NON-NLS-1$
								}
							} catch (Exception ex) { // exception in resolveProperties:
								return ValidationResult.error(String.format(Messages.getString("NodeForm.Validator.Properties.IllegalValue"), //$NON-NLS-1$
								                                            value, ex.getMessage()));
							}
						}
					}
					return ValidationResult.ok();
				})
			.bind(HTTPConnection::isIncludeBasicAuthHeader, HTTPConnection::setIncludeBasicAuthHeader);

		this.method = new Select<>();
		this.method.setLabel(Messages.getString("HTTPConnectionForm.Method.Label")); //$NON-NLS-1$
		this.method.setItems(HTTP_Method.values());
		this.method.setClassName(MethodClassName);
		this.method.setEnabled(this.isAdminUser);
		// this.method.setItemLabelGenerator(meth -> Messages.getString("HTTPConnectionForm.Method." + meth.name())); //$NON-NLS-1$
		this.method.addValueChangeListener(event ->
			{
				if (event.isFromClient()) {
					this.log.info("method changed from {} to {}", event.getOldValue(), event.getValue()); //$NON-NLS-1$
					adaptPayloadVisibility(event.getValue()); // Note: this is called BEFORE the Save button has been pressed!
				}
			});

		this.headers = new TextArea(Messages.getString("HTTPConnectionForm.Headers.Label")); //$NON-NLS-1$
		this.headers.setClassName(RequestHeadersClassName);
		this.headers.setThemeName(LabelPaddingTheme);
		this.headers.addThemeName(MonospaceTheme);
		this.headers.setClearButtonVisible(this.clearButtonsVisible);
		this.headers.setEnabled(this.isAdminUser);
		this.headers.addValueChangeListener(event ->
		{
			if (event.isFromClient()) {
				this.log.info("headers value changed from '{}' to '{}'", event.getOldValue(), event.getValue()); //$NON-NLS-1$
				// this.node.setHeaders(event.getValue());
				initRequestData();
			}
		});
		this.binder.forField(this.headers)
			.withValidator((value, _) ->
				{ // Explicit validator instance:
					try {
						if (this.node != null) KeyValuesConverter.convertStringToMap(this.node.resolveProperties(value));
						return ValidationResult.ok(); // still here -> the above conversion succeeded
					} catch (Exception ex) { // Exception -> the above conversion failed
						return ValidationResult.error(String.format(Messages.getString("HTTPConnectionForm.RequestHeaders.ErrorConvertingHeaders"), //$NON-NLS-1$
						                                            value, ex.getMessage()));
					}
				})
			.bind(HTTPConnection::getHeaders, HTTPConnection::setHeaders);

		this.payload = new TextArea(Messages.getString("HTTPConnectionForm.Payload.Label")); //$NON-NLS-1$
		this.payload.setClassName(RequestBodyClassName);
		this.payload.setThemeName(LabelPaddingTheme);
		this.payload.addThemeName(MonospaceTheme);
		this.payload.setClearButtonVisible(this.clearButtonsVisible);
		this.payload.setEnabled(this.isAdminUser);
		this.payload.addValueChangeListener(event ->
		{
			if (event.isFromClient()) {
				this.log.info("payload value changed from '{}' to '{}'", event.getOldValue(), event.getValue()); //$NON-NLS-1$
				// this.node.setPayload(event.getValue());
				initRequestData();
			}
		});
		this.binder.forField(this.payload)
			.withValidator(this.propertiesResolvableValidator)
			.bind(HTTPConnection::getPayload, HTTPConnection::setPayload);

		this.responseStatusCode = new IntegerField(Messages.getString("HTTPConnectionForm.ResponseStatusCode.Label")); //$NON-NLS-1$
		this.responseStatusCode.setClassName(ResponseStatusCodeClassName);
		this.responseStatusCode.setTooltipText(Messages.getString("HTTPConnectionForm.ResponseStatusCode.Tooltip")); //$NON-NLS-1$
		this.responseStatusCode.setThemeName(LabelPaddingTheme);
		this.responseStatusCode.setReadOnly(true);
		this.binder.forField(this.responseStatusCode)
			.bind(HTTPConnection::getResponseStatusCode, null);

		this.requestHeaders = new TextArea(Messages.getString("HTTPConnectionForm.RequestHeaders.Label")); //$NON-NLS-1$
		this.requestHeaders.setClassName(RequestHeadersClassName);
		this.requestHeaders.setThemeName(LabelPaddingTheme);
		this.requestHeaders.addThemeName(MonospaceTheme);
		this.requestHeaders.setReadOnly(true);
		this.binder.forField(this.requestHeaders)
			.bind(HTTPConnection::requestHeadersAsString, null);

		this.requestBody = new TextArea(Messages.getString("HTTPConnectionForm.RequestBody.Label")); //$NON-NLS-1$
		this.requestBody.setClassName(RequestBodyClassName);
		this.requestBody.setThemeName(LabelPaddingTheme);
		this.requestBody.addThemeName(MonospaceTheme);
		this.requestBody.setReadOnly(true);
		this.binder.forField(this.requestBody)
			.bind((n) -> requestBodyAsString(n), null);

		this.responseHeaders = new TextArea(Messages.getString("HTTPConnectionForm.ResponseHeaders.Label")); //$NON-NLS-1$
		this.responseHeaders.setClassName(ResponseHeadersClassName);
		this.responseHeaders.setThemeName(LabelPaddingTheme);
		this.responseHeaders.addThemeName(MonospaceTheme);
		this.responseHeaders.setReadOnly(true);
		this.binder.forField(this.responseHeaders)
			.bind(HTTPConnection::responseHeadersAsString, null);

		this.responseBody = new TextArea(Messages.getString("HTTPConnectionForm.ResponseBody.Label")); //$NON-NLS-1$
		this.responseBody.setClassName(ResponseBodyClassName);
		this.responseBody.setThemeName(LabelPaddingTheme);
		this.responseBody.addThemeName(MonospaceTheme);
		this.responseBody.setReadOnly(true);
		this.binder.forField(this.responseBody)
			.bind((n) -> responseBodyAsString(n), null);

		this.prettyPrintRequestXml = new Checkbox(Messages.getString("HTTPConnectionForm.PrettyPrintXml.Label")); //$NON-NLS-1$
		this.prettyPrintRequestXml.setClassName(BasicAuthHdrClassName);
		this.prettyPrintRequestXml.addValueChangeListener((_) -> this.requestBody.setValue(requestBodyAsString(this.node)));

		this.prettyPrintResponseXml = new Checkbox(Messages.getString("HTTPConnectionForm.PrettyPrintXml.Label")); //$NON-NLS-1$
		this.prettyPrintResponseXml.setClassName(BasicAuthHdrClassName);
		this.prettyPrintResponseXml.addValueChangeListener((_) -> this.responseBody.setValue(responseBodyAsString(this.node)));

		this.acceptableReturnCodes = new TextField(Messages.getString("HTTPConnectionForm.AcceptableResponseCodes.Label")); //$NON-NLS-1$
		this.acceptableReturnCodes.setClassName(AcceptableReturnCodesClassName);
		this.acceptableReturnCodes.setThemeName(LabelPaddingTheme);
		this.acceptableReturnCodes.addThemeName(MonospaceTheme);
		this.acceptableReturnCodes.setClearButtonVisible(this.clearButtonsVisible);
		this.acceptableReturnCodes.setEnabled(this.isAdminUser);

		// we cross-link the validators of the checkResults-checkbox and the acceptableReturnCodes-input field:
		Binder.Binding<N, String> acceptableReturnCodesBinding = // memorize the binding below so we can re-trigger it
			this.binder.forField(this.acceptableReturnCodes)
				.withValidator(this.propertiesResolvableValidator)
				.withValidator(text -> {
									boolean valid = text.matches("\\d{1,3}(\\s*,\\s*\\d{1,3})*") || (!this.checkResults.getValue() && text.isEmpty()); //$NON-NLS-1$
									this.log.trace("acceptableReturnCodes-validator: {}", valid); //$NON-NLS-1$
									return valid;
								},
				               Messages.getString("HTTPConnectionForm.AcceptableResponseCodes.ValidationErrorMsg")) //$NON-NLS-1$
				.bind(HTTPConnection::getAcceptableReturnCodes, HTTPConnection::setAcceptableReturnCodes);

		this.checkResults.addValueChangeListener(event ->
		{
			this.log.debug("checkResults value changed from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
			if (event.isFromClient()) {
				adaptAcceptableReturnCodesVisibility(event.getValue());
				acceptableReturnCodesBinding.validate();
			}
		});

		// combining and grouping bits and pieces:
		HorizontalLayout credentials = new HorizontalLayout(this.targetUid, this.targetPwd, this.includeBasicAuthHeader);
		credentials.addClassName(CombinedClassName);

		// combining and grouping bits and pieces:
		this.connnectionDetailsPanel.add(credentials, this.method, this.headers, this.payload);

		Accordion requestDetails = new Accordion();
		requestDetails.close(); // initially closed
		VerticalLayout requestDetailsPanel = new VerticalLayout();
		requestDetailsPanel.setClassName(RequestDetailsClassName);
		requestDetailsPanel.add(this.requestHeaders,  this.requestBody,  this.prettyPrintRequestXml,
		                        this.responseHeaders, this.responseBody, this.prettyPrintResponseXml);
		requestDetails.add(Messages.getString("HTTPConnectionForm.RequestAndResponseDetails.Label"), requestDetailsPanel); //$NON-NLS-1$

		HorizontalLayout responseDetailsFields = new HorizontalLayout(this.responseStatusCode, this.duration, this.timestamp);
		responseDetailsFields.addClassName(CombinedClassName);

		this.validationDetailsPanel.addComponentAtIndex(1, this.acceptableReturnCodes);

		this.fields.add(new NativeLabel(Messages.getString("HTTPConnectionForm.Result.Label")), //$NON-NLS-1$
		                responseDetailsFields,
		                requestDetails,
		                this.validationDetails
		               );
	}

	@Override
	public boolean setNode(N n, boolean abandonChanges) {
		boolean res = super.setNode(n, abandonChanges);
		adaptPayloadVisibility(n != null ? n.getMethod() : null);
		updateRequestData();
		updateResponseData();
		adaptAcceptableReturnCodesVisibility(n != null ? n.isCheckResults() : false);
		return res;
	}

	private void adaptAcceptableReturnCodesVisibility(boolean check) {
		this.acceptableReturnCodes.setVisible(check);
	}

//	@Override
//	protected void validate() throws ValidationException {
//		super.validate();
//		if (this.node != null) {
//			try {
//				this.node.createRequest();
//			} catch (Exception ex) {
//				log.error("Exception preparing/reporting httpRequest: ", ex); //$NON-NLS-1$
//				throw new ValidationException(Collections.emptyList(), Collections.emptyList());
//			}
//		}
//	}

	@Override
	protected void updateResolvableValues() {
		super.updateResolvableValues();
		initRequestData();
	}

	/* we want the payload field only be visible when a corresponding method is selected */
	protected  void adaptPayloadVisibility(HTTP_Method methodValue) {
		this.payload.setVisible(methodValue != null ? methodValue.isWithPayload() : false); // null happens when setting the form's node to null (when closing it)
		this.requestBody.setVisible(methodValue != null ? methodValue.isWithPayload() : false); // null happens when setting the form's node to null (when closing it)
	}

// shouldn't be necessary any more...
//	@Override
//	protected boolean binderHasChanges() {
//		boolean hasChanges = super.binderHasChanges();
//		if (hasChanges && this.node != null) this.node.setHttpRequest(null); // trigger a re-generation of the request if e.g. the URL has changed
//		return hasChanges;
//	}

	protected void initRequestData() {
		if (this.node != null) {
			try {
				this.node.ensureValidRequest();
			} catch (Exception ex) {
				this.log.error("Exception preparing/reporting httpRequest: ", ex); //$NON-NLS-1$
				this.node.setRequestBody(String.format(Messages.getString("HTTPConnectionForm.RequestHeaders.ErrorCreatingRequest"), ex).getBytes()); //$NON-NLS-1$
			}
		} else {
			this.requestHeaders.setValue(Messages.getString("HTTPConnectionForm.RequestHeaders.Undefined")); //$NON-NLS-1$
			this.requestBody.setValue(Messages.getString("HTTPConnectionForm.RequestBody.Undefined")); //$NON-NLS-1$
			this.responseHeaders.setValue(Messages.getString("HTTPConnectionForm.ResponseHeaders.Undefined")); //$NON-NLS-1$
			this.responseBody.setValue(Messages.getString("HTTPConnectionForm.ResponseBody.Undefined")); //$NON-NLS-1$
		}
	}

	protected void updateRequestData() {
		if (this.node != null) {
			this.requestHeaders.setValue(this.node.requestHeadersAsString());
			this.requestBody.setValue(requestBodyAsString());
		}
	}

	protected void updateResponseData() {
		if (this.node != null) {
			this.responseHeaders.setValue(this.node.responseHeadersAsString());
			this.responseBody.setValue(responseBodyAsString());
		}
	}

	protected String requestBodyAsString() {
		return requestBodyAsString(this.node);
	}
	protected String requestBodyAsString(N n) {
		String convertedRequest = n.requestBodyAsString();
		String requestText = (this.prettyPrintRequestXml.getValue() ? XmlPrettyPrinter.prettyPrintXML(convertedRequest, false) : convertedRequest);
		return requestText != null ? requestText : ""; //$NON-NLS-1$
	}
	protected String responseBodyAsString() {
		return responseBodyAsString(this.node);
	}
	protected String responseBodyAsString(N n) {
		final String convertedResponse = n.responseBodyAsString();
		final String responseText = (this.prettyPrintResponseXml.getValue() ? XmlPrettyPrinter.prettyPrintXML(convertedResponse, false) : convertedResponse);
		return responseText != null ? responseText : ""; //$NON-NLS-1$
	}

	@Override
	protected void handleChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
		super.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
		if (sourceNode == this.node) {
			switch (propertyName) {
			case HTTPConnection.PROPERTYNAME_REQUEST_HEADERS:
				this.requestHeaders.setValue(sourceNode.requestHeadersAsString());
				break;
			case HTTPConnection.PROPERTYNAME_REQUEST_BODY:
				this.requestBody.setValue(requestBodyAsString());
				break;
			case HTTPConnection.PROPERTYNAME_RESPONSE_HEADERS:
				this.responseHeaders.setValue(sourceNode.responseHeadersAsString());
				break;
			case HTTPConnection.PROPERTYNAME_RESPONSE_BODY:
				this.responseBody.setValue(responseBodyAsString());
			break;
			default:
				// ignore
			}
		}
	}

}