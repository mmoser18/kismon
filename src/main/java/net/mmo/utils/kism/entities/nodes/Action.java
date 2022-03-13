/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 12 Sep 2021
 */

package net.mmo.utils.kism.entities.nodes;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.mmo.utils.kism.entities.AbstractEntity;
import net.mmo.utils.kism.entities.nodes.Node.State;
import net.mmo.utils.kism.utils.ThreadSupport;

/**
 * defines the actions that can be taken after a change state
 * @param <N>
 */
@SuppressWarnings("javadoc")
@Setter
@Getter
@ToString
public abstract class Action <N extends Node> extends AbstractEntity implements Cloneable, Serializable
{
	private static final long serialVersionUID = 9046877318500148684L;
	public final static String PROPERTYNAME_ACTION = "action";  //$NON-NLS-1$

	/**
	 * the types of actions we support
	 */
	public enum ActionType {
		SendEMail,
		RunScript
	}

	protected int triggerThresholds[] = new int[] {3, 5, 2};  // FAILED, DEGRADED, OK

	protected ActionType actionType;

	protected State previousActionState;

	public void checkForAction(N node) {
		// the aim of the following is to NOT trigger an action right away but only if it the state has been stable for a given number of checks
		final State currentState = node.getState();
		if (node.getSameStateSince() == this.triggerThresholds[currentState.ordinal()]) { // we have seen this state now often enough ==> trigger action
			node.informOnPropertyChange(PROPERTYNAME_ACTION, this.previousActionState, currentState);
			this.previousActionState = currentState;
			ThreadSupport.executeLater(0, () -> { doAction(node); });
		}
	}

	public abstract void doAction(N node);
}
