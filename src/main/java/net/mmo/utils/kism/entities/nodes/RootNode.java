/**
- * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 27.11.2020
 */

package net.mmo.utils.kism.entities.nodes;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vaadin.flow.component.UIDetachedException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.CertificateHandling;
import net.mmo.utils.kism.utils.ExceptionUtils;

@SuppressWarnings("javadoc")
@Setter
@Getter
@Slf4j
public class RootNode extends IntermediateNode
{
	private static final long serialVersionUID = -1717387035835870973L;

	@JsonIgnore // no point in writing that value into the file itself. Restored/set after reading from it
	private transient String filePath;

	@JsonIgnore
	protected transient final Collection<PropertyChangeListener> changeListeners = new HashSet<>();

	protected CertificateHandling certificateHandling;


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
			throw new RuntimeException("This should NEVER be called for a RootNode!"); //$NON-NLS-1$
		}
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
		// this method is called at the end of the deserialization, i.e. all fields are set by now
		// As a side effect we initialize the certificate handling here.
		// (would be cleaner to have an explicit method for such post-initializations, but ...)
		if (this.certificateHandling == null) {
			log.info("Creating certificate handling"); //$NON-NLS-1$
			this.certificateHandling = new CertificateHandling();
		}
		this.certificateHandling.preloadKeysAndCerts();
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
			try {
				listener.propertyChange(evt);
			} catch (Exception ex) {
				if (log.isDebugEnabled()) { // we log the entire stack trace:
					log.debug("Exception calling listener {}:", listener, ex); //$NON-NLS-1$
				} else { // we just log the message:
					log.error("Exception calling listener {}: {}", listener, ExceptionUtils.exceptionCauseSummary(ex)); //$NON-NLS-1$
				}
				if (ex instanceof UIDetachedException) { // I hate that his reference pulls Vaadin flow into the application core! These should be distinct!
					// the UI is not available any longer - remove this listener to stop this exception happening again and again and hogging the log..:
					log.info("UIDetachedException -> removing listener {} from node {}:", listener, node.getName()); //$NON-NLS-1$
					this.getChangeListeners().remove(listener);
				}
			}
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
