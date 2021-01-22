/**

 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 08.11.2020
 */

package net.mmo.utils.kism.entities;

import java.util.ArrayList;
import java.util.function.Consumer;

import javax.persistence.Entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@EqualsAndHashCode(callSuper = true, exclude = {"children"})
// @Slf4j
public class IntermediateNode extends Node
{
	private static final long serialVersionUID = 1171867853266571835L;

	private ArrayList<Node>	children = new ArrayList<>();

	// required for deserialization only
	protected IntermediateNode() {
		super();
	}

	public IntermediateNode(String name, String description) {
		super(name, description);
	}
	public IntermediateNode(String name, String description, State state, IntermediateNode parent) {
		super(name, description, state, parent);
	}

	public Node addChild(Node child) {
		if (child == null) throw new IllegalArgumentException("child must not be null for addChild"); //$NON-NLS-1$
		Node oldParent = child.getParent();
		if ((oldParent != null) && (oldParent instanceof IntermediateNode)) {
			((IntermediateNode)oldParent).removeChild(child);
			((IntermediateNode)oldParent).deriveNewState();
		}
		synchronized(this) {
			child.setParent(this);
			this.getChildren().add(child);
			this.deriveNewState();
		}
		return child;
	}

	public Node removeChild(Node child) {
		synchronized(this) {
			child.setParent(null);
			this.getChildren().remove(child);
			this.deriveNewState();
		}
		return child;
	}

	// default implementation: find minimal child-state:
	public void deriveNewState() {
		State minimalState = State.OK;
		synchronized(this) {
			for (Node child: getChildren()) {
				if (child.isActive()) { // child is to be considered
					if (child.getState().ordinal() < minimalState.ordinal()) minimalState = child.getState();
				}
			}
		}
		if (getState() != minimalState) {
			setState(minimalState); // this triggers deriveNewState() on parent, so no need to call that explicitly
		}
	}

	// spread down the branch:
	@Override
	public void trickleDown(Consumer<Node> action) {
		super.trickleDown(action);
		for (Node child: getChildren()) {
			action.accept(child);
		}
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", children:").append(getChildren()) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}

}
