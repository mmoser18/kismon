/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import java.time.LocalDateTime;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.LeafNode;
import net.mmo.utils.kism.ui.UIConstants;
import net.mmo.utils.kism.ui.views.nodes.actions.ActionForm;

@SuppressWarnings("javadoc")
public abstract class LeafNodeForm <N extends LeafNode> extends ActionForm<N>
{
	private static final long serialVersionUID = 5772018540738340331L;

	static final String ActiveClassName = NodeFormField + "active"; //$NON-NLS-1$
	static final String PeriodClassName  = NodeFormField + "period"; //$NON-NLS-1$
	static final String TimeoutClassName = NodeFormField + "timeout"; //$NON-NLS-1$

	static final String ConnectionDetailsClassName = NodeFormField + "connection-details"; //$NON-NLS-1$

	static final String DurationClassName      = NodeFormField + "duration"; //$NON-NLS-1$
	static final String TimeStampClassName     = NodeFormField + "timestamp"; //$NON-NLS-1$

	static final String RequestDetailsClassName = NodeFormField + "request-details"; //$NON-NLS-1$

	protected Checkbox active;
	protected IntegerField period;
	protected IntegerField timeout;

	protected Accordion connnectionDetails;
	protected VerticalLayout connnectionDetailsPanel;

	protected TextField timestamp;
	protected NumberField duration;


	@Override
	public void init(Class<N> clazz, NodeService nodeService) {
		super.init(clazz, nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.active = new Checkbox(Messages.getString("LeafNodeForm.Active.Label")); //$NON-NLS-1$
		this.active.addClassName(ActiveClassName);
		this.active.setEnabled(this.isAdminUser);

		this.period = new IntegerField(Messages.getString("LeafNodeForm.Period.Label")); //$NON-NLS-1$
		this.period.setSuffixComponent(new Span(Messages.getString("LeafNodeForm.Period.Suffix"))); //$NON-NLS-1$
		this.period.addClassName(PeriodClassName);
		this.period.setThemeName(LabelPaddingTheme);
		this.period.setClearButtonVisible(this.clearButtonsVisible);
		this.period.setEnabled(this.isAdminUser);

		this.timeout = new IntegerField(Messages.getString("LeafNodeForm.Timeout.Label")); //$NON-NLS-1$
		this.timeout.setSuffixComponent(new Span(Messages.getString("LeafNodeForm.Timeout.Suffix"))); //$NON-NLS-1$
		this.timeout.addClassName(TimeoutClassName);
		this.timeout.setThemeName(LabelPaddingTheme);
		this.timeout.setClearButtonVisible(this.clearButtonsVisible);
		this.timeout.setEnabled(this.isAdminUser);

		this.duration = new NumberField(Messages.getString("LeafNodeForm.Duration.Label")); //$NON-NLS-1$
		this.duration.setSuffixComponent(new Span(Messages.getString("LeafNodeForm.Duration.Suffix"))); //$NON-NLS-1$
		this.duration.setClassName(DurationClassName);
		this.duration.setThemeName(LabelPaddingTheme);
		this.duration.setStep(0.0001);
		this.binder.forField(this.duration).bind(LeafNodeForm::getDurationInMillis, null); // nsec's --> msec's (rounded to micro-seconds)

		this.timestamp = new TextField(Messages.getString("LeafNodeForm.TimeStamp.Label")); //$NON-NLS-1$
		this.timestamp.setClassName(TimeStampClassName);
		this.timestamp.setThemeName(LabelPaddingTheme);

		this.binder.forField(this.timestamp)
			.withConverter(new Converter<String, LocalDateTime>()
				{
					private static final long serialVersionUID = 1659880075920218020L;
					@Override
					public Result<LocalDateTime> convertToModel(String value, ValueContext context) {
						try {
							return Result.ok(value == UIConstants.UNDEFINED_STRING ? null : LocalDateTime.parse(value, UIConstants.dateTimeFormatter)); // == is OK because it's a constant!
						} catch (Exception ex) {
							return Result.error("Illegal timestamp: '" + value + "'"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					@Override
					public String convertToPresentation(LocalDateTime value, ValueContext context) {
						return value != null ? value.format(UIConstants.dateTimeFormatter) : UIConstants.UNDEFINED_STRING;
					}
				})
			.bind(LeafNode::getTimestamp, null); // nsec's --> msec's (rounded to micro-seconds)

		HorizontalLayout activeAndPeriod = new HorizontalLayout(this.active, this.period, this.timeout);
		activeAndPeriod.addClassName(CombinedClassName);

		this.connnectionDetails = new Accordion();
		this.connnectionDetails.close(); // initially closed
		this.connnectionDetailsPanel = new VerticalLayout();
		this.connnectionDetailsPanel.setClassName(ConnectionDetailsClassName);
		this.connnectionDetails.add(Messages.getString("LeafNodeForm.ConnectionDetails.Label"), this.connnectionDetailsPanel); //$NON-NLS-1$

		this.fields.add(activeAndPeriod, this.connnectionDetails, this.executeButton);
	}

	// convenience method to convert duration in nano-seconds to milli-sec's
	public static Double convertDurationToMillis(long responseDuration) {
		return (responseDuration != 0 ? (Math.round(responseDuration/1000.0)*1.0e-3) : 0); // nsec's --> msec's (rounded to micro-seconds)
	}

	public static Double getDurationInMillis(LeafNode n) {
		return convertDurationToMillis(n.getDuration());
	}

	@Override
	protected void handleChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
		if (sourceNode == this.node) {
			switch (propertyName) {
			case LeafNode.PROPERTYNAME_RESPONSE_COMPLETE:
				/*if (!this.binderHasChanges())*/ readBean(sourceNode); // we (should) only update the form if the user hasn't changed anything, i.e. is not editing the form right now!
				break;
			default:
				break;
			}
		}
		super.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
	}

}
