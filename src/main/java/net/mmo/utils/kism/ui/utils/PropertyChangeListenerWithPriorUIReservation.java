/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.utils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.vaadin.flow.component.Component;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.Node;

/**
 * A PropertyChangeListener that first acquires access to the UI so that the calling thread can then do
 * UI updates.
 * @param <N> the type of the node
 */
@Slf4j
public abstract class PropertyChangeListenerWithPriorUIReservation<N extends Node> implements PropertyChangeListener
{
	Component component;

	/**
	 * @param component
	 */
	public PropertyChangeListenerWithPriorUIReservation(Component component) {
		this.component = component;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		@SuppressWarnings("unchecked")
		N source = (N)event.getSource();
		String propertyName = event.getPropertyName();
		Object oldValue = event.getOldValue();
		Object newValue = event.getNewValue();
		log.trace("changeListener called for '{}': '{}' -> '{}' for '{}'", propertyName, oldValue, newValue, source); //$NON-NLS-1$
		if (this.component.isVisible() && predicate(source, propertyName, oldValue, newValue)) {
			this.component.getUI().ifPresentOrElse((ui) -> ui.access(() -> handlePropertyChangeEvent(source, propertyName, oldValue, newValue)),
			                                       () -> log.trace("UI not present for {} - '{}' - ignoring PropertyChangeEvent '{}': '{}' -> '{}'.", //$NON-NLS-1$
			                                                                    this.component, source.getName(), propertyName, oldValue, newValue));
		}
	}

	/**
	 * Since acquiring the UI is a costly operation we allow to avoid that unless necessary by overriding
	 * the predicate method
	 * @param source
	 * @param propertyName
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	protected boolean predicate(N source, String propertyName, Object oldValue, Object newValue) {
		return true;
	}

	/**
	 * This method handles the property change event. It is only called if the predicate yielded "true".
	 * @param source
	 * @param propertyName
	 * @param oldValue
	 * @param newValue
	 */
	protected abstract void handlePropertyChangeEvent(N source, String propertyName, Object oldValue, Object newValue);
}
