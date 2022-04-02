/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 20 Mar 2022
 */

package net.mmo.utils.kism.entities.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import net.mmo.utils.kism.entities.nodes.actions.ActionCheck;

/**
 * defines a node that support actions
 */
@Setter
@Getter
@SuppressWarnings({"javadoc"})
public abstract class ActionableNode extends Node
{
	private static final long serialVersionUID = 161328878290221608L;

	@JsonIgnore
	transient private int sameStateSince = 0;

	private ActionCheck actionCheck;

	public ActionableNode() {
		super();
	}

	public ActionableNode(String name, String description) {
		super(name, description);
	}

	public ActionableNode(String name, String description, State state) {
		super(name, description, state);
	}

	public ActionableNode(String name, String description, State state, IntermediateNode parent) {
		super(name, description, state, parent);
	}

	@Override
	public void setState(State state) {
		if (this.getState() != state) {
			super.setState(state);
			this.sameStateSince = 1;
		} else {
			this.sameStateSince++;
		}
		if (this.actionCheck != null) {
			this.actionCheck.checkForAction(this);
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", sameStateSince:").append(getSameStateSince()) //$NON-NLS-1$
			.append(", action:").append(getActionCheck()) //$NON-NLS-1$
			.toString();
	}

}
