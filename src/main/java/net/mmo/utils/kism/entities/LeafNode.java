/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 08.11.2020
 */

package net.mmo.utils.kism.entities;

import javax.persistence.Entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
abstract public class LeafNode extends Node
{
	private static final long serialVersionUID = 265720619758262963L;

	/** required for deserialization only */
	protected LeafNode() {
		// empty
	}

	public LeafNode(String name, String description) {
		super(name, description);
	}
	// required for Unit-Tests:
	public LeafNode(String name, String description, State state) {
		super(name, description, state);
	}
	// required for Unit-Tests:
	public LeafNode(String name, String description, State state, IntermediateNode parent) {
		super(name, description, state, parent);
	}

}
