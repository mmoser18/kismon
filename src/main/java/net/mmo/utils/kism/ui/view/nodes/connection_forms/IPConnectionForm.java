/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import java.net.URI;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.connections.IPConnection;
import net.mmo.utils.kism.ui.view.nodes.NodeForm;
import net.mmo.utils.kism.ui.view.nodes.NodeView;
import net.mmo.utils.kism.utils.AppProperties;

@Slf4j
public abstract class IPConnectionForm <N extends IPConnection> extends NodeForm<N>
{
	private static final long serialVersionUID = 5772018540738340331L;

	static final String EnabledClassName = "node-form-field-enabled"; //$NON-NLS-1$
	static final String PeriodClassName  = "node-form-field-period"; //$NON-NLS-1$
	static final String TimeoutClassName = "node-form-field-timeout"; //$NON-NLS-1$

	static final String URLClassName          = "node-form-field-url"; //$NON-NLS-1$
	static final String ResultingURLClassName = "node-form-field-resulting-url"; //$NON-NLS-1$
	static final String ExecuteClassName      = "node-form-button-execute"; //$NON-NLS-1$
	static final String DurationClassName     = "node-form-field-duration"; //$NON-NLS-1$

	protected Checkbox enabled;
	protected IntegerField period;
	protected IntegerField timeout;

	protected TextField url;
	protected TextField resultingURL;

	// these are not yet added to the form:
	protected Button execute;
	protected NumberField duration;

	protected static boolean individualUpdates = Boolean.getBoolean(AppProperties.getProperties().getProperty("Connection.IndividualUpdates", "false")); //$NON-NLS-1$ //$NON-NLS-2$

	public IPConnectionForm(Class<N> clazz) {
		super(clazz);
	}

	@Override
	protected void createFormFields() {
		super.createFormFields();

		this.enabled = new Checkbox(Messages.getString("IPConnectionForm.Enabled.Label")); //$NON-NLS-1$
		this.enabled.addClassName(EnabledClassName);

		this.period = new IntegerField(Messages.getString("IPConnectionForm.Period.Label")); //$NON-NLS-1$
		this.period.setSuffixComponent(new Span(Messages.getString("IPConnectionForm.Period.Suffix"))); //$NON-NLS-1$
		this.period.addClassName(PeriodClassName);
		this.period.setThemeName(LabelPaddingTheme);

		this.timeout = new IntegerField(Messages.getString("IPConnectionForm.Timeout.Label")); //$NON-NLS-1$
		this.timeout.setSuffixComponent(new Span(Messages.getString("IPConnectionForm.Timeout.Suffix"))); //$NON-NLS-1$
		this.timeout.addClassName(TimeoutClassName);
		this.timeout.setThemeName(LabelPaddingTheme);

		HorizontalLayout enabledAndPeriod = new HorizontalLayout(this.enabled, this.period, this.timeout);
		enabledAndPeriod.addClassName(CombinedClassName);

		this.url = new TextField(Messages.getString("IPConnectionForm.URL.Label")); //$NON-NLS-1$
		this.url.setClassName(URLClassName);
		this.url.setThemeName(LabelPaddingTheme);
		this.url.addValueChangeListener(event ->
			{
				log.info("url value changed to: '{}'", event.getValue()); //$NON-NLS-1$
				if (event.isFromClient()) {
					updateResultingUrl(event.getValue());
				}
			});
		this.binder.forField(this.url)
			.withValidator(urlStr ->
				{ // Explicit validator instance
					log.trace("validating url: '{}'", urlStr); //$NON-NLS-1$
					try {
						if (this.node != null) {
							String resolvedUrlStr = this.node.resolveProperties(urlStr);
							URI uri = new URI(resolvedUrlStr);
							// no check - the actual error is thrown!
							log.debug("validation succeeded: '{}' => '{}'", urlStr, uri); //$NON-NLS-1$
						}
						return true;
					} catch (Exception ex) {
						log.debug("validation failed: '{}' -> {}", urlStr, ex); //$NON-NLS-1$
						return false;
					}
				}, Messages.getString("IPConnectionForm.Validator.URL.IllegalURL")) //$NON-NLS-1$
			.bind(IPConnection::getUrl, IPConnection::setUrl);

		this.resultingURL = new TextField(Messages.getString("IPConnectionForm.ResultingUrl.Value")); //$NON-NLS-1$
		this.resultingURL.setClassName(ResultingURLClassName);
		this.resultingURL.setThemeName(LabelPaddingTheme);
		// this.binder.forField(this.resultingURL).bind(IPConnection::getResultingUrl, null);
		this.binder.forField(this.resultingURL).bind(IPConnection::getResultingUrl, null);

		this.duration = new NumberField(Messages.getString("HTTPConnectionForm.Duration.Label")); //$NON-NLS-1$
		this.duration.setSuffixComponent(new Span(Messages.getString("HTTPConnectionForm.Duration.Suffix"))); //$NON-NLS-1$
		this.duration.setClassName(DurationClassName);
		this.duration.setThemeName(LabelPaddingTheme);
		this.duration.setStep(0.0001);
		this.binder.forField(this.duration).bind(IPConnectionForm::getDurationInMillis, null); // nsec's --> msec's (rounded to micro-seconds)

		this.execute = new Button(Messages.getString("HTTPConnectionForm.Execute.Label")); //$NON-NLS-1$
		this.execute.setClassName(ExecuteClassName);
		this.binder.addStatusChangeListener(e -> this.execute.setEnabled(this.binder.isValid()));
		this.execute.addClickListener(event ->
			{
				validateAndSave();
				try {
					this.node.sendRequest();
				} catch (Throwable t) {
					NodeView.ExecutionError("creating/executing request:", t); //$NON-NLS-1$
				}
			});

		this.fields.add(enabledAndPeriod, this.url, this.resultingURL);
	}

	protected String updateResultingUrl(String newValue) {
		return updateResultingField("updateResultingURL", //$NON-NLS-1$
		                            this.node,
		                            newValue,
		                            IPConnection::setResultingUrl,
		                            this.resultingURL
		                            );
	}

	@Override
	protected void updateResolvableValues() {
		super.updateResolvableValues();
		updateResultingUrl(this.node != null ? this.node.getUrl() : null);
	}

	public static Double getDurationInMillis(IPConnection n) {
		return IPConnection.convertNanoSecsToMillis(n.getDuration());
	}

	@Override
	protected void handleChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
		switch (propertyName) {
		case IPConnection.PROPERTYNAME_RESPONSE_COMPLETE:
			readBean(sourceNode);
			break;
		default:
			break;
		}
		super.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
	}

}
