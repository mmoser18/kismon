/**
 * @author Michael Moser (mmo@zurich.ibm.com)
 * @since 08.11.2020
 */

package net.mmo.utils.monitoring_kiss.entities;

import java.util.ArrayList;
import java.util.function.Function;

import javax.persistence.Entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class IntermediateNode extends Node
{
	private static final long serialVersionUID = 1171867853266571835L;

	private ArrayList<Node>	children = new ArrayList<>();

	private IntermediateNode(String name, String description, State state) {
		super(name, description, state);
	}
	public IntermediateNode(String name, String description, State state, IntermediateNode parent) {
		super(name, description, state, parent);
	}

	/** this is the one and only node that has no parent! */
	private final static IntermediateNode root = new IntermediateNode(Messages.getString("mk_root"), //$NON-NLS-1$
	                                                                  Messages.getString("mk_root-node"), //$NON-NLS-1$
	                                                                  State.OK);
	public static IntermediateNode getRoot() {
		return root;
	}

	public void addChild(Node child) {
		synchronized(this) {
			child.setParent(this);
			this.getChildren().add(child);
		}
		deriveNewState();
	}

	protected void reportStateChange(Node child) {
		final State childState = child.isDisabled() ? State.OK : child.getState();
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
