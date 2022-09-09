/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 20 Mar 2022
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.entities.nodes.actions.ActionCheck;
import net.mmo.utils.kism.ui.views.nodes.NodeForm;
import net.mmo.utils.kism.ui.views.nodes.actions.ActionFactory.ActionType;

/**
 * extends NodeForm with Action-Details
 * @param <N>
 */
public abstract class ActionForm <N extends ActionableNode> extends NodeForm<N>
{
	private static final long serialVersionUID = 7104996696515821143L;

	static final String ActionClassName = NodeFormField + "action"; //$NON-NLS-1$
	static final String ActionDetailsClassName = NodeFormField + "actionDetails"; //$NON-NLS-1$
	static final String ActionEnabledClassName = NodeFormField + "actionEnabled"; //$NON-NLS-1$
	static final String ActionThresholdClassName = NodeFormField + "actionThreshold"; //$NON-NLS-1$
	static final String ActionResetStateCounterClassName = NodeFormField + "actionResetStateCounter"; //$NON-NLS-1$

	static final String ActionTypeClassName = NodeFormField + "actionType"; //$NON-NLS-1$
	static final String ActionParametersClassName = NodeFormField + "actionParameters"; //$NON-NLS-1$

	protected Checkbox actionEnabled;
	protected Accordion actionDetails;
	protected VerticalLayout actionDetailsPanel;

	protected TextField[] triggerThreshold = new TextField[State.values().length];
	protected Validator<String> thresholdValidator;
	protected Select<ActionFactory.ActionType> actionType;
	protected Button resetSameStateCounter;

	protected Component actionDetailsForm;
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
			this.log.debug("checkResults value changed from {} to {} (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
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
							this.triggerThreshold[state.ordinal()].setValue(ac.getTriggerThresholds()[state.ordinal()]);
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
			tf.setEnabled(this.isAdminUser);
			this.binder.forField(tf)
				.withValidator(this.thresholdValidator)
				.bind((n) -> getThreshold(n, state), (n, value) -> setThreshold(n, state, value));
			this.triggerThreshold[state.ordinal()] = tf;
		}
		HorizontalLayout thresholds = new HorizontalLayout(this.triggerThreshold);

		this.resetSameStateCounter = new Button(Messages.getString("ActionForm.ResetStateCounter.Label")); //$NON-NLS-1$
		this.resetSameStateCounter.addClassName(ActionResetStateCounterClassName);
		this.resetSameStateCounter.addClickListener(event ->
		{
			if (this.node != null) {
				this.node.setSameStateSince(0);
				ActionCheck actionCheck = this.node.getActionCheck();
				if (actionCheck != null) actionCheck.setPreviousActionState(null);
			}
		});
		thresholds.add(this.resetSameStateCounter);

		this.actionType = new Select<ActionType>();
		this.actionType.setLabel(Messages.getString("ActionForm.ActionType.Label")); //$NON-NLS-1$
		this.actionType.setItems(ActionFactory.ActionType.values());
		this.actionType.setEmptySelectionAllowed(true);
		this.actionType.setEmptySelectionCaption(Messages.getString("ActionForm.ActionType.Value.None")); //$NON-NLS-1$
		this.actionType.setItemLabelGenerator(action -> (action != null
		                                                ? Messages.getString("ActionForm.ActionType.Value." + action.name()) //$NON-NLS-1$
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
				adaptActionDetailsVisibility(this.actionEnabled.getValue(), this.node, event.getValue());
			}
		});
		this.actionDetailsPanel.add(this.actionEnabled, thresholds, this.actionType);
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
					   || ac.getTriggerThresholds() == null // ... or no thresholds ...
					   || ActionForm.this.node.getAction() == null) { // ... or no action: thresholds are irrelevant (and will be removed on save)
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
				n.setAction(actionType != null ? ActionFactory.createActionOfType(actionType) : null);
			} catch (Exception ex) {
				this.log.error("Error setting action from type '" + actionType + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	protected void adaptActionType(boolean actionVisible, N n) {
		this.log.debug("adaptActionTypeAndDetailsVisibility(visible:{},n:{}",actionVisible, n); //$NON-NLS-1$
		this.actionEnabled.setValue(actionVisible);
		for (State state: State.values()) {
			this.triggerThreshold[state.ordinal()].setVisible(actionVisible);
		}
		this.actionType.setVisible(actionVisible);
		adaptActionDetailsVisibility(actionVisible, n, ActionFactory.getActionType(n));
	}

	@SuppressWarnings("unchecked")
	private void adaptActionDetailsVisibility(boolean visible, N n, ActionType at) {
		Runnable dataHasChanged = () -> setUiChanges(true);
		Component newActionHandlerForm =
			this.actionDetailsFormFactory.createActionDetailsForm(n, at, dataHasChanged);
		this.log.debug("adaptActionDetailsVisibility: form={}", newActionHandlerForm); //$NON-NLS-1$
		if (newActionHandlerForm != this.actionDetailsForm) {
			this.log.debug("different form"); //$NON-NLS-1$
			if (this.actionDetailsForm != null) {
				this.actionDetailsPanel.remove(this.actionDetailsForm);
				this.actionDetailsForm.setVisible(false);
				((ActionDetailsForm<N>)this.actionDetailsForm).setNode(null, null);
				this.actionDetailsForm = null;
			}
			if (newActionHandlerForm != null) {
				this.actionDetailsPanel.add(newActionHandlerForm);
				newActionHandlerForm.setVisible(visible);
				((ActionDetailsForm<N>)newActionHandlerForm).setNode(n, dataHasChanged);
//				this.actionDetailsForm = newActionHandlerForm;
			}
		}
	}

	@Override
	public boolean setNode(N n, boolean abandonChanges) {
		this.log.info("setNode node={}", n); //$NON-NLS-1$
		boolean res = super.setNode(n, abandonChanges);
		adaptActionType(hasAction(n), n);
		return res;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void readBean(N n) {
		if (this.actionDetailsForm != null) ((ActionDetailsForm<N>)this.actionDetailsForm).readBean();
		super.readBean(n);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void writeBean(N n) throws ValidationException {
		if (this.actionDetailsForm != null) ((ActionDetailsForm<N>)this.actionDetailsForm).writeBean();
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
}
