/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes;

import java.beans.PropertyChangeListener;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import net.mmo.utils.kism.entities.AbstractEntity;
import net.mmo.utils.kism.utils.KeyValuesConverter;
import net.mmo.utils.kism.utils.NodeProperties;
import net.mmo.utils.kism.utils.PropertyResolver;
import net.mmo.utils.kism.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

@SuppressWarnings("javadoc")
@Setter
@Getter
// @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
// This was supposed to avoid infinite recursion when serializing the tree as JSON
// but it did not work. Instead I have set "parent" as @JsonIgnore. It is redundant and can
// be reconstructed after reading the config from a file
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
public abstract class Node extends AbstractEntity
{
	private static final long serialVersionUID = 3875470009216934341L;

	public final static String PROPERTYNAME_STATE = "state";  //$NON-NLS-1$
	public final static String PROPERTYNAME_PARENT = "parent";  //$NON-NLS-1$
	public final static String PROPERTYNAME_APPLICABLE = "applicable";  //$NON-NLS-1$

	@JsonIgnore
	transient protected Logger log;

	public enum State {
		FAILED,
		DEGRADED,
		OK;

		/**
		 * Like valueOf() but returns null if the value is blank or "null"/"NULL"
		 * @param str
		 * @return
		 */
		public static State nullTolerantValueOf(String str) {
			return (str == null || str.isBlank() || str.equalsIgnoreCase("null")) ? null : State.valueOf(str);  //$NON-NLS-1$
		}
	}

	@NotNull
	@NotEmpty
	@NotBlank
	private String name;

	private String description;

	private boolean applicable = true; // child is to be considered for bubbling up state

	private NodeProperties properties = new NodeProperties();

	@JsonBackReference(value = "child-parent")
	protected transient IntermediateNode parent;

	@JsonIgnore
	transient private State state = State.OK;

	// required for deserialization
	protected Node() {
		super();
		this.log = LoggerFactory.getLogger(this.getClass());
		this.log.debug("New {} created.", this.getClass().getSimpleName()); //$NON-NLS-1$
	}
	protected Node(String name, String description) {
		this(name, description, State.OK);
	}
	protected Node(String name, String description, State state) {
		Assert.notNull(name, "node name must not be null");  //$NON-NLS-1$
		Assert.notNull(state, "node state must not be null");  //$NON-NLS-1$
		this.name = name;
		this.description = description;
		this.state = state;
		this.log = LoggerFactory.getLogger(this.getClass());
		this.log.info("New {} with name {} created.", this.getClass().getSimpleName(), name); //$NON-NLS-1$
	}

	public Node(String name, String description, State state, IntermediateNode parent) {
		this(name, description, state);
		if (parent != null) {
			parent.getChildren().add(this);
			parent.deriveNewState();
		}
	}

	// activities that need to be done after cloning an object - e.g. re-initializing transient fields.
	public void postClone(Node original) {
		this.parent = original.parent;
	}

	public void setProperties(NodeProperties properties) {
		this.properties = properties;
		if (!initializing) {
			updateAllResolvableValues();
		}
	}

	public void propertiesFromString(String values) {
		this.log.trace("setProperties(\"{}\")", values); //$NON-NLS-1$
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
	 *  to be overrriden by subclasses if they contribute resolvable (a) field(s)
	 */
	public void updateResolvableValues() {
		// empty
	}

	public void updateAllResolvableValues() {
		trickleDown((node) -> node.updateResolvableValues());
	}

	/**`Caution: This method is ONLY setting the node's parent!
	 * It is NOT doing any additional arithmetic required for consistency like removing the node from
	 * its previous parent and/or triggering the respective state-updates of the old and new parent!
	 * For that use the "prospectiveParent.addChild(node)" - method!
	 * This method shouldn't even be visible but the NodeService requires it when reconstructing the
	 * tree while reading it in from JSON. :-(
	 **/
	public void setParent(IntermediateNode parent) {
		if (this.parent != parent) {
			IntermediateNode oldParent = this.parent;
			this.parent = parent;
			informOnPropertyChange(PROPERTYNAME_PARENT, oldParent, parent);
			// changing parent also requires to adapt parent-properties:
			if (this.properties == null) this.properties = new NodeProperties();
			if (parent != null) {
				this.properties.setParentProperties(parent.getProperties());
			} else { // remove (from) parent
				this.properties.setParentProperties(null);
			}
		}
	}

	public void setState(State state) {
		if (this.state != state) {
			State oldState = this.state;
			this.state = state;
			informOnPropertyChange(PROPERTYNAME_STATE, oldState, state);
			if (this.applicable && this.parent != null) this.parent.deriveNewState();
		}
	}

	public void setApplicable(boolean applicable) {
		if (this.applicable != applicable) {
			boolean oldApplicable = this.applicable;
			this.applicable = applicable;
			informOnPropertyChange(PROPERTYNAME_APPLICABLE, oldApplicable, applicable);
			if (this.parent != null) this.parent.deriveNewState();
		}
	}

	protected void updateResultingValue(String originalValue, Consumer<String> setter) {
		if (!initializing) {
			try {
				setter.accept(resolveProperties(originalValue));
			} catch (Exception ex) {
				String msg = String.format("Error resolving '%s': %s", originalValue, ex.getMessage()); //$NON-NLS-1$
				this.log.error(msg);
				setter.accept(msg);
			}
		}
	}

	public String resultingFieldResolved(String originalValue, Supplier<String> getter, Consumer<String> setter) throws Exception {
		String value = getter.get();
		if (StringUtils.isEmpty(value)) {
			setter.accept(value = resolveProperties(originalValue));
			if (StringUtils.isEmpty(value)) {
				throw new IllegalArgumentException("Error resolving '" + originalValue + "' - must not yield null or blank value"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return value;
	}

	// walk up the tree:
	public void bubbleUp(Consumer<Node> activity) {
		for (Node n = this; n != null; n = n.getParent()) {
			activity.accept(n);
		}
	}

	// spread down the branch:
	public void trickleDown(Consumer<Node> activity) {
		activity.accept(this);
	}

	@JsonIgnore
	public RootNode getRootNode() {
		Node n = this;
		while (n.getParent() != null) { n = n.getParent(); }
		return (n instanceof RootNode ? (RootNode)n : null);
	}
	/** instead of recursively bubbling up we iterate up the parent ladder to execute action on the root node: */
	public void onRootNodeDo(Consumer<Node> activity) {
		RootNode root = getRootNode();
		if (root != null) {
			activity.accept(root);
		// no else: this is normal for "free-floating" nodes, e.g. when creating a new node that hasn't yet been plugged into the tree
		}
	}

	/** method to be overwritten by root nodes only! */
	protected void informOnPropertyChange(Node node, String propertyName, Object oldValue, Object newValue) {
		this.log.warn("informOnPropertyChange() called on non-root-node - ignored"); //$NON-NLS-1$
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

	// this is the code being executed when clicking "executeButton" manually:
	public abstract void executeRequest() throws Exception;

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", name:").append(getName()) //$NON-NLS-1$
			.append(", description:").append(getDescription()) //$NON-NLS-1$
			.append(", state:").append(getState()) //$NON-NLS-1$
			// to avoid recursion we must only emit the parent's name, not the entire object!:
			.append(", parent(name):").append(this.parent != null ? this.parent.getName() : "-null-") //$NON-NLS-1$ //$NON-NLS-2$
			.append(", applicable:").append(isApplicable()) //$NON-NLS-1$
			.append(", properties: {").append(KeyValuesConverter.convertMapToString(getProperties(), ", ")).append('}') //$NON-NLS-1$ //$NON-NLS-2$
			.append('}')
			.toString();
	}
}
