/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 15.01.2021
 */

package net.mmo.utils.kism.ui.view.nodes;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import net.mmo.utils.kism.entities.ResultChecker;
import net.mmo.utils.kism.entities.ResultChecker.Condition;
import net.mmo.utils.kism.ui.UIConstants;


public class ResultCheckerForm extends VerticalLayout
{
	private static final long serialVersionUID = 7520959458381303528L;

	private final static String ConditionClassName = NodeFormField + "condition"; //$NON-NLS-1$
	private final static String OperandClassName = NodeFormField + "operand"; //$NON-NLS-1$

	protected ComboBox<ResultChecker.Condition> condition;
	protected TextArea operand;

	ResultChecker resultChecker;
//	protected Binder<ResultChecker> binder; // a Binder that is aware of bean validation annotations

	public ResultCheckerForm() {
		createFields();
	}

	public void setResultChecker(ResultChecker resultChecker) {
		this.resultChecker = resultChecker;
	}

	private void createFields() {
		this.condition = new ComboBox<Condition>(Messages.getString("ResultChecker.Condition.Label")); //$NON-NLS-1$
		this.condition.setItems(Condition.values());
		this.condition.setItemLabelGenerator(cond -> Messages.getString("ResultChecker.Condition.Value." + cond.name())); //$NON-NLS-1$
		this.condition.addClassName(ConditionClassName);

		this.operand = new TextArea(Messages.getString("ResultChecker.Operand.Label")); //$NON-NLS-1$
		this.operand.addClassName(OperandClassName);
		this.operand.setThemeName(UIConstants.LabelPaddingTheme);

//		this.binder.bindInstanceFields(this); // this binder-setup has to be AFTER creation of the form fields

		add(this.condition, this.operand);
	}
}
