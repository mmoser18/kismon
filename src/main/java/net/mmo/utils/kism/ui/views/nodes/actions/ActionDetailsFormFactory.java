/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes.actions;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ActionableNode;
import net.mmo.utils.kism.entities.nodes.actions.IAction;
import net.mmo.utils.kism.ui.views.nodes.actions.ActionFactory.ActionType;
/**
 * creates appropriate form for action handler ...
 * @param <N>
 */
@Slf4j
public class ActionDetailsFormFactory <N extends ActionableNode>
{
	private ActionDetailsForm[] components = new ActionDetailsForm[ActionType.values().length];

	protected BeanValidationBinder<IAction<N>> binder;
	protected Runnable dataHasChanged;
	/**
	 * creates appropriate form for passed-in actionType.
	 * @param actionType
	 * @return the created form. Note that the created Object also extends Component so that it can be added to the view!
	 */
	@SuppressWarnings({})
	public ActionDetailsForm createActionDetailsForm(ActionType actionType) {
		log.debug("createActionDetailsForm(actionType:{})", actionType); //$NON-NLS-1$
		if (actionType != null) {
			try {
				ActionDetailsForm form = this.components[actionType.ordinal()];
				if (form == null) {
					form = (ActionDetailsForm)actionType.getActionFormClass().getDeclaredConstructor().newInstance();
					// sanity check:
					if (!Component.class.isAssignableFrom(form.getClass())) {
						throw new Exception("Programming-Error: The object created by " + //$NON-NLS-1$
						                    ActionDetailsFormFactory.class.getSimpleName() +
						                    " must be a subclass of " + Component.class); //$NON-NLS-1$
					}
					this.components[actionType.ordinal()] = form;
//				} else {
//					((Component)form).getElement().removeFromTree(); // make sure this is not attached to any old tree anymore.
				}
				log.debug("form is:{}", form); //$NON-NLS-1$
				return form;
			} catch (Exception ex) {
				log.error("Error creating new instance of '" + actionType.getActionFormClass().getName() + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return null;
	}
}
