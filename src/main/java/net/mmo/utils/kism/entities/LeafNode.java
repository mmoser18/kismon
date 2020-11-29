/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 08.11.2020
 */

package net.mmo.utils.kism.entities;

import javax.persistence.Entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class LeafNode extends Node
{
	private static final long serialVersionUID = 265720619758262963L;

	/** required for deserialization only */
	@SuppressWarnings("unused")
	private LeafNode() {
		// empty
	}

	public LeafNode(String name, String description, State state) {
		super(name, description, state);
		log.info("Created new {} {}", this.getClass().getSimpleName(), name); //$NON-NLS-1$
	}
	public LeafNode(String name, String description, State state, IntermediateNode parent) {
		super(name, description, state, parent);
		log.info("Created new {} {}", this.getClass().getSimpleName(), name); //$NON-NLS-1$
	}
}
