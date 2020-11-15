/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 07.11.2020
 */

package net.mmo.utils.kism.entities;

import java.io.Serializable;
import java.util.Properties;
import java.util.function.Function;

import javax.persistence.Entity;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Entity
@Setter
@Getter
// @EqualsAndHashCode(callSuper = true)
@Slf4j
public abstract class Node extends AbstractEntity implements Cloneable, Serializable
{
	private static final long serialVersionUID = 3875470009216934341L;

	public enum State {
		NOT_OK,
		DEGRADED,
		OK
	}

	@NotNull
	@NotEmpty
	private String name;

	private String description;

	private State state;

	protected IntermediateNode parent;

	private boolean enabled; // child is to be considered for bubbling up state

	private boolean ignored; // child is not to be considered for bubbling up state

	private Properties customProperties;

	private Node() {
		this.enabled = true;
		this.ignored = false;
	}
	protected Node(String name, String description, State state) {
		this();
		Assert.notNull(name, "node name must not be null");  //$NON-NLS-1$
		Assert.notNull(state, "node state must not be null");  //$NON-NLS-1$
		log.info("Creating new {} {}", this.getClass().getSimpleName(), name); //$NON-NLS-1$
		this.name = name;
		this.description = description;
		this.state = state;
	}

	public Node(String name, String description, State state, IntermediateNode parent) {
		this(name, description, state);
		if (parent != null) parent.addChild(this);
	}

	public void setState(State state) {
		this.state = state;
		if (this.parent != null) this.parent.reportStateChange(this);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (this.parent != null) this.parent.reportStateChange(this);
	}

	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
		if (this.parent != null) this.parent.reportStateChange(this);
	}

	public void applyToSubTree(Function<Node, Void> fn) {
		fn.apply(this);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
			+ "[name:" + getName() //$NON-NLS-1$
			+ ", description:" + getDescription() //$NON-NLS-1$
			+ ", state:" + getState() //$NON-NLS-1$
			// to avoid recursion we only emit the parent's name:
			+ ", parent(name):" + (this.parent != null ? this.parent.getName() : "-null-") //$NON-NLS-1$ //$NON-NLS-2$
			+ ", enabled:" + isEnabled() //$NON-NLS-1$
			+ ", ignored:" + isIgnored() //$NON-NLS-1$
			+ ", customProperties:" + getCustomProperties() //$NON-NLS-1$
			+ "]"; //$NON-NLS-1$
	}
}
