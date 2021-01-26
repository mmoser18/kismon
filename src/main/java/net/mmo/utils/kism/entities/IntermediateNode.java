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
import lombok.extern.slf4j.Slf4j;

@Entity
@Setter
@Getter
@EqualsAndHashCode(callSuper = true, exclude = {"children"})
@Slf4j
public class IntermediateNode extends Node
{
	private static final long serialVersionUID = 1171867853266571835L;

	public enum ChildrenCondition {
		ALL_MUST_BE_OK,  /** ALL children must be OK for an OK, else FAILED */
		AT_LEAST_ONE_OK, /** ALL must be OK for an OK, any OK is DEGRADED, none OK is failed */
		ANY_NON_FAILED,  /** At least one child must be OK for an OK, else FAILED */
		EXACTLY_ONE_OK   /** Exactly one must be OK, the others must be NOT OK */
	}

	private ArrayList<Node>	children = new ArrayList<>();

	private ChildrenCondition condition = ChildrenCondition.ALL_MUST_BE_OK;

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

	public void setCondition(ChildrenCondition condition) {
		if (this.condition != condition) {
			this.condition = condition;
			deriveNewState();
		}
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
			getChildren().add(child);
			deriveNewState();
		}
		return child;
	}

	public Node removeChild(Node child) {
		synchronized(this) {
			child.setParent(null);
			getChildren().remove(child);
			deriveNewState();
		}
		return child;
	}

	// default implementation: find minimal child-state:
	public void deriveNewState() {
		log.trace("derivedState: {}", getCondition()); //$NON-NLS-1$
		State derivedState;
		switch (getCondition()) {
		case ALL_MUST_BE_OK:
			derivedState = State.OK;
			synchronized(this) {
				for (Node child: getChildren()) {
					if (child.isEnabled()) { // child is to be considered
						if (child.getState() != null && child.getState().ordinal() < derivedState.ordinal()) derivedState = child.getState();
					}
				}
			}
			break;
		case AT_LEAST_ONE_OK:
			derivedState = State.FAILED;
			synchronized(this) {
				for (Node child: getChildren()) {
					if (child.isEnabled()) { // child is to be considered
						if (child.getState() != null && child.getState() == State.OK) {
							derivedState = State.OK;
							break; // for
						}
					}
				}
			}
			break;
		case ANY_NON_FAILED: // At least one child must be OK/DEGRADED for an OK/DEGRADED
			derivedState = State.FAILED;
			synchronized(this) {
				for (Node child: getChildren()) {
					if (child.isEnabled()) { // child is to be considered
						if (child.getState() != null && child.getState().ordinal() > derivedState.ordinal()) derivedState = child.getState();
					}
				}
			}
			break;
		case EXACTLY_ONE_OK:
			derivedState = State.FAILED;
			synchronized(this) {
				for (Node child: getChildren()) {
					if (child.isEnabled()) { // child is to be considered
						if (child.getState() != null && child.getState() == State.OK) {
							if (derivedState == State.FAILED) { // first OK
								derivedState = State.OK;
							} else { // already had an OK
								derivedState = State.FAILED;
								break;
							}
						}
					}
				}
			}
			break;
		default:
			throw new IllegalArgumentException("Unsupported Child-Operation: " + getCondition()); //$NON-NLS-1$
		}
		if (getState() != derivedState) {
			log.debug("{}: {} --> new state: {}", getName(), getCondition(), derivedState); //$NON-NLS-1$
			setState(derivedState); // this triggers deriveNewState() on parent, so no need to call that explicitly
		}
	}

	// spread down the branch:
	@Override
	public void trickleDown(Consumer<Node> action) {
		super.trickleDown(action);
		for (Node child: getChildren()) {
			child.trickleDown(action);
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
