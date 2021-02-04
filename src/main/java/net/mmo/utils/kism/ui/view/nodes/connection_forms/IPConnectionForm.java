/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.ResultChecker;
import net.mmo.utils.kism.entities.ResultChecker.Condition;
import net.mmo.utils.kism.entities.connections.IPConnection;
import net.mmo.utils.kism.ui.UIConstants;
import net.mmo.utils.kism.ui.view.nodes.NodeForm;
import net.mmo.utils.kism.ui.view.nodes.NodeView;
import net.mmo.utils.kism.utils.AppProperties;

@Slf4j
public abstract class IPConnectionForm <N extends IPConnection> extends NodeForm<N>
{
	private static final long serialVersionUID = 5772018540738340331L;

	static final String ActiveClassName = NodeFormField + "active"; //$NON-NLS-1$
	static final String PeriodClassName  = NodeFormField + "period"; //$NON-NLS-1$
	static final String TimeoutClassName = NodeFormField + "timeout"; //$NON-NLS-1$

	static final String ConnectionDetailsClassName = NodeFormField + "connection-details"; //$NON-NLS-1$
	static final String URLClassName           = NodeFormField + "url"; //$NON-NLS-1$
	static final String ResultingURLClassName  = NodeFormField + "resulting-url"; //$NON-NLS-1$

	static final String ExecuteClassName       = NodeFormField + "execute"; //$NON-NLS-1$
	static final String DurationClassName      = NodeFormField + "duration"; //$NON-NLS-1$
	static final String TimeStampClassName     = NodeFormField + "timestamp"; //$NON-NLS-1$

	static final String ValidationDetailsClassName = NodeFormField + "validation-details"; //$NON-NLS-1$
	static final String ResultCheckerClassName = NodeFormField + "resultChecker"; //$NON-NLS-1$
	static final String ConditionClassName = NodeFormField + "condition"; //$NON-NLS-1$
	static final String OperandClassName = NodeFormField + "operand"; //$NON-NLS-1$

	protected Checkbox active;
	protected IntegerField period;
	protected IntegerField timeout;

	protected Accordion connnectionDetails;
	protected VerticalLayout connnectionDetailsPanel;

	protected TextField url;
	protected TextField resultingURL;

	// these are not yet added to the form to allow subclasses to include them at appropriate places:
	protected Button execute;
	protected TextField timestamp;
	protected NumberField duration;

	protected Accordion validationDetails;
	protected VerticalLayout validationDetailsPanel;

	protected DateTimeFormatter dateTimeFormatter =
		DateTimeFormatter.ofPattern(AppProperties.getProperties().getProperty("LocalDateTimeStamp.Format", //$NON-NLS-1$
		                                                                      "yyyy-MM-dd HH:mm:ss.SSS")); //$NON-NLS-1$

	protected Checkbox checkResults;
	// protected ResultCheckerForm resultChecker;

	protected Select<ResultChecker.Condition> condition;
	protected TextArea operand;


	public IPConnectionForm(Class<N> clazz) {
		super(clazz);
	}

	@Override
	protected void createFormFields() {
		super.createFormFields();

		this.active = new Checkbox(Messages.getString("IPConnectionForm.Active.Label")); //$NON-NLS-1$
		this.active.addClassName(ActiveClassName);

		this.period = new IntegerField(Messages.getString("IPConnectionForm.Period.Label")); //$NON-NLS-1$
		this.period.setSuffixComponent(new Span(Messages.getString("IPConnectionForm.Period.Suffix"))); //$NON-NLS-1$
		this.period.addClassName(PeriodClassName);
		this.period.setThemeName(LabelPaddingTheme);

		this.timeout = new IntegerField(Messages.getString("IPConnectionForm.Timeout.Label")); //$NON-NLS-1$
		this.timeout.setSuffixComponent(new Span(Messages.getString("IPConnectionForm.Timeout.Suffix"))); //$NON-NLS-1$
		this.timeout.addClassName(TimeoutClassName);
		this.timeout.setThemeName(LabelPaddingTheme);

		this.url = new TextField(Messages.getString("IPConnectionForm.URL.Label")); //$NON-NLS-1$
		this.url.setClassName(URLClassName);
		this.url.setThemeName(LabelPaddingTheme);
		this.url.addValueChangeListener(event ->
			{
				log.info("url value changed to from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
				if (event.isFromClient()) {
					try {
						updateResultingUrl(event.getValue());
					} catch (Exception ex) {
						log.error("Error updating URL", ex); //$NON-NLS-1$
					}
				}
			});
		this.binder.forField(this.url)
			.withValidator((value, context) ->
				{ // Explicit validator instance
					log.trace("validating url: '{}'", value); //$NON-NLS-1$
					try {
						if (this.node != null) {
							// we check whether we can resolve the url and convert it to a valid URL:
							URI uri = new URI(this.node.resolveProperties(value));
							// no check necessary - any error is thrown!
							log.debug("validation succeeded: '{}' => '{}'", value, uri); //$NON-NLS-1$
						} else {
							log.debug("node == null: validation succeeded"); //$NON-NLS-1$
						}
						return ValidationResult.ok();
					} catch (Exception ex) {
						log.debug("validation failed: '{}' -> {}", value, ex); //$NON-NLS-1$
						return ValidationResult.error(String.format(Messages.getString("IPConnectionForm.URL.ValidatorErrorMsg"), //$NON-NLS-1$
						                                            value, ex));
					}
				})
			.bind(IPConnection::getUrl, IPConnection::setUrl);

		this.resultingURL = new TextField(Messages.getString("IPConnectionForm.ResultingUrl.Value")); //$NON-NLS-1$
		this.resultingURL.setClassName(ResultingURLClassName);
		this.resultingURL.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.resultingURL).bind(IPConnection::getResultingUrl, null);

		this.duration = new NumberField(Messages.getString("HTTPConnectionForm.Duration.Label")); //$NON-NLS-1$
		this.duration.setSuffixComponent(new Span(Messages.getString("HTTPConnectionForm.Duration.Suffix"))); //$NON-NLS-1$
		this.duration.setClassName(DurationClassName);
		this.duration.setThemeName(LabelPaddingTheme);
		this.duration.setStep(0.0001);
		this.binder.forField(this.duration).bind(IPConnectionForm::getDurationInMillis, null); // nsec's --> msec's (rounded to micro-seconds)

		this.timestamp = new TextField(Messages.getString("HTTPConnectionForm.TimeStamp.Label")); //$NON-NLS-1$
		this.timestamp.setClassName(TimeStampClassName);
		this.timestamp.setThemeName(LabelPaddingTheme);
		String languageTag = Messages.getString("TimeStamp.Locale"); //$NON-NLS-1$
		Locale locale = null;
		try {
			locale = Locale.forLanguageTag(languageTag);
			log.debug("Locale for '{}': '{}'", languageTag, locale); //$NON-NLS-1$
		} catch (Exception ex) {
			log.debug("Error converting {} to locale! Availabel locales are: {}", languageTag, Locale.getAvailableLocales()); //$NON-NLS-1$
		}

		this.binder.forField(this.timestamp)
			.withConverter(new Converter<String, LocalDateTime>()
				{
					private static final long serialVersionUID = 1659880075920218020L;
					@Override
					public Result<LocalDateTime> convertToModel(String value, ValueContext context) {
						try {
							return Result.ok(value == UIConstants.UNDEFINED_STRING ? null : LocalDateTime.parse(value, IPConnectionForm.this.dateTimeFormatter)); // == is OK because it's a constant!
						} catch (Exception ex) {
							return Result.error("Illegal timestamp: '" + value + "'"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					@Override
					public String convertToPresentation(LocalDateTime value, ValueContext context) {
						return value != null ? value.format(IPConnectionForm.this.dateTimeFormatter) : UIConstants.UNDEFINED_STRING;
					}
				})
			.bind(IPConnection::getTimestamp, null); // nsec's --> msec's (rounded to micro-seconds)

		this.execute = new Button(Messages.getString("HTTPConnectionForm.Execute.Label")); //$NON-NLS-1$
		this.execute.setClassName(ExecuteClassName);
		this.execute.setThemeName(LabelPaddingTheme);
		this.binder.addStatusChangeListener(event ->
			{
				log.info("binder statusChange: {} / {}", event, event.getSource().getStatusLabel()); //$NON-NLS-1$
				this.execute.setText(Messages.getString(this.binder.hasChanges()
				                                        ? "HTTPConnectionForm.SaveAndExecute.Label" //$NON-NLS-1$
				                                        : "HTTPConnectionForm.Execute.Label")); //$NON-NLS-1$
				boolean isValid = binderIsValid();
				log.debug("binder status change -> [execute] active:{}", isValid); //$NON-NLS-1$
				this.execute.setEnabled(isValid);
			});

		this.execute.addClickListener(event ->
			{
				log.info("execute clicked"); //$NON-NLS-1$
				if (binderHasChanges()) validateAndSave();
				try {
					this.node.executeRequest();
				} catch (Throwable t) {
					NodeView.ExecutionError("manually triggered execution", t); //$NON-NLS-1$
				}
			});

		this.checkResults = new Checkbox(Messages.getString("IPConnectionForm.CheckResult.Label")); //$NON-NLS-1$
		this.checkResults.setClassName(ActiveClassName);
		this.checkResults.addValueChangeListener(event ->
			{
				log.debug("checkResults value changed from {} to {} (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
				if (event.isFromClient()) {
					adaptResultCheckerVisibility(event.getValue(), this.node);
				}
			});

//		this.resultChecker = new ResultCheckerForm();
//		this.resultChecker.setClassName(ResultCheckerClassName);
		this.condition = new Select<Condition>(Condition.values());
		this.condition.setLabel(Messages.getString("ResultChecker.Condition.Label")); //$NON-NLS-1$
		this.condition.setItemLabelGenerator(cond -> Messages.getString("ResultChecker.Condition.Value." + cond.name())); //$NON-NLS-1$
		this.condition.addClassName(ConditionClassName);

		this.operand = new TextArea(Messages.getString("ResultChecker.Operand.Label")); //$NON-NLS-1$
		this.operand.addClassName(OperandClassName);
		this.operand.setThemeName(UIConstants.LabelPaddingTheme);

		this.binder.forField(this.condition).bind(IPConnectionForm::getCondition, IPConnectionForm::setCondition);
		this.binder.forField(this.operand)
			.withValidator(this.propertiesResolvable)
			.bind(IPConnectionForm::getOperand, IPConnectionForm::setOperand);

		HorizontalLayout activeAndPeriod = new HorizontalLayout(this.active, this.period, this.timeout);
		activeAndPeriod.addClassName(CombinedClassName);

		this.connnectionDetails = new Accordion();
		this.connnectionDetails.close(); // initially closed
		this.connnectionDetailsPanel = new VerticalLayout();
		this.connnectionDetailsPanel.setClassName(ConnectionDetailsClassName);
		this.connnectionDetails.add(Messages.getString("IPConnectionForm.ConnectionDetails.Label"), this.connnectionDetailsPanel); //$NON-NLS-1$
		this.connnectionDetailsPanel.add(this.url, this.resultingURL);

		this.validationDetails = new Accordion();
		this.validationDetails.close(); // initially closed
		this.validationDetailsPanel = new VerticalLayout();
		this.validationDetailsPanel.setClassName(ValidationDetailsClassName);
		this.validationDetails.add(Messages.getString("ResultChecker.ValidationDetails.Label"), this.validationDetailsPanel); //$NON-NLS-1$
		this.validationDetailsPanel.add(this.checkResults, this.condition, this.operand);

		this.fields.add(activeAndPeriod, this.connnectionDetails, this.execute);
	}

	@Override
	public boolean setNode(N node) {
		log.info("setNode node={}", node); //$NON-NLS-1$
		boolean res = super.setNode(node);
		adaptResultCheckerVisibility((node != null ? node.isCheckResults() : false), node);
		return res;
	}

//	// when the main-form's bean is read we also need to read the ResultChecker's data (if set):
//	@Override
//	protected void readBean(N n) {
//		log.info("readBean node={}", n); //$NON-NLS-1$
//		super.readBean(n);
//		if (n != null) {
//			ResultChecker rc = n.getResultChecker();
//			if (rc != null) {
//				Binder<ResultChecker> rcBinder = this.resultChecker.getBinder();
//				rcBinder.readBean(rc);
//			}
//		}
//	}
//
//	// when the main-form's bean is to be written we also first need to write the ResultChecker's data (if set):
//	@Override
//	protected void writeBean(N n) throws ValidationException {
//		log.info("writeBean node={}", n); //$NON-NLS-1$
//		if (n != null) {
//			ResultChecker rc = n.getResultChecker();
//			if (rc != null) {
//				Binder<ResultChecker> rcBinder = this.resultChecker.getBinder();
//				rcBinder.writeBean(rc);
//			}
//		}
//		super.writeBean(n);
//	}
//
//	@Override
//	protected boolean binderHasChanges() {
//		if (this.node != null) {
//			ResultChecker rc = this.node.getResultChecker();
//			if (rc != null) {
//				Binder<ResultChecker> rcBinder = this.resultChecker.getBinder();
//				if (rcBinder.hasChanges()) {
//					log.debug("binderHasChanges{}: true"); //$NON-NLS-1$
//					return true;
//				}
//			}
//		}
//		return super.binderHasChanges();
//	}
//
//	// We need to prevent bean validation when there is currently no bean set or else we get
//	// another one of these dreaded IllegalStateException. I don't why this is not done by default :-(
//	@Override
//	protected boolean binderIsValid() {
//		if (this.node != null) {
//			ResultChecker rc = this.node.getResultChecker();
//			if (rc != null) {
//				Binder<ResultChecker> rcBinder = this.resultChecker.getBinder();
//				if (rcBinder.getBean() != null && !rcBinder.isValid()) {
//					log.debug("binderIsValid{}: false"); //$NON-NLS-1$
//					return false;
//				}
//			}
//		}
//		return super.binderIsValid();
//	}

	static <N extends IPConnection> ResultChecker.Condition getCondition(N n) {
		return (n != null && n.getResultChecker() != null
				? n.getResultChecker().getCondition()
				: null);
	}

	static <N extends IPConnection> void setCondition(N n, ResultChecker.Condition condition) {
		if (n != null && n.getResultChecker() != null) {
			n.getResultChecker().setCondition(condition);
		}
	}

	static <N extends IPConnection> String getOperand(N n) {
		return (n != null && n.getResultChecker() != null
				? n.getResultChecker().getOperand()
				: null);
	}

	static <N extends IPConnection> void setOperand(N n, String operand) {
		if (n != null && n.getResultChecker() != null) {
			n.getResultChecker().setOperand(operand);
		}
	}


	private void adaptResultCheckerVisibility(boolean check, N n) {
		if (n != null) {
			if (n.getResultChecker() == null) {
				log.info("creating new ResultChecker:"); //$NON-NLS-1$
				n.setResultChecker(new ResultChecker());
//				// if checkResults is active we need to add the validity of the resultChecker's binder to this one:
//				this.binder.withValidator(nn -> (checkResults ? this.resultChecker.getBinder().isValid() : true),
//				                          "ResultCheck not valid"); //$NON-NLS-1$
			}
//			this.resultChecker.setResultChecker(n.getResultChecker());
//			this.resultChecker.setVisible(check);
			this.condition.setVisible(check);
			this.operand.setVisible(check);
		} else { // we leave the resultChecker attached to the node - in case the user clicks the check-button again.
			log.info("removing ResultChecker from view:"); //$NON-NLS-1$
//			this.resultChecker.setResultChecker(null);
//			this.resultChecker.setVisible(false);
			this.condition.setVisible(false);
			this.operand.setVisible(false);
		}
	}

	@Override
	protected void updateResolvableValues() {
		super.updateResolvableValues();
		updateResultingUrl(this.node != null ? this.node.getUrl() : null);
	}

	protected void updateResultingUrl(String newValue) {
		updateResultingField("updateResultingURL", //$NON-NLS-1$
		                     this.node,
		                     newValue,
		                     IPConnection::setResultingUrl,
		                     this.resultingURL
		                    );
	}

	public static Double getDurationInMillis(IPConnection n) {
		return IPConnection.convertDurationToMillis(n.getDuration());
	}

	@Override
	protected void handleChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
		switch (propertyName) {
		case IPConnection.PROPERTYNAME_RESPONSE_COMPLETE:
			if (!this.binderHasChanges()) readBean(sourceNode); // we only update the form if the user hasn't changed anything, i.e. is editing the form right now!
			break;
		default:
			break;
		}
		super.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
	}

}
