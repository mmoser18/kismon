/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes.actions;

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.actions.IAction;
import org.slf4j.Logger;

/**
 * The interface that action details forms have to implement.
 */
public interface ActionDetailsForm {
	/**
	 * @return a binder for this action form
	 */
	default Binder<IAction<ActionableNode>> getBinder() {
		return null;
	}

	/**
	 * @return logger
	 */
	Logger getLogger();
	/**
	 * @param action
	 * @return whether the validation is OK.
	 */
	default boolean setAction(IAction<ActionableNode> action) {
		getLogger().debug("setAction: {}", action); //$NON-NLS-1$
		Binder<IAction<ActionableNode>> binder = getBinder();
		if (binder != null) {
			binder.readBean(action);
			boolean valid = binder.isValid();
			getLogger().debug("setAction-binder valid: {}", valid); //$NON-NLS-1$
			return valid;
		}
		return true;
	}

	/**
	 * Reads form data from bean.
	 * @param action
	 */
	default void readAction(IAction<ActionableNode> action) {
		getLogger().debug("readAction: {}", action); //$NON-NLS-1$
		Binder<IAction<ActionableNode>> binder = getBinder();
		if (binder != null) {
			binder.readBean(action);
		}
	}

	/**
	 * Writes form data to bean.
	 * @param action
	 * @throws ValidationException
	 */
	default void writeAction(IAction<ActionableNode> action) throws ValidationException {
		getLogger().debug("writeAction: {}", action); //$NON-NLS-1$
		Binder<IAction<ActionableNode>> binder = getBinder();
		if (binder != null) {
			binder.writeBean(action);
		}
	}
}
