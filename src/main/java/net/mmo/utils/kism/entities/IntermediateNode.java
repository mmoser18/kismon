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
@EqualsAndHashCode(callSuper = true, exclude = {"children"})
// @Slf4j
public class IntermediateNode extends Node
{
	private static final long serialVersionUID = 1171867853266571835L;

	private ArrayList<Node>	children;

	// required for deserialization only
	protected IntermediateNode() {
		super();
	}

	protected IntermediateNode(String name, String description, State state) {
		super(name, description, state);
	}
	public IntermediateNode(String name, String description, State state, IntermediateNode parent) {
		super(name, description, state, parent);
	}

	// default implementation: find minimal child-state:
	public void deriveNewState() {
		State minimalState = State.OK;
		synchronized(this) {
			if (getChildren() != null) {
				for (Node child: getChildren()) {
					if (child.isActive()) { // child is to be considered
						if (child.getState().ordinal() < minimalState.ordinal()) minimalState = child.getState();
					}
				}
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
