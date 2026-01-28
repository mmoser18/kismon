/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import net.mmo.utils.kism.entities.nodes.actions.ActionCheck;
import net.mmo.utils.kism.entities.nodes.actions.IAction;

/**
 * defines a node that support actions
 */
@Setter
@Getter
@SuppressWarnings({"javadoc"})
public abstract class ActionableNode extends Node
{
	private static final long serialVersionUID = 161328878290221608L;

	public final static String PROPERTYNAME_SAME_STATE = "same_state";  //$NON-NLS-1$

	@JsonIgnore
	transient private int sameStateSince = 0;

	private ActionCheck actionCheck;

	/**
	 * The actual action
	 */
	protected IAction<ActionableNode> action;


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

	public void setSameStateSince(int sameStateSince) {
		if (sameStateSince != this.sameStateSince) {
			int oldSameStateSince = this.sameStateSince;
			this.sameStateSince = sameStateSince;
			informOnPropertyChange(PROPERTYNAME_SAME_STATE, oldSameStateSince, sameStateSince);
		}
	}

	public void incSameStateSince() {
		setSameStateSince(this.sameStateSince+1);
	}

	@Override
	public void setState(State state) {
		if (this.getState() != state) {
			super.setState(state);
			// we got a new state - reset the counter:
			setSameStateSince(1);
		} else {
			incSameStateSince();
		}
		if (this.actionCheck != null && state != null) { // the latter happens if we manually reset the state to undefined
			this.actionCheck.checkForAction(this);
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", sameStateSince:").append(getSameStateSince()) //$NON-NLS-1$
			.append(", actionCheck:").append(getActionCheck()) //$NON-NLS-1$
			.append(", action:").append(getAction()) //$NON-NLS-1$
			.append('}')
			.toString();
	}
}
