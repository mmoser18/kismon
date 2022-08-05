/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 12 Sep 2021
 */

package net.mmo.utils.kism.entities.nodes.actions;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.actions.impl.EmitLogAction;
import net.mmo.utils.kism.entities.nodes.actions.impl.RunScriptAction;
import net.mmo.utils.kism.entities.nodes.actions.impl.SendMailAction;

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
	public enum ActionType {
		EmitLogMsg(EmitLogAction.class),
		SendEmail(SendMailAction.class),
		RunScript(RunScriptAction.class);

		Class<? extends IAction<?>> actionClass;
		@SuppressWarnings("unchecked")
		ActionType(Class<?> actionClass) {
			// I was unable to convince the compiler to accept this type in the declaration
			this.actionClass = (Class<? extends IAction<?>>)actionClass;
		}

		@SuppressWarnings("unchecked")
		public <N extends ActionableNode> Class<IAction<N>> getActionClass() {
			return (Class<IAction<N>>)this.actionClass;
		}
	}

	public static <N extends ActionableNode> ActionType getActionType(IAction<ActionableNode> action) {
		if (action != null) {
			for (ActionType actionType: ActionType.values()) {
				if (actionType.actionClass.isAssignableFrom(action.getClass())) return actionType;
			}
			throw new IllegalArgumentException("Unexpected/unsupported action type '" + action + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	public static ActionType getActionType(ActionCheck ac) {
		return (ac != null ? getActionType(ac.getAction()) : null);
	}

	public static <N extends ActionableNode> ActionType getActionType(N n) {
		return (n != null ? getActionType(n.getActionCheck()) : null);
	}


	@SuppressWarnings("unchecked")
	public static <N extends ActionableNode> IAction<N> createActionOfType(ActionType actionType) throws Exception {
		return actionType != null ? (IAction<N>)actionType.actionClass.getDeclaredConstructor().newInstance() : null;
	}
}
