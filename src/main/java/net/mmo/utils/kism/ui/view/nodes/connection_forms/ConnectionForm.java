/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.connections.Connection;
import net.mmo.utils.kism.ui.view.nodes.NodeForm;

@Slf4j
public abstract class ConnectionForm <N extends Connection> extends NodeForm<N>
{
	private static final long serialVersionUID = 5772018540738340331L;

	public static final String EnabledClassName = "node-form-field-enabled"; //$NON-NLS-1$
	public static final String PeriodClassName  = "node-form-field-period"; //$NON-NLS-1$
	public static final String TimeoutClassName = "node-form-field-timeout"; //$NON-NLS-1$

	public static final String URLClassName          = "node-form-field-url"; //$NON-NLS-1$
	public static final String ResultingURLClassName = "node-form-field-resulting-url"; //$NON-NLS-1$
	public static final String ExecuteClassName      = "node-form-button-execute"; //$NON-NLS-1$
	public static final String DurationClassName     = "node-form-field-duration"; //$NON-NLS-1$


	protected Checkbox enabled;
	protected IntegerField period;
	protected IntegerField timeout;

	protected TextField url;
	protected Label resultingURL;

	// these are not yet added to the form:
	protected Button execute;
	protected NumberField duration;

	public ConnectionForm(Class<N> clazz) {
		super(clazz);
	}

	@Override
	protected void createFormFields() {
		super.createFormFields();

		this.enabled = new Checkbox(Messages.getString("ConnectionForm.Enabled.Label")); //$NON-NLS-1$
		this.enabled.addClassName(EnabledClassName);

		this.period = new IntegerField(Messages.getString("ConnectionForm.Period.Label")); //$NON-NLS-1$
		this.period.setSuffixComponent(new Span(Messages.getString("ConnectionForm.Period.Suffix"))); //$NON-NLS-1$
		this.period.addClassName(PeriodClassName);
		this.period.setThemeName(LabelPaddingTheme);

		this.timeout = new IntegerField(Messages.getString("ConnectionForm.Timeout.Label")); //$NON-NLS-1$
		this.timeout.setSuffixComponent(new Span(Messages.getString("ConnectionForm.Timeout.Suffix"))); //$NON-NLS-1$
		this.timeout.addClassName(TimeoutClassName);
		this.timeout.setThemeName(LabelPaddingTheme);

		HorizontalLayout enabledAndPeriod = new HorizontalLayout(this.enabled, this.period, this.timeout);
		enabledAndPeriod.addClassName(CombinedClassName);

		this.url = new TextField(Messages.getString("ConnectionForm.URL.Label")); //$NON-NLS-1$
		this.url.setClassName(URLClassName);
		this.url.setThemeName(LabelPaddingTheme);
		this.url.addValueChangeListener(event -> { if (event.isFromClient()) updateResultingURL(); });
		this.binder.forField(this.url)
			.withValidator(urlStr ->
				{ // Explicit validator instance
					try {
						return !this.node.getResultingURL(urlStr).isBlank(); // dummy check - the actual error is thrown!
					} catch (Exception ex) {
						return false;
					}
				}, Messages.getString("ConnectionForm.Validator.URL.IllegalURL")) //$NON-NLS-1$
			.bind(Connection::getUrl, Connection::setUrl);

		this.resultingURL = new Label();
		this.resultingURL.setClassName(ResultingURLClassName);

		this.duration = new NumberField(Messages.getString("HTTPConnectionForm.Duration.Label")); //$NON-NLS-1$
		this.duration.setSuffixComponent(new Span(Messages.getString("HTTPConnectionForm.Duration.Suffix"))); //$NON-NLS-1$
		this.duration.setClassName(DurationClassName);
		this.duration.setThemeName(LabelPaddingTheme);
		this.duration.setStep(0.0001);
		this.binder.forField(this.duration).bind(ConnectionForm::getDurationInMillis, null); // nsec's --> msec's (rounded to micro-seconds)

		this.execute = new Button(Messages.getString("HTTPConnectionForm.Execute.Label")); //$NON-NLS-1$
		this.execute.setClassName(ExecuteClassName);
		this.binder.addStatusChangeListener(e -> this.execute.setEnabled(this.binder.isValid()));
		this.execute.addClickListener(event -> validateAndSave());

		// we also may have to update the resulting URL if any of the properties change:
		this.properties.addValueChangeListener(event ->
			{
				if (event.isFromClient()) {
					if (this.node != null) this.node.setPropertiesFromString(event.getValue());
				}
				updateResultingURL();
			});

		this.fields.add(enabledAndPeriod, this.url, this.resultingURL);
	}

	@Override
	public void setNode(N node) {
		super.setNode(node);
		updateResultingURL();
	}

	@Override
	protected void handleChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
		switch (propertyName) {
		case Connection.PROPERTYNAME_DURATION:
			this.duration.setValue(Connection.convertNanoSecsToMillis((Long)newValue));
			break;
		default:
			super.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
		}
	}

	protected void updateResultingURL() {
		try {
			String urlString;
			if (this.node != null) {
				urlString = this.node.getResultingURL(this.url.getValue());
				log.debug("updateResultingURL: {} -> {}", this.url.getValue(), urlString); //$NON-NLS-1$
				this.resultingURL.setText(String.format(Messages.getString("ConnectionForm.ResultingUrl.Value"), urlString)); //$NON-NLS-1$
				this.resultingURL.setVisible(!urlString.equals(this.url.getValue()));
			} else {
				log.debug("updateResultingURL: node==null"); //$NON-NLS-1$
				this.resultingURL.setText(Messages.getString("ConnectionForm.ResultingUrl.Undefined")); //$NON-NLS-1$
				this.resultingURL.setVisible(true);
			}
		} catch (Exception ex) {
			log.debug("updateResultingURL: Illegal URL: {} - reason: {}", ex, ex.getCause()); //$NON-NLS-1$
			this.resultingURL.setText(String.format(Messages.getString("ConnectionForm.ResultingUrl.IllegalSyntax"), ex)); //$NON-NLS-1$
			this.resultingURL.setVisible(true);
		}
	}

	public static Double getDurationInMillis(Connection n) {
		return Connection.convertNanoSecsToMillis(n.getDuration());
	}
}
