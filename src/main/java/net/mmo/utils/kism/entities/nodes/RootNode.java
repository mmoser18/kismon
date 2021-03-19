/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 27.11.2020
 */

package net.mmo.utils.kism.entities.nodes;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Setter
@Getter
@Slf4j
public class RootNode extends IntermediateNode
{
	private static final long serialVersionUID = -1717387035835870973L;

	@JsonIgnore // no point in writing that value into the file itself. Restored/set after reading from it
	private transient String filePath;

	@JsonIgnore
	public transient final Collection<PropertyChangeListener> changeListeners = new HashSet<>();


	public RootNode() { // required for deserialization & node-factory
		super();
	}

	public RootNode(String name, String description) {
		super(name, description);
	}

	@SuppressWarnings({"deprecation"})
	@Override
	protected void finalize() {
		this.changeListeners.clear();
	}

	public void setParent(Node parent) {
		if (parent != null) {
			throw new RuntimeException("This should NEVER be called!"); //$NON-NLS-1$
		}
	}

	/**
	 * Register a listener. A listener can only be registered once.
	 */
	@Override
	public void addChangeListener(PropertyChangeListener listener) {
		this.getChangeListeners().add(listener);
	}

	/**
	 * Deregister a listener
	 */
	@Override
	public void removeChangeListener(PropertyChangeListener listener) {
		this.getChangeListeners().remove(listener);
	}

	/** method to inform listener(s) re. property changes that may have happened outside the GUI */
	@Override
	protected void informOnPropertyChange(Node node, String propertyName, Object oldValue, Object newValue) {
		log.trace("informOnPropertyChange({}, {}, {}, {})", node.getName(), propertyName, oldValue, newValue); //$NON-NLS-1$ / this is very verbose thus reduced to trace
		PropertyChangeEvent evt = new PropertyChangeEvent(node, propertyName, oldValue, newValue);
		this.getChangeListeners().forEach((listener) ->
		{
			log.trace("Calling listener {}: {}", listener, evt); //$NON-NLS-1$
			listener.propertyChange(evt);
		});
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", filePath:").append(this.filePath) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}
}
