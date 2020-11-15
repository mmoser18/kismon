/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 08.11.2020
 */

package net.mmo.utils.kism.entities;

import java.util.ArrayList;
import java.util.function.Function;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Setter
@Getter
// @EqualsAndHashCode(callSuper = true)
@Slf4j
public class IntermediateNode extends Node
{
	private static final long serialVersionUID = 1171867853266571835L;

	private ArrayList<Node>	children;

	protected IntermediateNode(String name, String description, State state) {
		super(name, description, state);
		log.info("Created new {} {}", this.getClass().getSimpleName(), name); //$NON-NLS-1$
	}
	public IntermediateNode(String name, String description, State state, IntermediateNode parent) {
		super(name, description, state, parent);
		log.info("Created new {} {}", this.getClass().getSimpleName(), name); //$NON-NLS-1$
	}

	public Node addChild(Node newChild) {
		Node newChildsOldParent = newChild.getParent();
		if ((newChildsOldParent != null) && (newChildsOldParent instanceof IntermediateNode)) {
			((IntermediateNode)newChildsOldParent).removeChild(newChild);
		}
		synchronized(this) {
			newChild.setParent(this);
			if (getChildren() == null) setChildren(new ArrayList<>());
			getChildren().add(newChild);
		}
		deriveNewState();
		return newChild;
	}

	public Node removeChild(Node child) {
		synchronized(this) {
			child.setParent(null);
			getChildren().remove(child);
			if (getChildren().size() <= 0) setChildren(null);
		}
		deriveNewState();
		return child;
	}

	protected void reportStateChange(Node child) {
		final State childState = child.isEnabled() ? child.getState() : State.OK;
		if (childState.ordinal() < this.getState().ordinal()) { // OK -> DEGRADED/NOT_OK, DEGRADED -> NOT_OK
			setState(childState);
		} else if (childState.ordinal() > this.getState().ordinal()) { // getting better:
			deriveNewState();
		} // else: no change...
	}

	// default implementation: find minimal child-state:
	private void deriveNewState() {
		State minimalState = State.OK;
		synchronized(this) {
			for (Node child: getChildren()) {
				if (child.isIgnored()) continue; // child is not to be considered
				if (child.getState().ordinal() < minimalState.ordinal()) minimalState = child.getState();
			}
		}
		setState(minimalState);
	}

	@Override
	public void applyToSubTree(Function<Node, Void> fn) {
		super.applyToSubTree(fn);
		for (Node child: getChildren()) {
			fn.apply(child);
		}
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer(super.toString());
			buf.setLength(buf.length()-1);
			return buf.append(", children:").append(getChildren()) //$NON-NLS-1$
					.append("]") //$NON-NLS-1$
					.toString();
	}

}
