/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.views.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.ResultChecker;
import net.mmo.utils.kism.entities.nodes.ResultChecker.Condition;
import net.mmo.utils.kism.entities.nodes.connections.CheckableConnection;
import net.mmo.utils.kism.ui.views.nodes.LeafNodeForm;

@Setter
@Getter
@Slf4j
public abstract class CheckableConnectionForm <N extends CheckableConnection> extends LeafNodeForm<N>
{
	private static final long serialVersionUID = -6625293976700949063L;

	static final String CheckResultsClassName = NodeFormField + "checkResults"; //$NON-NLS-1$

	static final String ValidationDetailsClassName = NodeFormField + "validation-details"; //$NON-NLS-1$
	static final String ResultCheckerClassName = NodeFormField + "resultChecker"; //$NON-NLS-1$
	static final String ConditionClassName = NodeFormField + "condition"; //$NON-NLS-1$
	static final String OperandClassName = NodeFormField + "operand"; //$NON-NLS-1$


	protected Checkbox checkResults;
	// protected ResultCheckerForm resultChecker;
	protected Select<ResultChecker.Condition> condition;
	// for now we support max. 3 operands:
	protected TextArea operand1;
	protected TextArea operand2;
	protected TextArea operand3;

	protected Accordion validationDetails;
	protected VerticalLayout validationDetailsPanel;


	public CheckableConnectionForm(Class<N> clazz, NodeService nodeService) {
		super(clazz, nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);

		this.checkResults = new Checkbox(Messages.getString("CheckableConnectionForm.CheckResult.Label")); //$NON-NLS-1$
		this.checkResults.setClassName(CheckResultsClassName);
		this.checkResults.setEnabled(this.isAdminUser);
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
		this.condition.setItemLabelGenerator(cond -> (cond != null
		                                             ? Messages.getString("ResultChecker.Condition.Value." + cond.name())  //$NON-NLS-1$
		                                             : Messages.getString("ResultChecker.Condition.Value.None"))); //$NON-NLS-1$
		this.condition.setEmptySelectionAllowed(true);
		this.condition.setEmptySelectionCaption(Messages.getString("ResultChecker.Condition.Value.None")); //$NON-NLS-1$
		this.condition.addClassName(ConditionClassName);
		this.condition.setEnabled(this.isAdminUser);
		this.binder.forField(this.condition).bind(CheckableConnectionForm::getCondition, CheckableConnectionForm::setCondition);
		this.condition.addValueChangeListener(event ->
		{
			log.debug("condition value changed from {} to {} (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
			if (event.isFromClient()) {
				adaptResultCheckerVisibility(this.checkResults.getValue(), this.node);
			}
		});

		this.operand1 = new TextArea(Messages.getString("ResultChecker.Operand.Label1")); //$NON-NLS-1$
		this.operand1.addClassName(OperandClassName);
		this.operand1.setThemeName(LabelPaddingTheme);
		this.operand1.setClearButtonVisible(this.clearButtonsVisible);
		this.operand1.setEnabled(this.isAdminUser);
		this.binder.forField(this.operand1)
			.withValidator(this.propertiesResolvableValidator)
			.withValidator(new OperandValidator(this, 1))
//			.withValidator((value, context) -> {
//				//TODO: make this into an own class
//				// if the nr of required args is smaller than 1 then this operand is irrelevant, so we always validate it as OK
//				Condition condValue = this.condition.getValue();
//				int nrArgs = (condValue != null ? condValue.getNrArgs() : -1);
//				log.debug("condition: {}, value: {}, nrArgs: {}", this.condition, condValue, nrArgs); //$NON-NLS-1$
//				if (condValue == null || nrArgs < 1) return ValidationResult.ok();
//				Class<?> clazz = condValue.getArgClasses()[0];
//				if (clazz == String.class) {
//					return this.nonEmptyStringValidator.apply(value, context);
//				} else if (clazz == Integer.class) {
//					return this.nonEmptyNumberValidator.apply(value, context);
//				}
//				throw new IllegalArgumentException("Unexpected argument type: " + clazz); //$NON-NLS-1$
//			})
			.bind(CheckableConnectionForm::getOperand1, CheckableConnectionForm::setOperand1);

		this.operand2 = new TextArea(Messages.getString("ResultChecker.Operand.Label2")); //$NON-NLS-1$
		this.operand2.addClassName(OperandClassName);
		this.operand2.setThemeName(LabelPaddingTheme);
		this.operand2.setClearButtonVisible(this.clearButtonsVisible);
		this.operand2.setEnabled(this.isAdminUser);
		this.binder.forField(this.operand2)
			.withValidator(this.propertiesResolvableValidator)
			.withValidator(new OperandValidator(this, 2))
//			.withValidator((value, context) -> {
//				//TODO: make this into an own class
//				// if the nr of required args is smaller than 2 then this operand is irrelevant, so we always validate it as OK
//				Condition condValue = this.condition.getValue();
//				int nrArgs = (condValue != null ? condValue.getNrArgs() : -1);
//				log.debug("condition: {}, value: {}, nrArgs: {}", this.condition, condValue, nrArgs); //$NON-NLS-1$
//				if (condValue == null || nrArgs < 2) return ValidationResult.ok();
//				Class<?> clazz = condValue.getArgClasses()[1];
//				if (clazz == String.class) {
//					return this.nonEmptyStringValidator.apply(value, context);
//				} else if (clazz == Integer.class) {
//					return this.nonEmptyNumberValidator.apply(value, context);
//				}
//				throw new IllegalArgumentException("Unexpected argument type: " + clazz); //$NON-NLS-1$
//			})
			.bind(CheckableConnectionForm::getOperand2, CheckableConnectionForm::setOperand2);

		this.operand3 = new TextArea(Messages.getString("ResultChecker.Operand.Label3")); //$NON-NLS-1$
		this.operand3.addClassName(OperandClassName);
		this.operand3.setThemeName(LabelPaddingTheme);
		this.operand3.setClearButtonVisible(this.clearButtonsVisible);
		this.operand3.setEnabled(this.isAdminUser);
		this.binder.forField(this.operand3)
			.withValidator(this.propertiesResolvableValidator)
			.withValidator(new OperandValidator(this, 3))
//			.withValidator((value, context) -> {
//				//TODO: make this into an own class
//				// if the nr of required args is smaller than 2 then this operand is irrelevant, so we always validate it as OK
//				Condition condValue = this.condition.getValue();
//				int nrArgs = (condValue != null ? condValue.getNrArgs() : -1);
//				log.info("condition: {}, value: {}, nrArgs: {}", this.condition, condValue, nrArgs); //$NON-NLS-1$
//				if (condValue == null || nrArgs < 3) return ValidationResult.ok();
//				Class<?> clazz = condValue.getArgClasses()[2];
//				if (clazz == String.class) {
//					return this.nonEmptyStringValidator.apply(value, context);
//				} else if (clazz == Integer.class) {
//					return this.nonEmptyNumberValidator.apply(value, context);
//				}
//				throw new IllegalArgumentException("Unexpected argument type: " + clazz); //$NON-NLS-1$
//			})
			.bind(CheckableConnectionForm::getOperand3, CheckableConnectionForm::setOperand3);

		this.validationDetails = new Accordion();
		this.validationDetails.close(); // initially closed
		this.validationDetailsPanel = new VerticalLayout();
		this.validationDetailsPanel.setClassName(ValidationDetailsClassName);
		this.validationDetails.add(Messages.getString("CheckableConnectionForm.ValidationDetails.Label"), this.validationDetailsPanel); //$NON-NLS-1$
		this.validationDetailsPanel.add(this.checkResults, this.condition, this.operand1, this.operand2, this.operand3);
	}

	@SuppressWarnings("serial")
	class OperandValidator implements Validator<String> {

		CheckableConnectionForm<N> form;
		int operandNr;

		OperandValidator(CheckableConnectionForm<N> form, int operandNr) {
			this.form = form;
			this.operandNr = operandNr;
		}
		@Override
		public ValidationResult apply(String value, ValueContext context) {
			//TODO: make this into an own class
			// if the nr of required args is smaller than operandNr then this operand is irrelevant, so we always validate it as OK
			Condition condValue = this.form.condition.getValue();
			int nrArgs = (condValue != null ? condValue.getNrArgs() : -1);
			log.info("condition: {}, value: {}, nrArgs: {}", this.form.condition, condValue, nrArgs); //$NON-NLS-1$
			if (condValue == null || nrArgs < this.operandNr) return ValidationResult.ok();
			Class<?> clazz = condValue.getArgClasses()[this.operandNr-1];
			if (clazz == String.class) {
				return this.form.nonEmptyStringValidator.apply(value, context);
			} else if (clazz == Integer.class) {
				return this.form.nonEmptyNumberValidator.apply(value, context);
			}
			throw new IllegalArgumentException("Unexpected argument type: " + clazz); //$NON-NLS-1$
		}
	}

	@Override
	public boolean setNode(N node, boolean abandonChanges) {
		log.info("setNode node={}", node); //$NON-NLS-1$
		boolean res = super.setNode(node, abandonChanges);
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

	static <N extends CheckableConnection> ResultChecker.Condition getCondition(N n) {
		return (n != null && n.getResultChecker() != null
				? n.getResultChecker().getCondition()
				: null);
	}

	static <N extends CheckableConnection> void setCondition(N n, ResultChecker.Condition condition) {
		if (n != null && n.getResultChecker() != null) {
			n.getResultChecker().setCondition(condition);
		}
	}

	static <N extends CheckableConnection> String getOperandN(N n, int opNr) {
		if (n != null) {
			ResultChecker resultChecker = n.getResultChecker();
			if (resultChecker != null) {
				Condition condition = resultChecker.getCondition();
				if (condition != null) {
					int nrExpectedArgs = condition.getNrArgs();
					int index = opNr-1;
					if (index < nrExpectedArgs) {
						try {
							return n.getResultChecker().getOperandN(index);
						} catch (Exception ex) {
							log.error(String.format("Something went wrong getting the result-check operand %d (condition: '%s') for Node '%s'", //$NON-NLS-1$
							                        opNr, condition, n.getName()),
							          ex);
						}
					} // the others are simply ignored
				}
			}
		}
		return null;
	}

	// Note: This method gets called by the binder for ALL fields (i.e. also those that are hidden).
	// The binder isn't aware of that So we need to ignore those that are not valid!
	static <N extends CheckableConnection> void setOperandN(N n, int opNr, String operand) {
		if (n != null) {
			ResultChecker resultChecker = n.getResultChecker();
			if (resultChecker != null) {
				Condition condition = resultChecker.getCondition();
				if (condition != null) {
					int nrExpectedArgs = condition.getNrArgs();
					int index = opNr-1;
					if (index < nrExpectedArgs) {
						try {
							n.getResultChecker().setOperandN(index, operand);
						} catch (Exception ex) {
							log.error(String.format("Something went wrong setting the result-check operand %d (condition: '%s') for Node '%s'", //$NON-NLS-1$
							                        opNr, condition, n.getName()),
							          ex);
						}
					} // the others are simply ignored
				}
			}
		}
	}

	static <N extends CheckableConnection> void setOperand1(N n, String operand) {
		setOperandN(n, 1, operand);
	}
	static <N extends CheckableConnection> String getOperand1(N n) {
		return getOperandN(n, 1);
	}

	static <N extends CheckableConnection> void setOperand2(N n, String operand) {
		setOperandN(n, 2, operand);
	}
	static <N extends CheckableConnection> String getOperand2(N n) {
		return getOperandN(n, 2);
	}

	static <N extends CheckableConnection> void setOperand3(N n, String operand) {
		setOperandN(n, 3, operand);
	}
	static <N extends CheckableConnection> String getOperand3(N n) {
		return getOperandN(n, 3);
	}

	protected void adaptResultCheckerVisibility(boolean check, N n) {
		if (n != null) {
			if (check && n.getResultChecker() == null) {
				log.info("Creating {}:", ResultChecker.class.getSimpleName()); //$NON-NLS-1$
				n.setResultChecker(new ResultChecker());
// attempt to bind the ResultChecker in via a child-form:
//				// if checkResults is active we need to add the validity of the resultChecker's binder to this one:
//				this.binder.withValidator(nn -> (checkResults ? this.resultChecker.getBinder().isValid() : true),
//				                          "ResultCheck not valid"); //$NON-NLS-1$
			}

//			this.resultChecker.setResultChecker(n.getResultChecker());
//			this.resultChecker.setVisible(check);
			this.condition.setVisible(check);
			// we need to control the visibility, number and labels of the operand fields:
			boolean op1Visible = false;
			boolean op2Visible = false;
			boolean op3Visible = false;
			Condition condValue = this.condition.getValue();
			if (condValue != null) {
				int nrArgs = condValue.getNrArgs();
				op1Visible = check && nrArgs >= 1;
				op2Visible = check && nrArgs >= 2;
				op3Visible = check && nrArgs >= 3;
			}
			this.operand1.setVisible(op1Visible);
			if (op1Visible) this.operand1.setLabel(Messages.getString( op1Visible && op2Visible
			                                                          ? "ResultChecker.Operand.Label1" //$NON-NLS-1$
			                                                          : "ResultChecker.Operand.Label")); //$NON-NLS-1$
			this.operand2.setVisible(op2Visible);
			if (op2Visible) this.operand2.setLabel(Messages.getString("ResultChecker.Operand.Label2")); //$NON-NLS-1$
			this.operand3.setVisible(op3Visible);
			if (op3Visible) this.operand3.setLabel(Messages.getString("ResultChecker.Operand.Label3")); //$NON-NLS-1$
		} else { // we leave the resultChecker attached to the node - in case the user clicks the check-button again.
			log.debug("removing ResultChecker from view:"); //$NON-NLS-1$
//			this.resultChecker.setResultChecker(null);
//			this.resultChecker.setVisible(false);
			this.condition.setVisible(false);
			this.operand1.setVisible(false);
			this.operand2.setVisible(false);
			this.operand3.setVisible(false);
		}
	}
}
