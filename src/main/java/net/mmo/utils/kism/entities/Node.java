/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 07.11.2020
 */

package net.mmo.utils.kism.entities;

import java.io.Serializable;
import java.util.function.Function;

import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.NodeProperties;
import org.springframework.util.Assert;

@Entity
@Setter
@Getter
//@EqualsAndHashCode(callSuper = true)
// @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
// This was supposed to avoid infinite recursion when serializing the tree as JSON
// but it did not work. Instead I have set "parent" as @JsonIgnore. It is redundant and can
// be reconstructed after reading the config from a file
@Slf4j
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
public abstract class Node extends AbstractEntity implements Cloneable, Serializable
{
	private static final long serialVersionUID = 3875470009216934341L;

	public enum State {
		FAILED,
		DEGRADED,
		OK
	}

	@NotNull
	@NotEmpty
	@NotBlank
	private String name;

	private String description;

	private State state;

	@JsonIgnore
	transient protected IntermediateNode parent;

	private boolean active = true; // child is to be considered for bubbling up state

	private NodeProperties properties = new NodeProperties();

	// required for deserialization
	protected Node() {
		log.info("New {} created.", this.getClass().getSimpleName()); //$NON-NLS-1$
	}
	protected Node(String name, String description) {
		this(name, description, State.OK);
	}
	protected Node(String name, String description, State state) {
		Assert.notNull(name, "node name must not be null");  //$NON-NLS-1$
		Assert.notNull(state, "node state must not be null");  //$NON-NLS-1$
		log.info("Creating new {} {}.", this.getClass().getSimpleName(), name); //$NON-NLS-1$
		this.name = name;
		this.description = description;
		this.state = state;
		log.info("New {} with name {} created.", this.getClass().getSimpleName(), name); //$NON-NLS-1$
	}

	public Node(String name, String description, State state, IntermediateNode parent) {
		this(name, description, state);
		if (parent != null) {
			parent.getChildren().add(this);
			parent.deriveNewState();
		}
	}

	public void setParent(IntermediateNode parent) {
		if (this.properties == null) this.properties = new NodeProperties();
		if (parent != null) {
			this.properties.setParentProperties(parent.getProperties());
		} else { // remove (from) parent
			this.properties.setParentProperties(null);
		}
		this.parent = parent;
	}

	public void setState(State state) {
		this.state = state;
		if (this.parent != null) this.parent.deriveNewState();
	}

	public void setActive(boolean active) {
		this.active = active;
		if (this.parent != null) this.parent.deriveNewState();
	}

	public void applyToSubTree(Function<Node, Void> fn) {
		fn.apply(this);
	}

	@JsonIgnore
	public String getPropertiesAsString() {
		return (this.properties != null ? this.properties.getPropertiesAsString() : null);
	}

	@JsonIgnore
	public void setPropertiesFromString(String values) {
		log.trace("setPropertiesFromString(\"{}\")", values); //$NON-NLS-1$
		if (this.properties == null) {
			this.properties = new NodeProperties(this.parent != null ? this.parent.getProperties() : null);
		}
		this.properties.setPropertiesFromString(values);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
			+ "[name:" + getName() //$NON-NLS-1$
			+ ", description:" + getDescription() //$NON-NLS-1$
			+ ", state:" + getState() //$NON-NLS-1$
			// to avoid recursion we only emit the parent's name:
			+ ", parent(name):" + (this.parent != null ? this.parent.getName() : "-null-") //$NON-NLS-1$ //$NON-NLS-2$
			+ ", active:" + isActive() //$NON-NLS-1$
			+ ", properties:" + getProperties() //$NON-NLS-1$
			+ "]"; //$NON-NLS-1$
	}
}
