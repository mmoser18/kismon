/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 07.11.2020
 */

package net.mmo.utils.kism.entities;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.KeyValuesConverter;
import net.mmo.utils.kism.utils.NodeProperties;
import net.mmo.utils.kism.utils.PropertyResolver;
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

	public final static String PROPERTYNAME_STATE  = "state";  //$NON-NLS-1$

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

	private boolean enabled = true; // child is to be considered for bubbling up state

	private NodeProperties properties = new NodeProperties();

	@JsonIgnore
	protected transient IntermediateNode parent;

	@JsonIgnore
	transient private State state = State.OK;

	@JsonIgnore
	public static boolean initializing = true; // signals that we are still initializing the application

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

	public void setProperties(NodeProperties properties) {
		this.properties = properties;
		if (!initializing) {
			updateAllResolvableValues();
		}
	}

	public void propertiesFromString(String values) {
		log.trace("setProperties(\"{}\")", values); //$NON-NLS-1$
		NodeProperties props = this.properties;
		if (props == null) {
			props = new NodeProperties(getParent() != null ? getParent().getProperties() : null);
		}
		props.setPropertiesFromString(values);
		setProperties(props);
	}

	@JsonIgnore
	public String getPropertiesAsString() {
		return (this.properties != null ? this.properties.getPropertiesAsString() : null);
	}

	/**
	 *  to be overrriden by subclasses if they contribute resolvable a field(s)
	 */
	public void updateResolvableValues() {
		// empty
	}

	public void updateAllResolvableValues() {
		trickleDown((node) -> node.updateResolvableValues());
	}

	public void setParent(IntermediateNode parent) {
		if (this.parent != parent) {
			if (this.properties == null) this.properties = new NodeProperties();
			if (parent != null) {
				this.properties.setParentProperties(parent.getProperties());
			} else { // remove (from) parent
				this.properties.setParentProperties(null);
			}
			this.parent = parent;
		}
	}

	public void setState(State state) {
		if (this.state != state) {
			State oldState = this.state;
			this.state = state;
			if (this.parent != null) this.parent.deriveNewState();
			informOnPropertyChange(PROPERTYNAME_STATE, oldState, state);
		}
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (this.parent != null) this.parent.deriveNewState();
	}

	protected void updateResultingValue(String originalValue, Consumer<String> setter) {
		if (!initializing) {
			try {
				setter.accept(resolveProperties(originalValue));
			} catch (Exception ex) {
				String msg = String.format("Error resolving '%s': %s", originalValue, ex.getMessage()); //$NON-NLS-1$
				log.error(msg);
				setter.accept(msg);
			}
		}
	}

	public String resultingFieldResolved(String originalValue, Supplier<String> getter, Consumer<String> setter) throws Exception {
		String value = getter.get();
		if (value == null || value.isBlank()) {
			setter.accept(value = resolveProperties(originalValue));
			if (value == null || value.isBlank()) throw new IllegalArgumentException("Error resolving '" + originalValue + "' - must not yield null or blank value"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return value;
	}

	// walk up the tree:
	public void bubbleUp(Consumer<Node> action) {
		for (Node n = this; n != null; n = n.getParent()) {
			action.accept(n);
		}
	}

	// spread down the branch:
	public void trickleDown(Consumer<Node> action) {
		action.accept(this);
	}

	/** instead of recursively bubbling up we iterate up the parent ladder to execute action on the root node: */
	public void onRootNodeDo(Consumer<Node> action) {
		Node n = this;
		while (n.getParent() != null) { n = n.getParent(); }
		// and call the above on the (presumable) RootNode:
		action.accept(n);
	}

	/** method to be overwritten by root nodes only! */
	protected void informOnPropertyChange(Node node, String propertyName, Object oldValue, Object newValue) {
		log.warn("informOnPropertyChange() called on non-root-node - ignored"); //$NON-NLS-1$
	}

	/** method to inform listener(s) re. property changes that may have happened outside the GUI */
	public void informOnPropertyChange(String propertyName, Object oldValue, Object newValue) {
		onRootNodeDo((n) -> n.informOnPropertyChange(this, propertyName, oldValue, newValue));
	}

	/** method to be overwritten by root node */
	public void addChangeListener(PropertyChangeListener listener) {
		onRootNodeDo((n) -> n.addChangeListener(listener));
	}

	/** method to be overwritten by root node */
	public void removeChangeListener(PropertyChangeListener listener) {
		onRootNodeDo((n) -> n.removeChangeListener(listener));
	}

	public String resolveProperties(String str) throws Exception {
		return (str != null ? PropertyResolver.resolveProperties(str, getProperties()) : null);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
			+ "{name:" + getName() //$NON-NLS-1$
			+ ", description:" + getDescription() //$NON-NLS-1$
			+ ", state:" + getState() //$NON-NLS-1$
			// to avoid recursion we only emit the parent's name:
			+ ", parent(name):" + (this.parent != null ? this.parent.getName() : "-null-") //$NON-NLS-1$ //$NON-NLS-2$
			+ ", enabled:" + isEnabled() //$NON-NLS-1$
			+ ", properties: {" + KeyValuesConverter.convertMapToString(getProperties(), ", ") + "}" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			+ "}"; //$NON-NLS-1$
	}
}
