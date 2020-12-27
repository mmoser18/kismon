/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
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

	public static final String ResponseStatusCodeClassName = "node-form-field-response-status"; //$NON-NLS-1$
	public static final String DurationClassName           = "node-form-field-duration"; //$NON-NLS-1$

	public static final String ResponseHeadersClassName  = "node-form-field-response-headers"; //$NON-NLS-1$
	public static final String ResponseBodyClassName     = "node-form-field-response-body"; //$NON-NLS-1$


	private ComboBox<HTTP_Method> method;

	private TextArea requestHeaders;
	private TextArea requestBody;

	private IntegerField responseStatusCode;
	private NumberField duration;

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
		this.method.setClassName(MethodClassName);
		this.method.setItems(HTTP_Method.values());
		this.method.addValueChangeListener(change ->
			{
				HTTP_Method methodValue = change.getValue();
				log.info("method changed to {}", methodValue); //$NON-NLS-1$
				adjustPayloadVisibility(methodValue); // Note: this is called BEFORE the Save button has been pressed!
			});

		this.requestHeaders = new TextArea(Messages.getString("HTTPConnectionForm.RequestHeaders.Label")); //$NON-NLS-1$
		this.requestHeaders.setClassName(RequestHeadersClassName);
		this.requestHeaders.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.requestHeaders).bind(RESTConnectionForm::convertRequestHeaders, null);

		this.requestBody = new TextArea(Messages.getString("HTTPConnectionForm.RequestBody.Label")); //$NON-NLS-1$
		this.requestBody.setClassName(RequestBodyClassName);
		this.requestBody.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.requestBody).bind(RESTConnectionForm::convertRequestBody,
		                                            RESTConnectionForm::setRequestBody);

		this.responseStatusCode = new IntegerField(Messages.getString("HTTPConnectionForm.ResponseStatusCode.Label")); //$NON-NLS-1$
		this.responseStatusCode.setClassName(ResponseStatusCodeClassName);
		this.responseStatusCode.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.responseStatusCode).bind(RESTConnectionForm::convertResponseStatusCode, null);

		this.duration = new NumberField(Messages.getString("HTTPConnectionForm.Duration.Label")); //$NON-NLS-1$
		this.duration.setClassName(DurationClassName);
		this.duration.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.duration).bind(RESTConnectionForm::convertDuration, null); // nsec's --> msec's (rounded to micro-seconds)

		HorizontalLayout responseStatusAndDuration = new HorizontalLayout(this.responseStatusCode, this.duration);
		responseStatusAndDuration.addClassName(CombinedClassName);

		this.responseHeaders = new TextArea(Messages.getString("HTTPConnectionForm.ResponseHeaders.Label")); //$NON-NLS-1$
		this.responseHeaders.setClassName(ResponseHeadersClassName);
		this.responseHeaders.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.responseHeaders).bind(RESTConnectionForm::convertResponseHeaders, null);

		this.responseBody = new TextArea(Messages.getString("HTTPConnectionForm.ResponseBody.Label")); //$NON-NLS-1$
		this.responseBody.setClassName(ResponseBodyClassName);
		this.responseBody.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.responseBody).bind(RESTConnectionForm::convertResponseBody, null);

		this.execute = new Button(Messages.getString("HTTPConnectionForm.Execute.Label")); //$NON-NLS-1$
		this.execute.setClassName(ExecuteClassName);
		this.execute.addClickListener(event -> this.node.sendRequest());

		this.fields.add(this.method,
		                this.requestHeaders, this.requestBody,
		                this.execute,
		                responseStatusAndDuration,
		                this.responseHeaders, this.responseBody);
	}

	@Override
	public void setNode(RESTConnection node) {
		super.setNode(node);
		updateRequestInfo();
	}

	@Override
	protected void updateResultingURL() {
		super.updateResultingURL();
		updateRequestInfo();
	}

	private void updateRequestInfo() {
		adjustPayloadVisibility(this.node != null ? this.node.getMethod() : null);
		updateRequestHeaders();
	}

	/* we want the payload field only be visible when a corresponding method is selected */
	private void adjustPayloadVisibility(HTTP_Method methodValue) {
		this.requestBody.setVisible(methodValue != null ? methodValue.isWithPayload() : false); // null happens when setting the form's node to null (when closing it)
	}

	private void updateRequestHeaders() {
		if (this.node != null) {
			try {
				this.node.createRequest();
			} catch (Exception ex) {
				log.error("Exception preparing/reporting request: ", ex); //$NON-NLS-1$
				this.requestHeaders.setValue(String.format(Messages.getString("HTTPConnectionForm.RequestHeaders.ErrorMsg"), ex)); //$NON-NLS-1$
			}
		} else {
			this.requestHeaders.setValue(Messages.getString("HTTPConnectionForm.RequestHeaders.Undefined")); //$NON-NLS-1$
		}
	}
	public void updateResponseData() {
		updateResponseStatus();
		updateDuration();
		updateResponseHeaders();
		updateResponseBody();
		updateState();
	}

	private void updateState() {
		this.state.setValue(this.node.getState().toString());
	}
	private void updateResponseStatus() {
		this.responseStatusCode.setValue(convertResponseStatusCode(this.node));
	}
	private void updateDuration() {
		this.duration.setValue(convertDuration(this.node));
	}
	private void updateResponseHeaders() {
		this.responseHeaders.setValue(convertResponseHeaders(this.node));
	}
	private void updateResponseBody() {
		this.responseBody.setValue(convertResponseBody(this.node));
	}

	private static Integer convertResponseStatusCode(RESTConnection n) {
		return convertResponseStatusCode(n.getResponseStatusCode());
	}
	private static Integer convertResponseStatusCode(int responseCode) {
		return (responseCode != 0 ? Integer.valueOf(responseCode) : null);
	}

	private static Double convertDuration(RESTConnection n) {
		return convertDuration(n.getDuration());
	}
	private static Double convertDuration(long responseDuration) {
		return (responseDuration != 0 ? (Math.round(responseDuration/1000)*1.0e-3) : 0); // nsec's --> msec's (rounded to micro-seconds)
	}

	private static String convertRequestHeaders(RESTConnection n) {
		return convertHeaders(n.getRequestHeaders());
	}
	private static String convertResponseHeaders(RESTConnection n) {
		return convertHeaders(n.getResponseHeaders());
	}
	private static String convertHeaders(HttpHeaders headers) {
		return (headers != null ? headers.toString() : null);
	}

	private static String convertRequestBody(RESTConnection n) {
		return convertBody(n.getRequestBody());
	}
	private static String convertResponseBody(RESTConnection n) {
		return convertBody(n.getResponseBody());
	}
	private static String convertBody(byte[] body) {
		return (body != null ? new String(body, StandardCharsets.UTF_8) : null);
	}

	private static void setRequestBody(RESTConnection n, String value) {
		n.setRequestBody(convertBody(value));
	}
	private static byte[] convertBody(String value) {
		return (value != null ? value.getBytes(StandardCharsets.UTF_8) : null);
	}
}