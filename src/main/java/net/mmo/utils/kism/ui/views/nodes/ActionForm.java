/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 20 Mar 2022
 */

package net.mmo.utils.kism.ui.views.nodes;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.entities.nodes.actions.ActionCheck;
import net.mmo.utils.kism.entities.nodes.actions.ActionFactory;
import net.mmo.utils.kism.entities.nodes.actions.ActionFactory.ActionType;
import net.mmo.utils.kism.entities.nodes.actions.IAction;

/**
 * extends NodeForem with Action-Details
 * @param <N>
 */
@Slf4j
public abstract class ActionForm <N extends ActionableNode> extends NodeForm<N>
{
	private static final long serialVersionUID = 7104996696515821143L;

	static final String ActionClassName = NodeFormField + "action"; //$NON-NLS-1$
	static final String ActionDetailsClassName = NodeFormField + "action-details"; //$NON-NLS-1$
	static final String DoActionClassName = NodeFormField + "doAction"; //$NON-NLS-1$
	static final String ThresholdClassName = NodeFormField + "threshold"; //$NON-NLS-1$
	static final String ActionTypeClassName = NodeFormField + "actionType"; //$NON-NLS-1$
	static final String ActionParameterClassName = NodeFormField + "actionParameter"; //$NON-NLS-1$

	protected Accordion actionDetails;

	protected VerticalLayout actionDetailsPanel;
	protected Checkbox doAction;

	protected TextField[] triggerThreshold = new TextField[State.values().length];

	protected Select<ActionFactory.ActionType> actionType;

	protected Validator<String> thresholdValidator;

	@Override
	protected void appendFurtherFields(NodeService nodeService) {
		this.actionDetails = new Accordion();
		this.actionDetails.close(); // initially closed
		this.actionDetailsPanel = new VerticalLayout();
		this.actionDetailsPanel.setClassName(ActionDetailsClassName);
		this.actionDetails.add(Messages.getString("ActionCheckForm.ActionDetails.Label"), this.actionDetailsPanel); //$NON-NLS-1$

		this.doAction = new Checkbox(Messages.getString("ActionCheckForm.DoAction.Label")); //$NON-NLS-1$
		this.doAction.setClassName(DoActionClassName);
		this.doAction.setEnabled(this.isAdminUser);
		this.doAction.addValueChangeListener(event ->
		{
			log.debug("checkResults value changed from {} to {} (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
			if (event.isFromClient()) {
				boolean visible = event.getValue();
				if (visible) {
					ActionCheck ac = this.node.getActionCheck();
					if (ac == null) { // create a new ActionCheck
						ac = new ActionCheck();
						this.node.setActionCheck(ac);
					}
					for (State state: State.values()) {
						log.info("actionCheck: {}", ac); //$NON-NLS-1$
						this.triggerThreshold[state.ordinal()].setValue(ac.getTriggerThresholds()[state.ordinal()]);
					}
					IAction<ActionableNode> action = ac.getAction();
					this.actionType.setValue(ActionFactory.getActionType(action));
				} else {
					// We don't immediately remove the actionCheck in case the user changes his mind.
					// this.node.setActionCheck(null);
					// instead we do the cleanup in the validateAndSave
				}
				adaptActionCheckParametersVisibility(visible);
			}
		});

		for (State state: State.values()) {
			TextField tf = new TextField();
			tf.setLabel(Messages.getString("ActionCheckForm.Threshold." + state.name() + ".Label")); //$NON-NLS-1$ //$NON-NLS-2$
			tf.setClassName(ThresholdClassName);
			tf.setEnabled(this.isAdminUser);
			this.binder.forField(tf)
				.withValidator(this.thresholdValidator)
				.bind((n) -> getThreshold(n, state), (n, value) -> setThreshold(n, state, value));
			this.triggerThreshold[state.ordinal()] = tf;
		}
		HorizontalLayout thresholds = new HorizontalLayout(this.triggerThreshold);

		this.actionType = new Select<ActionType>(ActionFactory.ActionType.values());
		this.actionType.setLabel(Messages.getString("ActionCheckForm.ActionType.Label")); //$NON-NLS-1$
//		this.actionType.setItemLabelGenerator(action -> (action != null
//                        ? Messages.getString("ActionCheckForm.ActionType.Value." + action.name())  //$NON-NLS-1$
//                        : Messages.getString("ActionCheckForm.ActionType.Value.None"))); //$NON-NLS-1$
		this.actionType.setEmptySelectionAllowed(true);
		this.actionType.setEmptySelectionCaption(Messages.getString("ActionCheckForm.ActionType.Value.None")); //$NON-NLS-1$
		this.actionType.setItemLabelGenerator(action -> (action != null
		                                                ? Messages.getString("ActionCheckForm.ActionType.Value." + action.name()) //$NON-NLS-1$
		                                                : Messages.getString("ActionCheckForm.ActionType.Value.None"))); //$NON-NLS-1$
		this.actionType.addClassName(ActionClassName);
		this.actionType.setEnabled(this.isAdminUser);
		this.binder.forField(this.actionType).bind(ActionForm::getActionType, ActionForm::setActionType);
		this.actionType.addValueChangeListener(event ->
		{
			log.debug("condition value changed from {} to {} (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
			if (event.isFromClient()) {
				adaptActionParametersVisibility(this.doAction.getValue(), this.node);
			}
		});
		this.actionDetailsPanel.add(this.doAction, thresholds, this.actionType);
		this.fields.add(this.actionDetails);

		adaptActionCheckParametersVisibility(this.node != null && this.node.getActionCheck() != null);
		super.appendFurtherFields(nodeService);
	}

	@Override
	protected void createValidators() {
		super.createValidators();
		this.thresholdValidator = new Validator<String>()
		{
			private static final long serialVersionUID = -476108409332944491L;

			@Override
			public ValidationResult apply(String value, ValueContext context) {
				if (ActionForm.this.node != null) {
					ActionCheck ac = ActionForm.this.node.getActionCheck();
					if (ac == null // nothing to check...
					    || ac.getTriggerThresholds() == null // no thresholds
					    || ac.getAction() == null) { // no action - thresholds are irrelevant and will be removed
						return ValidationResult.ok();
					}
					return ActionForm.this.positiveNumberValidator.apply(value, context);
				}
				return ValidationResult.ok(); // if there is no node set, yet - we assume it's valid
			}
		};
	}

	private String getThreshold(N n, State state) {
		ActionCheck ac = n.getActionCheck();

		return ac != null ? ac.getTriggerThresholds()[state.ordinal()] : Messages.getString("ActionCheckForm.Threshold.Value.Undefined"); //$NON-NLS-1$
	}
	private void setThreshold(N n, State state, String value) {
		ActionCheck ac = n.getActionCheck();
		if (ac != null) {
			ac.getTriggerThresholds()[state.ordinal()] = value;
		} else {
			log.error("This should never happen - setThreshold called with value {} for state {} for non-existent ActionCheck in node {}", //$NON-NLS-1$
			          value, state, n);
		}
	}

	static <N extends ActionableNode> ActionType getActionType(N n) {
		try {
			if (n != null && n.getActionCheck() != null) {
				return ActionFactory.getActionType(n.getActionCheck().getAction());
			}
		} catch (Exception ex) {
			log.error("Error deriving action-type from action", ex); //$NON-NLS-1$
		}
		return null;
	}

	static <N extends ActionableNode> void setActionType(N n, ActionType actionType) {
		if (n != null && n.getActionCheck() != null) {
			try {
				n.getActionCheck().setAction(actionType != null ? ActionFactory.createActionOfType(actionType) : null);
			} catch (Exception ex) {
				log.error("Error setting action from type '" + actionType + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private void adaptActionCheckParametersVisibility(boolean visible) {
		for (State state: State.values()) {
			this.triggerThreshold[state.ordinal()].setVisible(visible);
		}
		this.actionType.setVisible(visible);
	}

	private void adaptActionParametersVisibility(boolean visible, N n) {
		// TODO: to be completed
	}

	/**
	 * If the use has selected "None" action when saving then we reset the doAction to false
	 */
	@Override
	protected void validateAndSave() {
		if (this.node != null) {
			if (!this.doAction.getValue() || this.actionType.getValue() == null) {
				this.doAction.setValue(false);
				this.node.setActionCheck(null);
			}
		}
		super.validateAndSave();
	}
}
