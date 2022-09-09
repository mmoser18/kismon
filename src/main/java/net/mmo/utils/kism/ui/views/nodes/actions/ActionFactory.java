/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 12 Sep 2021
 */

package net.mmo.utils.kism.ui.views.nodes.actions;

import com.vaadin.flow.component.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.actions.IAction;
import net.mmo.utils.kism.entities.nodes.actions.impl.EmitLogAction;
import net.mmo.utils.kism.entities.nodes.actions.impl.RunScriptAction;
import net.mmo.utils.kism.entities.nodes.actions.impl.SendMailAction;
import net.mmo.utils.kism.ui.views.nodes.actions.impl.EmitLogForm;
import net.mmo.utils.kism.ui.views.nodes.actions.impl.RunScriptActionForm;
import net.mmo.utils.kism.ui.views.nodes.actions.impl.SendMailForm;

/**
 * defines the actions that can be taken after a change state
 * @param <N>
 */
@SuppressWarnings("javadoc")
@Setter
@Getter
@ToString
public class ActionFactory <N extends ActionableNode>
{
	/**
	 * the types of actions we support
	 */
	@SuppressWarnings("rawtypes") // we can't have type arguments in enumerations...
	public enum ActionType {
		EmitLogMsg(EmitLogAction.class, EmitLogForm.class),
		SendEmail (SendMailAction.class, SendMailForm.class),
		RunScript (RunScriptAction.class, RunScriptActionForm.class);

		private Class<? extends IAction> actionClass;
		private Class<? extends Component> actionFormClass;
		private ActionType(Class<? extends IAction> actionClass, Class<? extends Component> actionFormClass) {
			// I was unable to convince the compiler to accept this type in the declaration
			this.actionClass = actionClass;
			this.actionFormClass = actionFormClass;
		}

		public Class<? extends IAction> getActionClass() {
			return this.actionClass;
		}
		public Class<? extends Component> getActionFormClass() {
			return this.actionFormClass;
		}
	}

	public static <N extends ActionableNode> ActionType getActionType(IAction<ActionableNode> action) {
		if (action != null) {
			for (ActionType actionType: ActionType.values()) {
				if (actionType.getActionClass().isAssignableFrom(action.getClass())) return actionType;
			}
			throw new IllegalArgumentException("Unexpected/unsupported action type '" + action + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	public static <N extends ActionableNode> ActionType getActionType(N n) {
		return (n != null ? getActionType(n.getAction()) : null);
	}


	@SuppressWarnings("unchecked")
	public static <N extends ActionableNode> IAction<N> createActionOfType(ActionType actionType) throws Exception {
		return actionType != null ? (IAction<N>)actionType.getActionClass().getDeclaredConstructor().newInstance() : null;
	}
}
