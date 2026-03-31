/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes.actions;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.entities.nodes.actions.ActionCheck;
import net.mmo.utils.kism.entities.nodes.actions.IAction;
import net.mmo.utils.kism.ui.UIConstants;
import net.mmo.utils.kism.ui.views.nodes.NodeForm;
import net.mmo.utils.kism.ui.views.nodes.actions.ActionFactory.ActionType;
/**
 * extends NodeForm with Action-Details
 * @param <N>
 */
public abstract class ActionForm <N extends ActionableNode> extends NodeForm<N>
{
	private static final long serialVersionUID = 7104996696515821143L;

	static final String ActionClassName = NodeFormField + "action-"; //$NON-NLS-1$
	static final String ActionDetailsClassName = ActionClassName + "Details"; //$NON-NLS-1$
	static final String ActionEnabledClassName = ActionClassName + "Enabled"; //$NON-NLS-1$
	static final String ActionThresholdsClassName = ActionClassName + "Thresholds"; //$NON-NLS-1$
	static final String ActionThresholdClassName = ActionClassName + "Threshold"; //$NON-NLS-1$

	static final String ActionStateResetAndTriggerClassName = ActionClassName + "StateResetAndTrigger"; //$NON-NLS-1$
	static final String ActionSameStateCounterClassName = ActionClassName + "SameStateCounter"; //$NON-NLS-1$
	static final String ActionResetStateCounterClassName = ActionClassName + "ResetStateCounter"; //$NON-NLS-1$

	static final String ActionTriggerClassName = ActionClassName + "TriggerAction"; //$NON-NLS-1$

	static final String ActionTypeClassName = ActionClassName + "Type"; //$NON-NLS-1$

	static final String ActionParametersClassName = ActionClassName + "Parameters"; //$NON-NLS-1$

	protected Checkbox actionEnabled;
	protected Accordion actionDetails;
	protected VerticalLayout actionDetailsPanel;

	HorizontalLayout triggerThresholds;

	protected TextField[] triggerThresholdFields = new TextField[State.values().length];
	protected Validator<String> thresholdValidator;
	protected Select<ActionFactory.ActionType> actionType;

	HorizontalLayout stateResetAndTrigger;
	protected IntegerField sameStateSince;
	protected Button resetSameStateCounterButton;
	protected Button triggerActionButton;

	protected ActionDetailsForm actionDetailsForm; // Note: this must also be a Component!
	private ActionDetailsFormFactory<N> actionDetailsFormFactory = new ActionDetailsFormFactory<>();

	@Override
	protected void appendFurtherFields(NodeService nodeService) {
		this.actionDetails = new Accordion();
		this.actionDetails.close(); // initially closed
		this.actionDetailsPanel = new VerticalLayout();
		this.actionDetailsPanel.setClassName(ActionDetailsClassName);

		this.actionDetails.add(Messages.getString("ActionForm.ActionDetails.Label"), this.actionDetailsPanel); //$NON-NLS-1$

		this.actionEnabled = new Checkbox(Messages.getString("ActionForm.ActionEnabled.Label")); //$NON-NLS-1$
		this.actionEnabled.setClassName(ActionEnabledClassName);
		this.actionEnabled.setEnabled(this.isAdminUser);
		this.actionEnabled.addValueChangeListener(event ->
		{
			this.log.debug("actionEnabled value changed from {} to {} (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
			if (event.isFromClient()) {
				boolean visible = event.getValue();
				if (this.node != null) {
					if (visible) {
						ActionCheck ac = this.node.getActionCheck();
						if (ac == null) { // create a new ActionCheck
							ac = new ActionCheck();
							this.node.setActionCheck(ac);
						}
						for (State state: State.values()) {
							this.log.debug("threshold[{}]: {}", state, ac); //$NON-NLS-1$
							this.triggerThresholdFields[state.ordinal()].setValue(ac.getTriggerThresholds()[state.ordinal()]);
						}
						this.actionType.setValue(ActionFactory.getActionType(this.node));
					} else {
						// We don't immediately remove the actionCheck in case the user changes his mind.
						// this.node.setActionCheck(null);
						// ... instead we do the cleanup in the validateAndSave
					}
				}
				adaptActionType(visible, this.node);
			}
		});

		for (State state: State.values()) {
			TextField tf = new TextField();
			tf.setLabel(Messages.getString("ActionForm.Threshold." + state.name() + ".Label")); //$NON-NLS-1$ //$NON-NLS-2$
			tf.setClassName(ActionThresholdClassName);
			tf.setThemeName(UIConstants.LabelPaddingTheme);
			tf.setEnabled(this.isAdminUser);
			this.binder.forField(tf)
				.withValidator(this.thresholdValidator)
				.bind((n) -> getThreshold(n, state), (n, value) -> setThreshold(n, state, value));
			this.triggerThresholdFields[state.ordinal()] = tf;
		}
		this.triggerThresholds = new HorizontalLayout(this.triggerThresholdFields);
		this.triggerThresholds.setClassName(ActionThresholdsClassName);

		this.stateResetAndTrigger = new HorizontalLayout();
		this.stateResetAndTrigger.setClassName(ActionStateResetAndTriggerClassName);

		this.sameStateSince = new IntegerField(Messages.getString("ActionForm.StateCounter.Label")); //$NON-NLS-1$
		this.sameStateSince.addClassName(ActionSameStateCounterClassName);

		this.resetSameStateCounterButton = new Button(Messages.getString("ActionForm.ResetStateCounter.Label")); //$NON-NLS-1$
		this.resetSameStateCounterButton.addClassName(ActionResetStateCounterClassName);
		this.resetSameStateCounterButton.addClickListener(_ ->
		{
			if (this.node != null) {
				this.node.setSameStateSince(0);
				ActionCheck actionCheck = this.node.getActionCheck();
				if (actionCheck != null) actionCheck.setPreviousActionState(null);
			}
		});

		this.triggerActionButton = new Button(Messages.getString("ActionForm.TriggerAction.Label")); //$NON-NLS-1$
		this.triggerActionButton.addClassName(ActionTriggerClassName);
		this.triggerActionButton.addClickListener(_ ->
		{
			if (this.node != null) {
				IAction<ActionableNode> action = this.node.getAction();
				try {
					action.doAction(this.node, this.node.getState(), null);
				} catch (Exception ex) {
					this.log.error("error executing action " + action, ex); //$NON-NLS-1$
					NodeService.createNotification(String.format(Messages.getString("ActionForm.TriggerActionError.Message"), ex.getMessage())); //$NON-NLS-1$
				}
			}
		});
		this.stateResetAndTrigger.add(this.sameStateSince, this.resetSameStateCounterButton, this.triggerActionButton);

		this.actionType = new Select<ActionType>();
		this.actionType.setLabel(Messages.getString("ActionForm.ActionType.Label")); //$NON-NLS-1$
		this.actionType.setItems(ActionFactory.ActionType.values());
		this.actionType.setEmptySelectionAllowed(true);
		this.actionType.setEmptySelectionCaption(Messages.getString("ActionForm.ActionType.Value.None")); //$NON-NLS-1$
		this.actionType.setItemLabelGenerator(at -> (at != null
		                                            ? Messages.getString("ActionForm.ActionType.Value." + at.name()) //$NON-NLS-1$
		                                            : Messages.getString("ActionForm.ActionType.Value.None"))); //$NON-NLS-1$
		this.actionType.addClassName(ActionClassName);
		this.actionType.setEnabled(this.isAdminUser);
		this.binder.forField(this.actionType).bind((n) -> getActionType(n), (n, value) -> setActionType(n, value));
		this.actionType.addValueChangeListener(event ->
		{
			this.log.debug("action type value changed from {} to {} (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
			if (event.isFromClient()) {
				ActionType at = event.getValue();
				try {
					this.node.setAction(ActionFactory.createActionOfType(at));
				} catch (Exception ex) {
					this.log.error("Error creating action", ex); //$NON-NLS-1$
				}
				adaptActionDetailsVisibility(this.actionEnabled.getValue(), this.node, at);
			}
		});

		this.actionDetailsPanel.add(this.actionEnabled, this.triggerThresholds, this.stateResetAndTrigger, this.actionType);
		adaptActionType(hasAction(this.node), this.node);

		this.fields.add(this.actionDetails);
		super.appendFurtherFields(nodeService);
	}

	private boolean hasAction(N n) {
		return n != null && n.getAction() != null;
	}
	@Override
	protected void createValidators() {
		super.createValidators();
		this.thresholdValidator = new Validator<String>()
		{
			private static final long serialVersionUID = -476108409332944491L;

			@SuppressWarnings("synthetic-access")
			@Override
			public ValidationResult apply(String value, ValueContext context) {
				if (ActionForm.this.node != null) {
					ActionCheck ac = ActionForm.this.node.getActionCheck();
					if (ac == null // nothing to check...
					   || ac.getTriggerThresholds() == null // ... or no triggerThresholds ...
					   || ActionForm.this.node.getAction() == null) { // ... or no action: triggerThresholds are irrelevant (and will be removed on save)
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

		return ac != null ? ac.getTriggerThresholds()[state.ordinal()] : Messages.getString("ActionForm.Threshold.Value.Undefined"); //$NON-NLS-1$
	}
	private void setThreshold(N n, State state, String value) {
		ActionCheck ac = n.getActionCheck();
		if (ac != null) {
			ac.getTriggerThresholds()[state.ordinal()] = value;
		} else if (!value.equals(Messages.getString("ActionForm.Threshold.Value.Undefined"))) { //$NON-NLS-1$
			this.log.error("This should never happen - setThreshold called with value {} for state {} for non-existent ActionCheck in node {}", //$NON-NLS-1$
			               value, state, n);
		}
	}

	/**
	 * @param n
	 * @return the actionType of the passed node
	 */
	public ActionType getActionType(N n) {
		try {
			if (n != null) {
				return ActionFactory.getActionType(n);
			}
		} catch (Exception ex) {
			this.log.error("Error deriving action-type from action", ex); //$NON-NLS-1$
		}
		return null;
	}

	void setActionType(N n, ActionType actionType) {
		if (n != null && n.getActionCheck() != null) {
			try {
				if (getActionType(n) != actionType) {
					n.setAction(actionType != null ? ActionFactory.createActionOfType(actionType) : null);
				}
			} catch (Exception ex) {
				this.log.error("Error setting action from type '" + actionType + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	protected void adaptActionType(boolean actionVisible, N n) {
		this.log.debug("adaptActionType(visible:{},n:{})",actionVisible, n); //$NON-NLS-1$
		this.actionEnabled.setValue(actionVisible);
		this.actionType.setVisible(actionVisible);
		this.triggerThresholds.setVisible(actionVisible);
		this.stateResetAndTrigger.setVisible(actionVisible);
		adaptActionDetailsVisibility(actionVisible, n, ActionFactory.getActionType(n));
	}

	private void adaptActionDetailsVisibility(boolean visible, N n, ActionType at) {
		ActionDetailsForm newActionDetailsForm = this.actionDetailsFormFactory.createActionDetailsForm(at);

		this.log.debug("adaptActionDetailsVisibility: form={}", newActionDetailsForm); //$NON-NLS-1$
		if (this.actionDetailsForm != newActionDetailsForm) { // we got a new/different form: "disconnect" the old action form:
			this.log.debug("got different form: new:{} (old:{})", newActionDetailsForm, this.actionDetailsForm); //$NON-NLS-1$
			if (this.actionDetailsForm != null) {
				this.actionDetailsForm.setAction(null);
				Component comp = (Component)this.actionDetailsForm;
				comp.setVisible(false);
				// disconnect from component tree:
				this.actionDetailsPanel.remove(comp);
				comp.getElement().removeFromTree(); // make sure there is no more reference into the active tree
				this.actionDetailsForm = null;
			}
		}
		if (newActionDetailsForm != null) { // then connect the new one action form:
			Binder<IAction<ActionableNode>> actionBinder = newActionDetailsForm.getBinder();
			if (actionBinder != null) {
				actionBinder.addValueChangeListener(event ->
				{
					if (event.isFromClient()) {
						this.log.debug("actionBinder:valueChangeEvent from Client:"); //$NON-NLS-1$
						setUiChanges(true);
					}
				});
			}
			newActionDetailsForm.setAction(n.getAction());
			this.actionDetailsForm = newActionDetailsForm;
			// connect to component tree:
			Component comp = (Component)this.actionDetailsForm;
			this.actionDetailsPanel.add(comp);
			comp.setVisible(visible);
		}
	}

	@Override
	public boolean setNode(N n, boolean abandonChanges) {
		boolean res = super.setNode(n, abandonChanges);
		adaptActionType(hasAction(n), n);

		if (this.actionDetailsForm != null) {
			IAction<ActionableNode> action = (n != null ? n.getAction() : null);
			this.actionDetailsForm.setAction(action);
		}
		return res;
	}

	@Override
	protected void readBean(N n) {
		if (n != null && this.actionDetailsForm != null) {
			this.actionDetailsForm.readAction(n.getAction());
		}
		super.readBean(n);
	}

	@Override
	protected void writeBean(N n) throws ValidationException {
		if (n != null && this.actionDetailsForm != null) {
			this.actionDetailsForm.writeAction(n.getAction());
		}
		super.writeBean(n);
	}
	/**
	 * If the use has selected "None" action when saving then we reset the doAction-checkbox to false
	 * and the node's actionCheck to null.
	 */
	@Override
	protected void validateAndSave() {
		if (this.node != null) {
			if (!this.actionEnabled.getValue() || this.actionType.getValue() == null) {
				this.actionEnabled.setValue(false);
				this.node.setActionCheck(null);
			}
		}
		super.validateAndSave();
	}

	@Override
	protected void handleChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
		if (sourceNode == this.node) {
			switch (propertyName) {
			case ActionableNode.PROPERTYNAME_SAME_STATE:
				/*if (!this.binderHasChanges())*/ readBean(sourceNode); // we (should) only update the form if the user hasn't changed anything, i.e. is not editing the form right now!
				break;
			default:
				break;
			}
		}
		super.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
	}

}
