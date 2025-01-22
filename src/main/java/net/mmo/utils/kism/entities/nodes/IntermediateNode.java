/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("javadoc")
@Setter
@Getter
@Slf4j
public class IntermediateNode extends ActionableNode
{
	private static final long serialVersionUID = 1171867853266571835L;

	public static final String PROPERTYNAME_CHILDREN_CONDITION = "children_condition"; //$NON-NLS-1$

	/**
	 * Defines the possible conditions to be evaluated by Intermediate nodes:
	 */
	public enum ChildrenCondition {
		ALL_MUST_BE_OK, // ALL children must be OK for an OK, else FAILED
		DEGRADED_ON_NOT_ALL_OK, // ALL children OK is OK, DEGRADED if ONE child is not OK, else FAILED
		ANY_NON_FAILED, // At least one child must be OK/DEGRADED for an OK/DEGRADED, else FAILED
		EXACTLY_ONE_OK // Exactly ONE child must be OK, all others must not be OK
	}

	protected ChildrenCondition condition = ChildrenCondition.ALL_MUST_BE_OK;

	/**
	 * Container for this IntermediateNode's children:
	 */
	@JsonManagedReference(value = "child-parent")
	protected ArrayList<Node> children = new ArrayList<>();
	/**
	 * This flag mirror's the UI's current state whether an item is expanded or collapsed:
	 * It allows to expand/collapse the exact same nodes when restarting the GUI.
	 * One could consider saving this flag per user but simplicity considerations prevented this... ;-)
	 */
	public boolean expanded = false;


	public IntermediateNode() { // required for deserialization & node-factory
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
			ChildrenCondition oldCondition = this.getCondition();
			this.condition = condition;
			informOnPropertyChange(PROPERTYNAME_CHILDREN_CONDITION, oldCondition, condition);
			deriveNewState();
		}
	}

	public Node addChild(Node child) {
		return addChildAtPos(-1, child); // -1 means: add to end
	}

	public Node addChildAtPos(int pos, Node child) {
		if (child == null) throw new IllegalArgumentException("child must not be null for addChild"); //$NON-NLS-1$
		Node oldParent = child.getParent();
		if ((oldParent != null) && (oldParent instanceof IntermediateNode)) {
			((IntermediateNode)oldParent).removeChild(child);
		}
		synchronized(this) {
			child.setParent(this);
			if (pos >= 0) {
				this.children.add(pos, child);
			} else {
				this.children.add(child);
			}
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

	// default implementation: find minimal child-state: FAILED < DEGRADED < OK
	public void deriveNewState() {
		log.trace("deriveNewState using condition '{}':", getCondition()); //$NON-NLS-1$
		State derivedState;
		switch (getCondition()) {
		case ALL_MUST_BE_OK:
			derivedState = State.OK;
			synchronized(this) {
				for (Node child: getChildren()) {
					if (child.isApplicable() && child.getState() != null) { // child is to be considered
						if (child.getState().ordinal() < State.OK.ordinal()) {
							derivedState = State.FAILED;
							break;
						}
					}
				}
			}
			break;
		case DEGRADED_ON_NOT_ALL_OK:
			derivedState = State.OK;
			synchronized(this) {
				for (Node child: getChildren()) {
					if (child.isApplicable() && child.getState() != null) { // child is to be considered
						if (child.getState() != State.OK) {
							if (derivedState == State.OK) { // first non-OK:
								derivedState = State.DEGRADED;
							} else { // second non-OK
								derivedState = State.FAILED;
								break; // one non-OK is enough for FAILED!
							}
						}
					}
				}
			}
			break;
		case ANY_NON_FAILED: // At least one child must be OK/DEGRADED for an OK/DEGRADED
			derivedState = State.FAILED;
			synchronized(this) {
				for (Node child: getChildren()) {
					if (child.isApplicable() && child.getState() != null) { // child is to be considered
						if (child.getState() == State.OK) {
							derivedState = State.OK;
							break; //leave for - it can't get better than OK
						}
						if (child.getState() == State.DEGRADED) {
							derivedState = State.DEGRADED;
							// no break - there is still room for an OK
						}
					}
				}
			}
			break;
		case EXACTLY_ONE_OK:
			derivedState = State.FAILED;
			synchronized(this) {
				for (Node child: getChildren()) {
					if (child.isApplicable()) { // child is to be considered
						if (child.getState() != null && child.getState() == State.OK) {
							if (derivedState == State.FAILED) { // first OK
								derivedState = State.OK;
							} else { // we already had an OK! ==> Error!
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
//		if (getState() != derivedState) {
			log.debug("'{}': '{}' --> new state: '{}'", getName(), getCondition(), derivedState); //$NON-NLS-1$
			setState(derivedState); // this triggers deriveNewState() on parent, so no need to call that explicitly
//		} else {
//			log.debug("'{}': '{}' --> state remained: '{}'", getName(), getCondition(), derivedState); //$NON-NLS-1$
//		}
	}

	// spread down the branch:
	@Override
	public void trickleDown(Consumer<Node> activity) {
		super.trickleDown(activity);
		for (Node child: getChildren()) {
			child.trickleDown(activity);
		}
	}

	@Override
	public void executeRequest() throws Exception {
		getChildren().forEach((node) -> {
			try {
				node.executeRequest();
			} catch (Exception ex) {
				log.info("Exception executing '{}': {}", node, ex); //$NON-NLS-1$
			}
		});
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
