/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 12 Sep 2021
 */

package net.mmo.utils.kism.entities.nodes.actions;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.mmo.utils.kism.entities.AbstractEntity;
import net.mmo.utils.kism.entities.nodes.Node;

/**
 * defines the actions that can be taken after a change state
 * @param <N>
 */
@SuppressWarnings("javadoc")
@Setter
@Getter
@ToString
public abstract class ActionFactory <N extends Node> extends AbstractEntity implements Cloneable, Serializable
{
	private static final long serialVersionUID = -4483697161349195285L;

	/**
	 * the types of actions we support
	 */
	public enum ActionType {
		EmitLogMsg,
		SendEmail,
		RunScript,
	}

	public Action<?> createAction(ActionType actionType) throws Exception {
		switch (actionType) {
		case EmitLogMsg:
			return new EmitLogAction<N>();
		case SendEmail:
			return new EmitSendMailAction<N>();
		case RunScript:
			return new RunScriptAction<N>();
		default:
			break;
		}
		throw new IllegalArgumentException("Unexpected/unsupported action type '" + actionType + "'"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
