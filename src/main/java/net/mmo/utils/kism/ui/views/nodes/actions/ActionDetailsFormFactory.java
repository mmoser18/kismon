/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.06.2022
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
	private Component[] components = new Component[ActionType.values().length];

	protected BeanValidationBinder<IAction<N>> binder;
	protected Runnable dataHasChanged;
	/**
	 * creates appropriate form for passed-in action handler
	 * @param actionType
	 * @return the created form
	 */
	@SuppressWarnings({})
	public Component createActionDetailsForm(ActionType actionType) {
		log.info("createActionDetailsForm(actionType:{})", actionType); //$NON-NLS-1$
		if (actionType != null) {
			try {
				Component comp = this.components[actionType.ordinal()];
				if (comp == null) {
					comp = actionType.getActionFormClass().getDeclaredConstructor().newInstance();
					this.components[actionType.ordinal()] = comp;
				}
				return comp;
			} catch (Exception ex) {
				log.error("Error creating new instance of '" + actionType.getActionFormClass().getName() + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return null;
	}
}
