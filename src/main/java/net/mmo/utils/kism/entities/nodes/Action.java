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

@Setter
@Getter
@ToString
public abstract class Action <N extends Node> extends AbstractEntity implements Cloneable, Serializable
{
	private static final long serialVersionUID = 9046877318500148684L;
	public final static String PROPERTYNAME_ACTION = "action";  //$NON-NLS-1$

	public enum ActionType {
		EMail,
		Script
	}

	private int triggerThreshold = 3;
	private ActionType actionType;

	public void checkForAction(N node) {
		if (node.getState() != State.OK &&
			node.getSameStateSince() == this.triggerThreshold) { // we have seen this state now often enough ==> trigger action
			doAction(node);
			node.informOnPropertyChange(PROPERTYNAME_ACTION, node.getSameStateSince(), null);
		}
	}

	public abstract void doAction(N node);
}
