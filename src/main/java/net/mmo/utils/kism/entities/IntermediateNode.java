/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 08.11.2020
 */

package net.mmo.utils.kism.entities;

import java.util.ArrayList;
import java.util.function.Function;

import javax.persistence.Entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
public class IntermediateNode extends Node
{
	private static final long serialVersionUID = 1171867853266571835L;

	private ArrayList<Node>	children = new ArrayList<>();

	protected IntermediateNode(String name, String description, State state) {
		super(name, description, state);
	}
	public IntermediateNode(String name, String description, State state, IntermediateNode parent) {
		super(name, description, state, parent);
	}

	public void addChild(Node child) {
		synchronized(this) {
			child.setParent(this);
			this.getChildren().add(child);
		}
		deriveNewState();
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
			for (Node child: this.children) {
				if (child.isIgnored()) continue; // child is not to be considered
				if (child.getState().ordinal() < minimalState.ordinal()) minimalState = child.getState();
			}
		}
		setState(minimalState);
	}

	@Override
	public void applyToSubTree(Function<Node, Void> fn) {
		super.applyToSubTree(fn);
		for (Node child: this.children) {
			fn.apply(child);
		}
	}

	@Override
	public String toString() {
		return super.toString()
			+ ", children:" + this.children //$NON-NLS-1$
			+ "]"; //$NON-NLS-1$
	}

}
